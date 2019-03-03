//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.entity.traits.*;
import xyz.chunkstories.api.entity.traits.serializable.*;
import xyz.chunkstories.api.events.voxel.WorldModificationCause;
import xyz.chunkstories.api.graphics.MeshMaterial;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.inventory.InventoryHolder;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.util.ColorsTools;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.core.entity.traits.TraitArmor;
import xyz.chunkstories.core.entity.traits.TraitFoodLevel;
import xyz.chunkstories.core.entity.traits.MinerTrait;
import xyz.chunkstories.core.entity.traits.TraitEyeLevel;
import xyz.chunkstories.core.entity.traits.TraitTakesFallDamage;
import org.joml.*;

import java.util.HashMap;

/**
 * Core/Vanilla player, has all the functionality you'd want from it:
 * creative/survival mode, flying and walking controller...
 */
public class EntityPlayer extends EntityHumanoid implements WorldModificationCause, InventoryHolder {
	private TraitControllable controllerComponent;

	protected TraitInventory inventory;
	TraitSelectedItem selectedItemComponent;

	private TraitName name;
	TraitCreativeMode creativeMode;
	TraitFlyingMode flyMode;

	TraitArmor armor;
	private TraitFoodLevel foodLevel;

	TraitVoxelSelection raytracer;

	private boolean onLadder = false;

	Location lastCameraLocation;

	int variant;

	public EntityPlayer(EntityDefinition t, World world) {
		super(t, world);

		//controllerComponent = new TraitController(this);
		inventory = new TraitInventory(this, 10, 4);
		selectedItemComponent = new TraitSelectedItem(this, inventory);
		name = new TraitName(this);
		creativeMode = new TraitCreativeMode(this);
		flyMode = new TraitFlyingMode(this);
		foodLevel = new TraitFoodLevel(this, 100);
		armor = new TraitArmor(this, 4, 1);

		raytracer = new TraitVoxelSelection(this) {

			@Override
			public Location getBlockLookingAt(boolean inside, boolean can_overwrite) {
				double eyePosition = stance.getStance().getEyeLevel();

				Vector3d initialPosition = new Vector3d(getLocation());
				initialPosition.add(new Vector3d(0, eyePosition, 0));

				Vector3d direction = new Vector3d(entityRotation.getDirectionLookingAt());

				if (inside)
					return EntityPlayer.this.world.getCollisionsManager().raytraceSelectable(new Location(EntityPlayer.this.world, initialPosition), direction,
							256.0);
				else
					return EntityPlayer.this.world.getCollisionsManager().raytraceSolidOuter(new Location(EntityPlayer.this.world, initialPosition), direction,
							256.0);
			}

		};

		new TraitInteractible(this) {

			@Override
			public boolean handleInteraction(Entity entity, Input input) {
				if (entityHealth.isDead() && input.getName().equals("mouse.right")) {

					Controller controller = controllerComponent.getController();//entity.traits.tryWith(TraitControllable.class, TraitControllable::getController);
					if (controller instanceof Player) {
						Player p = (Player) controller;
						p.openInventory(inventory);
						return true;
					}
				}
				return false;
			}
		};

		//new PlayerOverlay(this);
		//new TraitRenderable(this, EntityPlayerRenderer<EntityPlayer>::new);
		new TraitDontSave(this);

		new TraitEyeLevel(this) {

			@Override
			public double getEyeLevel() {
				return stance.getStance().getEyeLevel();
			}

		};

		new PlayerMovementController(this);
		controllerComponent = new EntityPlayerController(this);

		new MinerTrait(this);

		int variant = ColorsTools.getUniqueColorCode(this.getName()) % 6;
		HashMap<String, String> aaTchoum = new HashMap<>();
		aaTchoum.put("albedoTexture", "./models/human/variant" + variant + ".png");
		MeshMaterial customSkin = new MeshMaterial("playerSkin", aaTchoum, "opaque");
		new EntityHumanoidRenderer(this, customSkin);
	}

	// Server-side updating
	@Override
	public void tick() {

		// if(world instanceof WorldMaster)
		traits.with(MinerTrait.class, mt -> mt.tickTrait());

		// Tick item in hand if one such exists
		ItemPile pileSelected = this.traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());
		if (pileSelected != null)
			pileSelected.getItem().tickInHand(this, pileSelected);

		// Auto-pickups items on the ground
		if (world instanceof WorldMaster && (world.getTicksElapsed() % 60L) == 0L) {

			for (Entity e : world.getEntitiesInBox(getLocation(), new Vector3d(3.0))) {
				if (e instanceof EntityGroundItem && e.getLocation().distance(this.getLocation()) < 3.0f) {
					EntityGroundItem eg = (EntityGroundItem) e;
					if (!eg.canBePickedUpYet())
						continue;

					world.getSoundManager().playSoundEffect("sounds/item/pickup.ogg", Mode.NORMAL, getLocation(), 1.0f,
							1.0f);

					ItemPile pile = eg.getItemPile();
					if (pile != null) {
						ItemPile left = this.inventory.addItemPile(pile);
						if (left == null)
							world.removeEntity(eg);
						else
							eg.setItemPile(left);
					}
				}
			}
		}

		if (world instanceof WorldMaster) {
			// Food/health subsystem handled here decrease over time

			// Take damage when starving
			// TODO: move to trait
			if ((world.getTicksElapsed() % 100L) == 0L) {
				if (foodLevel.getValue() == 0)
					entityHealth.damage(TraitFoodLevel.HUNGER_DAMAGE_CAUSE, 1);
				else {
					// 27 minutes to start starving at 0.1 starveFactor
					// Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

					// Starve slowly if inactive
					float starve = 0.03f;

					// Walking drains you
					if (this.entityVelocity.getVelocity().length() > 0.3) {
						starve = 0.06f;
						// Running is even worse
						if (this.entityVelocity.getVelocity().length() > 0.7)
							starve = 0.15f;
					}

					float newfoodLevel = foodLevel.getValue() - starve;
					foodLevel.setValue(newfoodLevel);
				}
			}

			// It restores hp
			// TODO move to trait
			if (foodLevel.getValue() > 20 && !entityHealth.isDead()) {
				if (entityHealth.getHealth() < entityHealth.getMaxHealth()) {
					entityHealth.setHealth(entityHealth.getHealth() + 0.01f);

					float newfoodLevel = foodLevel.getValue() - 0.01f;
					foodLevel.setValue(newfoodLevel);
				}
			}

			// Being on a ladder resets your jump height
			if (onLadder)
				traits.with(TraitTakesFallDamage.class, fd -> fd.resetFallDamage());

			// So does flying
			if (flyMode.get())
				traits.with(TraitTakesFallDamage.class, fd -> fd.resetFallDamage());
		}

		super.tick();

	}

	/*class PlayerOverlay extends TraitHasOverlay {

		public PlayerOverlay(Entity entity) {
			super(entity);
		}

		@Override
		public void drawEntityOverlay(RenderingInterface renderer) {
			if (EntityPlayer.this.equals(((WorldClient) getWorld()).getClient().getPlayer().getControlledEntity())) {
				float scale = 2.0f;

				renderer.textures().getTexture("./textures/gui/hud/hud_survival.png").setLinearFiltering(false);
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(
						renderer.getWindow().getWidth() / 2 - 256 * 0.5f * scale, 64 + 64 + 16 - 32 * 0.5f * scale,
						256 * scale, 32 * scale, 0, 32f / 256f, 1, 0,
						renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true, null);

				// Health bar
				int horizontalBitsToDraw = (int) (8
						+ 118 * Math2.clamp(entityHealth.getHealth() / entityHealth.getMaxHealth(), 0.0, 1.0));
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(renderer.getWindow().getWidth() / 2 - 128 * scale,
						64 + 64 + 16 - 32 * 0.5f * scale, horizontalBitsToDraw * scale, 32 * scale, 0, 64f / 256f,
						horizontalBitsToDraw / 256f, 32f / 256f,
						renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true,
						new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));

				// Food bar
				horizontalBitsToDraw = (int) (0 + 126 * Math2.clamp(foodLevel.getValue() / 100f, 0.0, 1.0));
				renderer.getGuiRenderer().drawBoxWindowsSpaceWithSize(
						renderer.getWindow().getWidth() / 2 + 0 * 128 * scale + 0, 64 + 64 + 16 - 32 * 0.5f * scale,
						horizontalBitsToDraw * scale, 32 * scale, 0.5f, 64f / 256f, 0.5f + horizontalBitsToDraw / 256f,
						32f / 256f, renderer.textures().getTexture("./textures/gui/hud/hud_survival.png"), false, true,
						new Vector4f(1.0f, 1.0f, 1.0f, 0.75f));

				// If we're using an item that can render an overlay
				if (selectedItemComponent.getSelectedItem() != null) {
					ItemPile pile = selectedItemComponent.getSelectedItem();
					if (pile.getItem() instanceof ItemOverlay)
						((ItemOverlay) pile.getItem()).drawItemOverlay(renderer, pile);
				}

				// We don't want to render our own tag do we ?
				return;
			}

			// Renders the nametag above the player heads
			Vector3d pos = getLocation();

			// don't render tags too far out
			if (pos.distance(renderer.getCamera().getCameraPosition()) > 32f)
				return;

			// Don't render a dead player tag
			if (entityHealth.getHealth() <= 0)
				return;

			Vector3fc posOnScreen = renderer.getCamera().transform3DCoordinate(
					new Vector3f((float) (double) pos.x(), (float) (double) pos.y() + 2.0f, (float) (double) pos.z()));

			float scale = posOnScreen.z();
			String txt = name.getName();
			float dekal = renderer.getFontRenderer().defaultFont().getWidth(txt) * 16 * scale;
			if (scale > 0)
				renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
						posOnScreen.x() - dekal / 2, posOnScreen.y(), txt, 16 * scale, 16 * scale,
						new Vector4f(1, 1, 1, 1));
		}
	}*/

	/*class EntityPlayerRenderer<H extends EntityPlayer> extends EntityHumanoidRenderer<H> {
		@Override
		public void setupRender(RenderingInterface renderingContext) {
			super.setupRender(renderingContext);
		}

		@Override
		public int renderEntities(RenderingInterface renderer, RenderingIterator<H> renderableEntitiesIterator) {
			renderer.useShader("entities_animated");

			setupRender(renderer);
			int e = 0;

			Vector3f loc3f = new Vector3f();
			Vector3f pre3f = new Vector3f();

			for (EntityPlayer entity : renderableEntitiesIterator.getElementsInFrustrumOnly()) {
				Location location = entity.getPredictedLocation();
				loc3f.set((float) location.x(), (float) location.y(), (float) location.z());
				pre3f.set((float) entity.getPredictedLocation().x(), (float) entity.getPredictedLocation().y(),
						(float) entity.getPredictedLocation().z());
				TraitAnimated animation = entity.traits.get(TraitAnimated.class);
				if (animation == null)
					return 0;

				if (!renderer.getCurrentPass().name.startsWith("shadow")
						|| location.distance(renderer.getCamera().getCameraPosition()) <= 15f) {
					((CachedLodSkeletonAnimator) animation.getAnimatedSkeleton()).lodUpdate(renderer);

					Matrix4f matrix = new Matrix4f();
					matrix.translate(loc3f);
					renderer.setObjectMatrix(matrix);

					// Obtains the data for the block the player is standing inside of, so he can be
					// lit appropriately
					CellData cell = entity.getWorld().peekSafely(entity.getLocation());
					renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());

					variant = ColorsTools.getUniqueColorCode(entity.getName()) % 6;

					// Player textures
					Texture2D playerTexture = renderer.textures()
							.getTexture("./models/human/variant" + variant + ".png");
					playerTexture.setLinearFiltering(false);

					renderer.bindAlbedoTexture(playerTexture);
					renderer.bindNormalTexture(renderer.textures().getTexture("./textures/normalnormal.png"));
					renderer.bindMaterialTexture(renderer.textures().getTexture("./textures/defaultmaterial.png"));

					renderer.meshes().getRenderableAnimatableMesh("./models/human/human.dae").render(renderer,
							animation.getAnimatedSkeleton(), System.currentTimeMillis() % 1000000);

					for (ItemPile aip : entity.armor.iterator()) {
						ItemArmor ia = (ItemArmor) aip.getItem();

						renderer.bindAlbedoTexture(renderer.textures().getTexture(ia.getOverlayTextureName()));
						renderer.textures().getTexture(ia.getOverlayTextureName()).setLinearFiltering(false);

						SkeletonAnimator armorMask = ia.bodyPartsAffected().size() == 0
								? animation.getAnimatedSkeleton()
								: new SkeletonAnimator() {

									@Override
									public Matrix4fc getBoneHierarchyTransformationMatrix(String nameOfEndBone,
											double animationTime) {
										return animation.getAnimatedSkeleton()
												.getBoneHierarchyTransformationMatrix(nameOfEndBone, animationTime);
									}

									@Override
									public Matrix4fc getBoneHierarchyTransformationMatrixWithOffset(
											String nameOfEndBone, double animationTime) {
										return animation.getAnimatedSkeleton()
												.getBoneHierarchyTransformationMatrixWithOffset(nameOfEndBone,
														animationTime);
									}

									@Override
									public boolean shouldHideBone(RenderingInterface renderingContext,
											String boneName) {
										return animation.getAnimatedSkeleton().shouldHideBone(renderingContext,
												boneName) || !ia.bodyPartsAffected().contains(boneName);
									}

								};

						renderer.meshes().getRenderableAnimatableMesh("./models/human/human_overlay.dae")
								.render(renderer, armorMask, System.currentTimeMillis() % 1000000);
					}

					e++;
				}
			}

			renderer.useShader("entities");
			for (EntityPlayer entity : renderableEntitiesIterator.getElementsInFrustrumOnly()) {

				// don't render items in hand when far
				if (renderer.getCurrentPass().name.startsWith("shadow")
						&& entity.getLocation().distance(renderer.getCamera().getCameraPosition()) > 15f)
					continue;

				TraitAnimated animation = entity.traits.get(TraitAnimated.class);

				ItemPile selectedItemPile = entity.traits.tryWith(TraitSelectedItem.class,
						eci -> eci.getSelectedItem());

				if (selectedItemPile != null) {
					Matrix4f itemMatrix = new Matrix4f();
					itemMatrix.translate(pre3f);

					itemMatrix.mul(animation.getAnimatedSkeleton().getBoneHierarchyTransformationMatrix(
							"boneItemInHand", System.currentTimeMillis() % 1000000));

					CellData cell = entity.getWorld().peekSafely(entity.getLocation());
					renderer.currentShader().setUniform2f("worldLightIn", cell.getBlocklight(), cell.getSunlight());
					selectedItemPile.getItem().getDefinition().getRenderer().renderItemInWorld(renderer,
							selectedItemPile, world, entity.getLocation(), itemMatrix);
				}
			}

			return e;
		}
	}*/

	@Override
	public String getName() {
		return name.getName();
	}
}

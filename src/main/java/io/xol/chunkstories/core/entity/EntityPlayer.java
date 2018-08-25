//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.animation.SkeletonAnimator;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.traits.*;
import io.xol.chunkstories.api.entity.traits.serializable.*;
import io.xol.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.interfaces.ItemOverlay;
import io.xol.chunkstories.api.item.interfaces.ItemZoom;
import io.xol.chunkstories.api.item.inventory.InventoryHolder;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.physics.EntityHitbox;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.entity.RenderingIterator;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.api.world.World.WorldCell;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.core.entity.components.EntityArmorInventory;
import io.xol.chunkstories.core.entity.components.EntityFoodLevel;
import io.xol.chunkstories.core.entity.components.EntityStance.EntityHumanoidStance;
import io.xol.chunkstories.core.entity.traits.MinerTrait;
import io.xol.chunkstories.core.entity.traits.TraitControlledMovement;
import io.xol.chunkstories.core.entity.traits.TraitEyeLevel;
import io.xol.chunkstories.core.entity.traits.TraitTakesFallDamage;
import io.xol.chunkstories.core.item.armor.ItemArmor;
import io.xol.chunkstories.core.item.inventory.InventoryLocalCreativeMenu;
import org.joml.*;

import java.util.Iterator;

/**
 * Core/Vanilla player, has all the functionality you'd want from it:
 * creative/survival mode, flying and walking controller...
 */
public class EntityPlayer extends EntityHumanoid implements WorldModificationCause, InventoryHolder {
	protected TraitController controllerComponent;

	protected TraitInventory inventory;
	protected TraitSelectedItem selectedItemComponent;

	protected TraitName name;
	protected TraitCreativeMode creativeMode;
	protected TraitFlyingMode flyMode;

	protected EntityArmorInventory armor;
	protected EntityFoodLevel foodLevel;

	protected TraitVoxelSelection raytracer;

	protected boolean onLadder = false;

	Location lastCameraLocation;

	int variant;

	public EntityPlayer(EntityDefinition t, Location location) {
		super(t, location);

		controllerComponent = new TraitController(this);
		inventory = new TraitInventory(this, 10, 4);
		selectedItemComponent = new TraitSelectedItem(this, inventory);
		name = new TraitName(this);
		creativeMode = new TraitCreativeMode(this);
		flyMode = new TraitFlyingMode(this);
		foodLevel = new EntityFoodLevel(this, 100);
		armor = new EntityArmorInventory(this, 4, 1);

		raytracer = new TraitVoxelSelection(this) {

			@Override
			public Location getBlockLookingAt(boolean inside, boolean can_overwrite) {
				double eyePosition = stance.get().eyeLevel;

				Vector3d initialPosition = new Vector3d(getLocation());
				initialPosition.add(new Vector3d(0, eyePosition, 0));

				Vector3d direction = new Vector3d(entityRotation.getDirectionLookingAt());

				if (inside)
					return world.collisionsManager().raytraceSelectable(new Location(world, initialPosition), direction,
							256.0);
				else
					return world.collisionsManager().raytraceSolidOuter(new Location(world, initialPosition), direction,
							256.0);
			}

		};

		new TraitInteractible(this) {

			@Override
			public boolean handleInteraction(Entity entity, Input input) {
				if (entityHealth.isDead() && input.getName().equals("mouse.right")) {

					Controller controller = entity.traits.tryWith(TraitController.class, ec -> ec.getController());
					if (controller != null && controller instanceof Player) {
						Player p = (Player) controller;
						p.openInventory(inventory);
						// p.sendMessage("HELLO THIS MY CADAVERER, PLZ FUCK OFF");
						return true;
					}
				}
				return false;
			}
		};

		new TraitRenderable(this, EntityPlayerRenderer<EntityPlayer>::new);
		new TraitDontSave(this);

		new PlayerOverlay(this);
		new TraitEyeLevel(this) {

			@Override
			public double getEyeLevel() {
				return stance.get().eyeLevel;
			}

		};

		new PlayerMovement(this);
		new PlayerWhenControlled(this);

		new MinerTrait(this);
	}

	class PlayerWhenControlled extends TraitWhenControlled {

		public PlayerWhenControlled(Entity entity) {
			super(entity);
		}

		@Override
		public void onEachFrame(LocalPlayer controller) {
			if (controller.hasFocus()) {
				moveCamera(controller);
			}
		}

		double lastPX = -1f;
		double lastPY = -1f;

		public void moveCamera(LocalPlayer controller) {
			if (entityHealth.isDead())
				return;

			double cPX = controller.getInputsManager().getMouse().getCursorX();
			double cPY = controller.getInputsManager().getMouse().getCursorY();

			double dx = 0, dy = 0;
			if (lastPX != -1f) {
				dx = cPX - controller.getWindow().getWidth() / 2.0;
				dy = cPY - controller.getWindow().getHeight() / 2.0;
			}
			lastPX = cPX;
			lastPY = cPY;

			double rotH = entityRotation.getHorizontalRotation();
			double rotV = entityRotation.getVerticalRotation();

			double modifier = 1.0f;
			ItemPile selectedItem = traits.tryWith(TraitSelectedItem.class, eci -> eci.getSelectedItem());

			if (selectedItem != null && selectedItem.getItem() instanceof ItemZoom) {
				ItemZoom item = (ItemZoom) selectedItem.getItem();
				modifier = 1.0 / item.getZoomFactor();
			}

			rotH += dx * modifier / 3f
					* controller.getClient().getConfiguration().getDoubleOption("client.input.mouseSensitivity");
			rotV -= dy * modifier / 3f
					* controller.getClient().getConfiguration().getDoubleOption("client.input.mouseSensitivity");
			entityRotation.setRotation(rotH, rotV);

			controller.getInputsManager().getMouse().setMouseCursorLocation(controller.getWindow().getWidth() / 2.0,
					controller.getWindow().getHeight() / 2.0);
		}

		@Override
		public void setupCamera(RenderingInterface renderer) {
			lastCameraLocation = getLocation();

			renderer.getCamera().setCameraPosition(new Vector3d(getLocation().add(0.0, stance.get().eyeLevel, 0.0)));

			renderer.getCamera().setRotationX(entityRotation.getVerticalRotation());
			renderer.getCamera().setRotationY(entityRotation.getHorizontalRotation());

			float modifier = 1.0f;
			if (selectedItemComponent.getSelectedItem() != null
					&& selectedItemComponent.getSelectedItem().getItem() instanceof ItemZoom) {
				ItemZoom item = (ItemZoom) selectedItemComponent.getSelectedItem().getItem();
				modifier = 1.0f / item.getZoomFactor();
			}

			float videoFov = (float) renderer.getClient().getConfiguration().getDoubleOption("client.video.fov");
			float speedEffect = (float) (entityVelocity.getVelocity().x() * entityVelocity.getVelocity().x()
					+ entityVelocity.getVelocity().z() * entityVelocity.getVelocity().z());

			speedEffect -= 0.07f * 0.07f;
			speedEffect = Math.max(0.0f, speedEffect);
			speedEffect *= 500.0f;

			renderer.getCamera().setFOV(modifier * (float) (videoFov + speedEffect));
		}

		@Override
		public boolean onControllerInput(Input input, Controller controller) {
			// We are moving inventory bringup here !
			if (input.getName().equals("inventory") && world instanceof WorldClient) {

				if (creativeMode.get()) {
					((WorldClient) getWorld()).getClient().openInventories(inventory,
							new InventoryLocalCreativeMenu(world));
				} else {
					((WorldClient) getWorld()).getClient().openInventories(inventory, armor);
				}

				return true;
			}

			Location blockLocation = raytracer.getBlockLookingAt(true, false);

			double maxLen = 1024;

			if (blockLocation != null) {
				Vector3d diff = new Vector3d(blockLocation).sub(getLocation());
				// Vector3d dir = diff.clone().normalize();
				maxLen = diff.length();
			}

			Vector3d initialPosition = new Vector3d(getLocation());
			initialPosition.add(new Vector3d(0, stance.get().eyeLevel, 0));

			Vector3dc direction = entityRotation.getDirectionLookingAt();

			Iterator<Entity> i = world.collisionsManager().rayTraceEntities(initialPosition, direction, maxLen);
			while (i.hasNext()) {
				Entity e = i.next();
				if (e != EntityPlayer.this
						&& e.traits.with(TraitInteractible.class, ti -> ti.handleInteraction(EntityPlayer.this, input)))
					return true;
			}

			ItemPile itemSelected = selectedItemComponent.getSelectedItem();
			if (itemSelected != null) {
				// See if the item handles the interaction
				if (itemSelected.getItem().onControllerInput(EntityPlayer.this, itemSelected, input, controller))
					return true;
			}
			if (getWorld() instanceof WorldMaster) {
				// Creative mode features building and picking.
				if (creativeMode.get()) {
					if (input.getName().equals("mouse.left")) {
						if (blockLocation != null) {
							// Player events mod
							if (controller instanceof Player) {
								Player player = (Player) controller;
								WorldCell cell = world.peekSafely(blockLocation);
								FutureCell future = new FutureCell(cell);
								future.setVoxel(getDefinition().store().parent().voxels().air());
								future.setBlocklight(0);
								future.setSunlight(0);
								future.setMetaData(0);

								PlayerVoxelModificationEvent event = new PlayerVoxelModificationEvent(cell, future,
										TraitCreativeMode.CREATIVE_MODE, player);

								// Anyone has objections ?
								world.getGameContext().getPluginManager().fireEvent(event);

								if (event.isCancelled())
									return true;

								Vector3d rnd = new Vector3d();
								for (int k = 0; k < 40; k++) {
									rnd.set(blockLocation);
									rnd.add(Math.random() * 0.98, Math.random() * 0.98, Math.random() * 0.98);
									world.getParticlesManager().spawnParticleAtPosition("voxel_frag", rnd);
								}
								world.getSoundManager().playSoundEffect("sounds/gameplay/voxel_remove.ogg", Mode.NORMAL,
										blockLocation, 1.0f, 1.0f);

								try {
									world.poke(future, EntityPlayer.this);
									// world.poke((int)blockLocation.x, (int)blockLocation.y, (int)blockLocation.z,
									// null, 0, 0, 0, this);
								} catch (WorldException e) {
									// Discard but maybe play some effect ?
								}

								return true;
							}
						}
					} else if (input.getName().equals("mouse.middle")) {
						if (blockLocation != null) {
							CellData ctx = getWorld().peekSafely(blockLocation);

							if (!ctx.getVoxel().isAir()) {
								// Spawn new itemPile in his inventory
								ItemVoxel item = (ItemVoxel) world.getGameContext().getContent().items()
										.getItemDefinition("item_voxel").newItem();
								item.voxel = ctx.getVoxel();
								item.voxelMeta = ctx.getMetaData();

								ItemPile itemVoxel = new ItemPile(item);
								inventory.setItemPileAt(selectedItemComponent.getSelectedSlot(), 0, itemVoxel);
								return true;
							}
						}
					}
				}
			}
			// Here goes generic entity response to interaction

			// n/a

			// Then we check if the world minds being interacted with
			return world.handleInteraction(EntityPlayer.this, blockLocation, input);
		}
	}

	class PlayerMovement extends TraitControlledMovement {

		public PlayerMovement(Entity entity) {
			super(entity);
		}

		@Override
		public void tick(LocalPlayer controller) {
			if (flyMode.get()) {
				// Delegate movement handling to the fly mode component
				flyMode.tick(controller);

				// Flying also means we're standing
				stance.set(EntityHumanoidStance.STANDING);
			} else {

				boolean focus = controller.hasFocus();
				if (focus && traits.get(TraitCollidable.class).isOnGround()) {
					if (controller.getInputsManager().getInputByName("crouch").isPressed())
						stance.set(EntityHumanoidStance.CROUCHING);
					else
						stance.set(EntityHumanoidStance.STANDING);
				}

				super.tick(controller);

				// if(focus)
				// traits.with(MinerTrait.class, mt -> mt.tickTrait());
			}

			// TODO check if this is needed
			// Instead of creating a packet and dealing with it ourselves, we instead push
			// the relevant components
			entityLocation.pushComponentEveryoneButController();
			// In that case that means pushing to the server.
		}

		@Override
		public double getForwardSpeed() {
			return ((!running || stance.get() == EntityHumanoidStance.CROUCHING) ? 0.06 : 0.09);
		}

		@Override
		public double getBackwardsSpeed() {
			return 0.05;
		}
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
					entityHealth.damage(EntityFoodLevel.HUNGER_DAMAGE_CAUSE, 1);
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

	class PlayerOverlay extends TraitHasOverlay {

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
	}

	class EntityPlayerRenderer<H extends EntityPlayer> extends EntityHumanoidRenderer<H> {
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
	}

	public Location getPredictedLocation() {
		return lastCameraLocation != null ? lastCameraLocation : getLocation();
	}

	class TraitPlayerHealth extends EntityHumanoidHealth {

		public TraitPlayerHealth(Entity entity) {
			super(entity);
		}

		@Override
		public float damage(DamageCause cause, EntityHitbox osef, float damage) {
			if (!isDead()) {
				int i = 1 + (int) Math.random() * 3;
				world.getSoundManager().playSoundEffect("sounds/entities/human/hurt" + i + ".ogg", Mode.NORMAL,
						EntityPlayer.this.getLocation(), (float) Math.random() * 0.4f + 0.8f, 5.0f);
			}

			return super.damage(cause, osef, damage);
		}
	}

	@Override
	public String getName() {
		return name.getName();
	}
}

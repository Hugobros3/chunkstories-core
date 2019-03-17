//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import java.util.Iterator;

import xyz.chunkstories.api.entity.traits.generic.TraitSerializableBoolean;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.gui.Font;
import xyz.chunkstories.api.gui.GuiDrawer;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4f;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.client.LocalPlayer;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.TraitHitboxes;
import xyz.chunkstories.api.entity.traits.serializable.TraitCreativeMode;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.ItemDefinition;
import xyz.chunkstories.api.item.interfaces.ItemCustomHoldingAnimation;
import xyz.chunkstories.api.item.interfaces.ItemOverlay;
import xyz.chunkstories.api.item.interfaces.ItemZoom;
import xyz.chunkstories.api.item.inventory.Inventory;
import xyz.chunkstories.api.item.inventory.InventoryOwner;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.physics.EntityHitbox;
import xyz.chunkstories.api.sound.SoundSource.Mode;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.World.WorldCell;
import xyz.chunkstories.api.world.WorldClient;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.core.entity.traits.TraitEyeLevel;

public class ItemFirearm extends ItemWeapon implements ItemOverlay, ItemZoom, ItemCustomHoldingAnimation {
	public final boolean automatic;
	public final double rpm;
	public final String soundName;
	public final double damage;
	public final double accuracy;
	public final double range;
	public final double soundRange;
	public final int shots;
	public final double shake;
	public final long reloadCooldown;

	public final boolean scopedWeapon;
	public final float scopeZoom;
	public final float scopeSlow;
	public final String scopeTexture;

	public final String holdingAnimationName;
	public final String shootingAnimationName;
	public final long shootingAnimationDuration;

	private boolean wasTriggerPressedLastTick = false;
	private long lastShot = 0L;

	private long cooldownEnd = 0L;
	private long animationStart = 0L;
	private long animationCooldownEnd = 0L;

	private boolean isScoped = false;

	private ItemPile currentMagazine;

	public ItemFirearm(ItemDefinition type) {
		super(type);

		automatic = type.resolveProperty("fireMode", "semiauto").equals("fullauto");
		rpm = Double.parseDouble(type.resolveProperty("roundsPerMinute", "60.0"));
		soundName = type.resolveProperty("fireSound", "sounds/weapons/ak47/shoot_old.ogg");

		damage = Double.parseDouble(type.resolveProperty("damage", "1.0"));
		accuracy = Double.parseDouble(type.resolveProperty("accuracy", "0.0"));
		range = Double.parseDouble(type.resolveProperty("range", "1000.0"));
		soundRange = Double.parseDouble(type.resolveProperty("soundRange", "1000.0"));

		reloadCooldown = Long.parseLong(type.resolveProperty("reloadCooldown", "150"));

		shots = Integer.parseInt(type.resolveProperty("shots", "1"));
		shake = Double.parseDouble(type.resolveProperty("shake", accuracy / 4.0 + ""));

		scopedWeapon = type.resolveProperty("scoped", "false").equals("true");
		scopeZoom = Float.parseFloat(type.resolveProperty("scopeZoom", "2.0"));
		scopeSlow = Float.parseFloat(type.resolveProperty("scopeSlow", "2.0"));

		scopeTexture = type.resolveProperty("scopeTexture", "./textures/gui/scope.png");

		holdingAnimationName = type.resolveProperty("holdingAnimationName", "./animations/human/holding-rifle.bvh");
		shootingAnimationName = type.resolveProperty("shootingAnimationName",
				"./animations/human/human_shoot_pistol.bvh");

		shootingAnimationDuration = Long.parseLong(type.resolveProperty("shootingAnimationDuration", "200"));
	}

	/*
	public ItemRenderer getCustomItemRenderer(ItemRenderer fallbackRenderer) {
		ItemRenderer itemRenderer;

		String modelName = getDefinition().resolveProperty("modelObj", "none");
		if (!modelName.equals("none"))
			itemRenderer = new ItemModelRenderer(this, fallbackRenderer, modelName,
					getDefinition().resolveProperty("modelDiffuse", "none"));
		else
			itemRenderer = new FlatIconItemRenderer(this, fallbackRenderer, getDefinition());

		if (scopedWeapon)
			itemRenderer = new ScopedWeaponItemRenderer(itemRenderer);

		return itemRenderer;
	}*/

	/** Displays a scope sometimes */
	/*class ScopedWeaponItemRenderer extends ItemRenderer {
		ItemRenderer actualRenderer;

		public ScopedWeaponItemRenderer(ItemRenderer itemRenderer) {
			super(null);
			this.actualRenderer = itemRenderer;
		}

		@Override
		public void renderItemInInventory(RenderingInterface renderingInterface, ItemPile pile, float screenPositionX,
										  float screenPositionY, int scaling) {
			actualRenderer.renderItemInInventory(renderingInterface, pile, screenPositionX, screenPositionY, scaling);
		}

		@Override
		public void renderItemInWorld(RenderingInterface renderingInterface, ItemPile pile, World world,
									  Location location, Matrix4f handTransformation) {
			if (pile.getInventory() != null) {
				if (pile.getInventory().getHolder() != null) {
					Entity clientEntity = renderingInterface.getClient().getPlayer().getControlledEntity();
					ItemFirearm item = (ItemFirearm) pile.getItem();

					if (item.isScoped() && clientEntity.equals(pile.getInventory().getHolder()))
						return;
				}
			}
			actualRenderer.renderItemInWorld(renderingInterface, pile, world, location, handTransformation);
		}
	}*/

	/**
	 * Should be called when the owner has this item selected
	 *
	 * @param owner
	 */
	@Override
	public void tickInHand(Entity owner, ItemPile itemPile) {
		Controller controller = owner.traits.tryWith(TraitControllable.class, TraitControllable::getController);

		// For now only client-side players can trigger shooting actions
		if (controller instanceof LocalPlayer) {
			if (!((LocalPlayer) controller).hasFocus())
				return;

			LocalPlayer player = (LocalPlayer) controller;

			if (player.getInputsManager().getInputByName("mouse.left").isPressed()) {
				// Check for bullet presence (or creative mode)
				boolean bulletPresence = owner.traits.tryWithBoolean(TraitCreativeMode.class, TraitSerializableBoolean::get)
						|| checkBullet(itemPile);
				if (!bulletPresence && !wasTriggerPressedLastTick) {
					// Play sounds
					owner.getWorld().getSoundManager().playSoundEffect("sounds/dogez/weapon/default/dry.ogg",
							Mode.NORMAL, owner.getLocation(), 1.0f, 1.0f, 1f, (float) soundRange);
				} else if ((automatic || !wasTriggerPressedLastTick)
						&& (System.currentTimeMillis() - lastShot) / 1000.0d > 1.0 / (rpm / 60.0)) {
					// Fire virtual input
					player.getInputsManager()
							.onInputPressed(controller.getInputsManager().getInputByName("shootGun"));

					lastShot = System.currentTimeMillis();
				}
			}

			isScoped = this.isScopedWeapon() && controller.getInputsManager().getInputByName("mouse.right").isPressed();
			wasTriggerPressedLastTick = controller.getInputsManager().getInputByName("mouse.left").isPressed();
		}
	}

	@Override
	public boolean onControllerInput(Entity entity, ItemPile pile, Input input, Controller controller) {
		// Don't do anything with the left mouse click
		if (input.getName().startsWith("mouse.")) {
			return true;
		}

		World world = entity.getWorld();
		if (input.getName().equals("shootGun")) {
			// Serverside checks
			// if (user.getWorld() instanceof WorldMaster)
			{
				// Is the reload cooldown done
				if (cooldownEnd > System.currentTimeMillis())
					return false;

				// Do we have any bullets to shoot
				boolean bulletPresence = entity.traits.tryWithBoolean(TraitCreativeMode.class, ecm -> ecm.get())
						|| checkBullet(pile);
				if (!bulletPresence) {
					// Dry.ogg
					return true;
				} else if (!entity.traits.tryWithBoolean(TraitCreativeMode.class, ecm -> ecm.get())) {
					consumeBullet(pile);
				}
			}

			// Jerk client view a bit
			if (entity.getWorld() instanceof WorldClient) {
				entity.traits.with(TraitRotation.class, rot -> {
					rot.applyInpulse(shake * (Math.random() - 0.5) * 3.0, shake * -(Math.random() - 0.25) * 5.0);
				});
			}

			// Play sounds
			if (controller != null) {
				world.getSoundManager().playSoundEffect(this.soundName, Mode.NORMAL, entity.getLocation(), 1.0f,
						1.0f, 1.0f, (float) soundRange);
			}

			playAnimation();

			// Raytrace shot
			Vector3d eyeLocation = new Vector3d(entity.getLocation());
			entity.traits.with(TraitEyeLevel.class, tel -> eyeLocation.y += tel.getEyeLevel());

			Vector3dc shooterDirection = entity.traits.tryWith(TraitRotation.class, er -> er.getDirectionLookingAt());
			if (shooterDirection == null)
				return false;

			// For each shot
			for (int ss = 0; ss < shots; ss++) {
				Vector3d direction = new Vector3d(shooterDirection);
				direction.normalize();

				// Find wall collision
				Location shotBlock = entity.getWorld().getCollisionsManager().raytraceSolid(eyeLocation, direction, range);
				Vector3dc nearestLocation = null;

				// Loops to try and break blocks
				boolean brokeLastBlock = false;
				while (entity.getWorld() instanceof WorldMaster && shotBlock != null) {
					WorldCell peek = entity.getWorld().peekSafely(shotBlock);
					// int data = peek.getData();
					Voxel voxel = peek.getVoxel();

					brokeLastBlock = false;
					if (!voxel.isAir() && voxel.getVoxelMaterial().resolveProperty("bulletBreakable") != null
							&& voxel.getVoxelMaterial().resolveProperty("bulletBreakable").equals("true")) {
						// TODO Spawn an event to check if it's okay

						// Destroy it
						peek.setVoxel(voxel.store().air());

						brokeLastBlock = true;
						ItemMeleeWeapon.spawnDebris(entity, direction, shotBlock);
						entity.getWorld().getSoundManager().playSoundEffect("sounds/environment/glass.ogg", Mode.NORMAL,
								shotBlock, (float) Math.random() * 0.2f + 0.9f, 1.0f);

						// Re-raytrace the ray
						shotBlock = entity.getWorld().getCollisionsManager().raytraceSolid(eyeLocation, direction, range);
					} else
						break;
				}

				// Spawn decal and particles on block the bullet embedded itself in
				if (shotBlock != null && !brokeLastBlock) {
					Location shotBlockOuter = entity.getWorld().getCollisionsManager().raytraceSolidOuter(eyeLocation,
							direction, range);

					if (shotBlockOuter != null) {
						Vector3d normal = shotBlockOuter.sub(shotBlock);

						double NbyI2x = 2.0 * direction.dot(normal);
						Vector3d NxNbyI2x = new Vector3d(normal);
						NxNbyI2x.mul(NbyI2x);

						Vector3d reflected = new Vector3d(direction);
						reflected.sub(NxNbyI2x);
						// Vector3d.sub(direction, NxNbyI2x, reflected);

						// shotBlock.setX(shotBlock.getX() + 1);

						WorldCell peek = entity.getWorld().peekSafely(shotBlock);

						// int data = user.getWorld().getVoxelData(shotBlock);
						// Voxel voxel = VoxelsStore.get().getVoxelById(data);

						// This seems fine

						for (Box box : peek.getTranslatedCollisionBoxes()) {
							Vector3dc thisLocation = box.lineIntersection(eyeLocation, direction);
							if (thisLocation != null) {
								if (nearestLocation == null
										|| nearestLocation.distance(eyeLocation) > thisLocation.distance(eyeLocation))
									nearestLocation = thisLocation;
							}
						}

						Vector3d particleSpawnPosition = new Vector3d(nearestLocation);

						// Position adjustements so shot blocks always shoot proper particles
						if (shotBlock.x() - particleSpawnPosition.x() <= -1.0)
							particleSpawnPosition.add(-0.01, 0d, 0d);
						if (shotBlock.y() - particleSpawnPosition.y() <= -1.0)
							particleSpawnPosition.add(0d, -0.01, 0d);
						if (shotBlock.z() - particleSpawnPosition.z() <= -1.0)
							particleSpawnPosition.add(0d, 0d, -0.01);

						for (int i = 0; i < 25; i++) {
							Vector3d untouchedReflection = new Vector3d(reflected);

							Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
									Math.random() * 2.0 - 1.0);
							random.mul(0.5);
							untouchedReflection.add(random);
							untouchedReflection.normalize();

							untouchedReflection.mul(0.25);

							world.getParticlesManager().spawnParticleAtPositionWithVelocity("voxel_frag",
									particleSpawnPosition, untouchedReflection);
						}

						world.getSoundManager().playSoundEffect(
								peek.getVoxel().getVoxelMaterial().resolveProperty("landingSounds"), Mode.NORMAL,
								particleSpawnPosition, 1, 0.05f);
						world.getDecalsManager().add(nearestLocation, normal.negate(), new Vector3d(0.5), "bullethole");
					}
				}

				// Hitreg takes place on server bois
				if (entity.getWorld() instanceof WorldMaster) {
					// Iterate over each found entities
					Iterator<Entity> shotEntities = entity.getWorld().getCollisionsManager().rayTraceEntities(eyeLocation,
							direction, 256f);
					while (shotEntities.hasNext()) {
						Entity shotEntity = shotEntities.next();
						// Don't shoot itself & only living things get shot
						if (!shotEntity.equals(entity)) {
							TraitHitboxes hitboxes = shotEntity.traits.get(TraitHitboxes.class);
							TraitHealth health = shotEntity.traits.get(TraitHealth.class);

							if (health != null && hitboxes != null) {

								// Get hit location
								for (EntityHitbox hitBox : hitboxes.getHitBoxes()) {
									Vector3dc hitPoint = hitBox.lineIntersection(eyeLocation, direction);

									if (hitPoint == null)
										continue;

									// System.out.println("shot" + hitBox.getName());

									// Deal damage
									health.damage(pileAsDamageCause(pile), hitBox, (float) damage);

									// Spawn blood particles
									Vector3d bloodDir = direction.normalize().mul(0.75);
									for (int i = 0; i < 120; i++) {
										Vector3d random = new Vector3d(Math.random() * 2.0 - 1.0,
												Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0);
										random.mul(0.25);
										random.add(bloodDir);

										entity.getWorld().getParticlesManager()
												.spawnParticleAtPositionWithVelocity("blood", hitPoint, random);
									}

									// Spawn blood on walls
									if (nearestLocation != null)
										entity.getWorld().getDecalsManager().add(nearestLocation, bloodDir,
												new Vector3d(Math.min(3, shots) * damage / 20f), "blood");
								}
							}
						}
					}
				}

			}

			world.getParticlesManager().spawnParticleAtPosition("muzzle", eyeLocation);

			FirearmShotEvent event = new FirearmShotEvent(this, entity, controller);
			entity.getWorld().getGameContext().getPluginManager().fireEvent(event);

			return (entity.getWorld() instanceof WorldMaster);

		}
		return false;
	}

	private boolean checkBullet(ItemPile weaponInstance) {
		if (currentMagazine == null)
			if (!findMagazine(weaponInstance))
				return false;

		if (currentMagazine.getAmount() <= 0) {
			currentMagazine = null;
			return false;
		}

		return true;

	}

	private void consumeBullet(ItemPile weaponInstance) {
		assert currentMagazine != null;

		currentMagazine.setAmount(currentMagazine.getAmount() - 1);

		if (currentMagazine.getAmount() <= 0) {
			currentMagazine.getInventory().setItemAt(currentMagazine.getX(), currentMagazine.getY(), null, 0 , false);
			currentMagazine = null;

			// Set reload cooldown
			if (findMagazine(weaponInstance))
				cooldownEnd = System.currentTimeMillis() + this.reloadCooldown;
		}
	}

	private boolean findMagazine(ItemPile weaponInstance) {
		Inventory inventory = weaponInstance.getInventory();
		for (ItemPile pile : weaponInstance.getInventory().getContents()) {
			if (pile != null && pile.getItem() instanceof ItemFirearmMagazine) {
				ItemFirearmMagazine magazineItem = (ItemFirearmMagazine) pile.getItem();
				if (magazineItem.isSuitableFor(this) && pile.getAmount() > 0) {
					currentMagazine = pile;
					break;
				}
			}
		}
		return currentMagazine != null;
	}

	@Override
	public void drawItemOverlay(GuiDrawer drawer, ItemPile pile) {
		InventoryOwner holder = pile.getInventory().getOwner();
		if (holder instanceof Entity) {
			Controller controller = ((Entity) holder).traits.tryWith(TraitControllable.class, TraitControllable::getController);
			if(controller instanceof LocalPlayer) {
				if (isScoped())
					drawScope(drawer);

				if (cooldownEnd > System.currentTimeMillis()) {
					String reloadText = "Reloading weapon, please wait";

					Font font = drawer.getFonts().defaultFont();
					int cooldownLength = font.getWidth(reloadText);
					drawer.drawString(-cooldownLength + drawer.getGui().getViewportWidth() / 2,drawer.getGui().getViewportHeight() / 2, reloadText);
				}
			}
		}
	}

	private void drawScope(GuiDrawer drawer) {
		// Temp, rendering interface should provide us
		int min = Math.min(drawer.getGui().getViewportWidth(), drawer.getGui().getViewportHeight());
		int max = Math.max(drawer.getGui().getViewportWidth(), drawer.getGui().getViewportHeight());

		int bandwidth = (max - min) / 2;
		int x = 0;

		drawer.drawBox(x, 0, x += bandwidth, drawer.getGui().getViewportHeight(), new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
		drawer.drawBox(x, 0, x += min, drawer.getGui().getViewportHeight(), scopeTexture);
		drawer.drawBox(x, 0, x += bandwidth, drawer.getGui().getViewportHeight(), new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
	}

	public boolean isScoped() {
		return isScoped;
	}

	public boolean isScopedWeapon() {
		return scopedWeapon;
	}

	public float getZoomFactor() {
		return isScoped ? scopeZoom : 1f;
	}

	public float getScopeSlow() {
		return scopeSlow;
	}

	public void playAnimation() {
		this.animationStart = System.currentTimeMillis();
		this.animationCooldownEnd = this.animationStart + this.shootingAnimationDuration;
	}

	@Override
	public String getCustomAnimationName() {
		if (this.animationCooldownEnd > System.currentTimeMillis())
			return this.shootingAnimationName;
		return holdingAnimationName;
	}

	@Override
	public double transformAnimationTime(double originalTime) {
		if (this.animationCooldownEnd > System.currentTimeMillis()) {
			return System.currentTimeMillis() - this.animationStart;
		}
		return originalTime;
	}
}

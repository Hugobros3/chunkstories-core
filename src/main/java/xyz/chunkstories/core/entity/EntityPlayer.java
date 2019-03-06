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
import xyz.chunkstories.api.gui.GuiDrawer;
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

		new PlayerOverlay(this);
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

	@Override
	public String getName() {
		return name.getName();
	}
}

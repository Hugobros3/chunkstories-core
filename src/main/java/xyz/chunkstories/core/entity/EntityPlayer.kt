//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.MeleeWeapon
import xyz.chunkstories.api.entity.traits.TraitDontSave
import xyz.chunkstories.api.entity.traits.TraitInteractible
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.api.gui.inventory.InventoryManagementUIPanel
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.InventoryOwner
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.util.getUniqueColorCode
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.entity.traits.*
import java.util.*

/**
* Core/Vanilla player, has all the functionality you'd want from it:
* creative/survival mode, flying and walking controller...
*/
class EntityPlayer(t: EntityDefinition, world: World) : EntityHumanoid(t, world), InventoryOwner {
	private val controllerComponent: TraitControllable

	protected var traitInventory: TraitInventory
	internal var traitSelectedItem: TraitSelectedItem

	private val traitName: TraitName
	internal var traitCreativeMode: TraitCreativeMode
	internal var traitFlyingMode: TraitFlyingMode

	internal var traitArmor: TraitArmor
	private val traitFoodLevel: TraitFoodLevel

	internal var traitSight: TraitSight

	internal var lastCameraLocation: Location? = null
	internal var variant: Int = 0

	val name: String
		get() = traitName.name

	init {
		traitInventory = object : TraitInventory(this, 10, 4) {
			override fun createMainInventoryPanel(inventory: Inventory, layer: Layer): InventoryManagementUIPanel? {
				return inventory.run {
					//val crafts = loadRecipes(recipesTest, entity.world.content)
					val craftingStation = entity.traits[TraitCrafting::class]?.getCraftingStation()
					val craftingAreaSideSize = craftingStation?.craftingAreaSideSize ?: 0

					val ui = InventoryManagementUIPanel(layer, width * 20 + 16, height * 20 + 16 + 8 + 20 * 3 + 8 + 8)
					for (x in 0 until width) {
						for (y in 0 until height) {
							val slot = InventorySlot.RealSlot(this, x, y)
							val uiSlot = ui.InventorySlotUI(slot, x * 20 + 8, y * 20 + 8)
							ui.slots.add(uiSlot)
						}
					}

					entity.traits[TraitArmor::class]?.inventory?.let {
						for(armorSlot in 0 until it.width) {
							val slot = InventorySlot.RealSlot(it, armorSlot, 0)
							val uiSlot = ui.InventorySlotUI(slot, 0 * 20 + 8,  84 + armorSlot * 20 + 8)
							ui.slots.add(uiSlot)
						}
					}

					craftingStation?.bringUpCraftingMenuSlots(ui, 8 + height * 20 + 8)

					ui
				}
			}
		}
		traitSelectedItem = TraitSelectedItem(this, traitInventory)
		traitName = TraitName(this)
		traitCreativeMode = TraitCreativeMode(this)
		traitFlyingMode = TraitFlyingMode(this)
		traitFoodLevel = TraitFoodLevel(this, 100f)
		traitArmor = TraitArmor(this, 4, 1)

		traitSight =
				object : TraitSight(this) {
					override val headLocation: Location
						get() = Location(world, Vector3d(location).add(0.0, traitStance.stance.eyeLevel, 0.0))
					override val lookingAt: Vector3dc
						get() = traitRotation.directionLookingAt

				}

		val fists = object : MeleeWeapon {
			override val name = "fists"
			override val damage = 15f
			override val warmupMillis = 20
			override val cooldownMillis = 180
			override val reach = 5.0
			override val attackSound = "sounds/entities/human/punch_attack.ogg"
		}
		TraitMeleeCombat(this, fists)
		TraitHealthFoodOverlay(this)
		TraitDontSave(this)

		object : TraitEyeLevel(this) {
			override val eyeLevel: Double
				get() = traitStance.stance.eyeLevel

		}

		TraitMaybeFlyControlledMovement(this)
		controllerComponent = PlayerController(this)

		object : TraitInteractible(this) {
			override fun handleInteraction(entity: Entity, input: Input): Boolean {
				if (traitHealth.isDead && input.name == "mouse.right") {

					val controller = this@EntityPlayer.controller//entity.traits.tryWith(TraitControllable.class, TraitControllable::getController);
					if (controller is Player) {
						TODO("open inventory stuff again")
						// controller.openInventory(traitInventory.inventory)
						return true
					}
				}
				return false
			}
		}

		TraitMining(this)

		traitHealth =
				object : EntityHumanoidHealth(this) {
					override fun playDamageSound() {
						if (!isDead) {
							val i = 1 + Math.random().toInt() * 3
							entity.world.soundManager.playSoundEffect("sounds/entities/human/hurt$i.ogg", SoundSource.Mode.NORMAL, entity.location, Math.random().toFloat() * 0.4f + 0.8f, 5.0f)
						}
					}
				}

		TraitCrafting(this)
		TraitCanPickupItems(this)

		val variant = getUniqueColorCode(name) % 6
		val customSkin = MeshMaterial("playerSkin", mapOf("albedoTexture" to "./models/human/variant$variant.png"), "opaque")
		EntityHumanoidRenderer(this, customSkin)
		PlayerCamera(this)
	}
}
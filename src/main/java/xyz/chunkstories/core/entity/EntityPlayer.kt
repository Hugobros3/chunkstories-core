//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.TraitDontSave
import xyz.chunkstories.api.entity.traits.TraitInteractible
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.gui.Layer
import xyz.chunkstories.api.gui.inventory.InventorySlot
import xyz.chunkstories.api.gui.inventory.InventoryUI
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.InventoryOwner
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.entity.traits.*
import java.util.*

/**
 * Core/Vanilla player, has all the functionality you'd want from it:
 * creative/survival mode, flying and walking controller...
 */
class EntityPlayer(t: EntityDefinition, world: World) : EntityHumanoid(t, world), WorldModificationCause, InventoryOwner, DamageCause {
    private val controllerComponent: TraitControllable

    protected var traitInventory: TraitInventory
    internal var traitSelectedItem: TraitSelectedItem

    private val traitName: TraitName
    internal var traitCreativeMode: TraitCreativeMode
    internal var traitFlyingMode: TraitFlyingMode

    internal var traitArmor: TraitArmor
    private val traitFoodLevel: TraitFoodLevel

    internal var traitSight: TraitSight

    //private val onLadder = false

    internal var lastCameraLocation: Location? = null
    internal var variant: Int = 0

    override val name: String
        get() = traitName.name

    init {
        traitInventory = object : TraitInventory(this, 10, 4) {
            override fun createMainInventoryPanel(inventory: Inventory, layer: Layer): InventoryUI? {
                val craftingAreaSideSize = 3
                return inventory.run {
                    //val crafts = loadRecipes(recipesTest, entity.world.content)

                    val ui = InventoryUI(layer, width * 20 + 16, height * 20 + 16 + 8 + 20 * craftingAreaSideSize + 8)
                    for (x in 0 until width) {
                        for (y in 0 until height) {
                            val slot = InventorySlot.RealSlot(this, x, y)
                            val uiSlot = ui.InventorySlotUI(slot, x * 20 + 8, y * 20 + 8)
                            ui.slots.add(uiSlot)
                        }
                    }

                    val craftSizeReal = 20 * craftingAreaSideSize
                    val offsetx = ui.width / 2 - craftSizeReal / 2

                    val craftingSlots = Array(craftingAreaSideSize) { y ->
                        Array(craftingAreaSideSize) { x ->
                            InventorySlot.FakeSlot()
                        }
                    }
                    val craftingUiSlots = Array(craftingAreaSideSize) { y ->
                        Array(craftingAreaSideSize) { x ->
                            val slot = craftingSlots[y][x]
                            val uiSlot = ui.InventorySlotUI(slot, offsetx + x * 20, 8 + height * 20 + 8 + (craftingAreaSideSize - y - 1) * 20)
                            uiSlot
                        }
                    }

                    craftingUiSlots.forEach { it.forEach { ui.slots.add(it) } }

                    val outputSlot = object : InventorySlot.SummoningSlot() {

                        override val visibleContents: Pair<Item, Int>?
                            get() {
                                val recipe = entity.world.content.recipes.getRecipeForInventorySlots(craftingSlots)
                                if (recipe != null)
                                    return Pair(recipe.result.first.newItem(), recipe.result.second)
                                return null
                            }

                        //TODO use packet to talk the server into this
                        override fun commitTransfer(destinationInventory: Inventory, destX: Int, destY: Int, amount: Int) {
                            val recipe = entity.world.content.recipes.getRecipeForInventorySlots(craftingSlots) ?: return
                            repeat(amount / recipe.result.second) {
                                recipe.craftUsing(craftingSlots, destinationInventory, destX, destY)
                            }
                        }
                    }

                    ui.slots.add(ui.InventorySlotUI(outputSlot, offsetx + craftSizeReal + 20, 8 + height * 20 + 8 + 1 * 20))

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

                    val controller = controllerComponent.controller//entity.traits.tryWith(TraitControllable.class, TraitControllable::getController);
                    if (controller is Player) {
                        controller.openInventory(traitInventory.inventory)
                        return true
                    }
                }
                return false
            }
        }

        MinerTrait(this)

        traitHealth =
                object : EntityHumanoidHealth(this) {
                    override fun playDamageSound() {
                        if (!isDead) {
                            val i = 1 + Math.random().toInt() * 3
                            entity.world.soundManager.playSoundEffect("sounds/entities/human/hurt$i.ogg", SoundSource.Mode.NORMAL, entity.location, Math.random().toFloat() * 0.4f + 0.8f, 5.0f)
                        }
                    }
                }

        TraitCanPickupItems(this)

        val variant = ColorsTools.getUniqueColorCode(name) % 6
        val aaTchoum = HashMap<String, String>()
        aaTchoum["albedoTexture"] = "./models/human/variant$variant.png"
        val customSkin = MeshMaterial("playerSkin", aaTchoum, "opaque")
        EntityHumanoidRenderer(this, customSkin)
    }
}
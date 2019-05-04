//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.EntityGroundItem
import xyz.chunkstories.api.entity.traits.*
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.inventory.InventoryOwner
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.util.ColorsTools
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.traits.TraitArmor
import xyz.chunkstories.core.entity.traits.TraitFoodLevel
import xyz.chunkstories.core.entity.traits.MinerTrait
import xyz.chunkstories.core.entity.traits.TraitEyeLevel
import xyz.chunkstories.core.entity.traits.TraitTakesFallDamage
import org.joml.*

import java.util.HashMap

/**
 * Core/Vanilla player, has all the functionality you'd want from it:
 * creative/survival mode, flying and walking controller...
 */
class EntityPlayer(t: EntityDefinition, world: World) : EntityHumanoid(t, world), WorldModificationCause, InventoryOwner {
    private val controllerComponent: TraitControllable

    protected var traitInventory: TraitInventory
    internal var traitSelectedItem: TraitSelectedItem

    private val traitName: TraitName
    internal var traitCreativeMode: TraitCreativeMode
    internal var traitFlyingMode: TraitFlyingMode

    internal var traitArmor: TraitArmor
    private val traitFoodLevel: TraitFoodLevel

    internal var traitVoxelSelection: TraitVoxelSelection

    private val onLadder = false

    internal var lastCameraLocation: Location? = null
    internal var variant: Int = 0

    override val name: String
        get() = traitName.name

    init {

        //controllerComponent = new TraitController(this);
        traitInventory = TraitInventory(this, 10, 4)
        traitSelectedItem = TraitSelectedItem(this, traitInventory)
        traitName = TraitName(this)
        traitCreativeMode = TraitCreativeMode(this)
        traitFlyingMode = TraitFlyingMode(this)
        traitFoodLevel = TraitFoodLevel(this, 100f)
        traitArmor = TraitArmor(this, 4, 1)

        traitVoxelSelection = object : TraitVoxelSelection(this) {

            override fun getBlockLookingAt(inside: Boolean, can_overwrite: Boolean): Location? {
                val eyePosition = traitStance.stance.eyeLevel

                val initialPosition = Vector3d(location)
                initialPosition.add(Vector3d(0.0, eyePosition, 0.0))

                val direction = Vector3d(traitRotation.directionLookingAt)

                return if (inside)
                    this@EntityPlayer.world.collisionsManager.raytraceSelectable(Location(this@EntityPlayer.world, initialPosition), direction,
                            256.0)
                else
                    this@EntityPlayer.world.collisionsManager.raytraceSolidOuter(Location(this@EntityPlayer.world, initialPosition), direction,
                            256.0)
            }

        }

        PlayerOverlay(this)
        TraitDontSave(this)

        object : TraitEyeLevel(this) {

            override val eyeLevel: Double
                get() = traitStance.stance.eyeLevel

        }

        PlayerMovementController(this)
        controllerComponent = EntityPlayerController(this)

        object : TraitInteractible(this) {
            override fun handleInteraction(entity: Entity, input: Input): Boolean {
                if (traitHealth.isDead && input.name == "mouse.right") {

                    val controller = controllerComponent.controller//entity.traits.tryWith(TraitControllable.class, TraitControllable::getController);
                    if (controller is Player) {
                        val p = controller as Player?
                        p!!.openInventory(traitInventory.inventory)
                        return true
                    }
                }
                return false
            }
        }

        MinerTrait(this)

        traitHealth = TraitPlayerHealth(this)

        val variant = ColorsTools.getUniqueColorCode(name) % 6
        val aaTchoum = HashMap<String, String>()
        aaTchoum["albedoTexture"] = "./models/human/variant$variant.png"
        val customSkin = MeshMaterial("playerSkin", aaTchoum, "opaque")
        EntityHumanoidRenderer(this, customSkin)
    }

    // Server-side updating
    override fun tick() {

        val world = world

        // if(world instanceof WorldMaster)
        traits[MinerTrait::class]?.tickTrait()

        // Tick item in hand if one such exists
        val pileSelected = this.traits[TraitSelectedItem::class]?.selectedItem
        if (pileSelected != null)
            pileSelected.item.tickInHand(this, pileSelected)

        // Auto-pickups items on the ground
        if (world is WorldMaster && world.ticksElapsed % 60L == 0L) {

            for (e in world.getEntitiesInBox(location, Vector3d(3.0))) {
                if (e is EntityGroundItem && e.location.distance(this.location) < 3.0f) {
                    if (!e.canBePickedUpYet())
                        continue

                    world.soundManager.playSoundEffect("sounds/item/pickup.ogg", Mode.NORMAL, location, 1.0f, 1.0f)

                    val groundInventoy = e.traits[TraitInventory::class.java]!!.inventory

                    val pileToCollect = groundInventoy.getItemPileAt(0, 0)

                    val overflow = this.traitInventory.inventory.addItem(pileToCollect!!.item, pileToCollect.amount)
                    pileToCollect.amount = overflow

                    if (pileToCollect.amount <= 0)
                        world.removeEntity(e)
                }
            }
        }

        if (world is WorldMaster) {
            // Food/health subsystem handled here decrease over time

            // Take damage when starving
            // TODO: move to trait
            if (world.ticksElapsed % 100L == 0L) {
                if (traitFoodLevel.getValue() == 0f)
                    traitHealth.damage(TraitFoodLevel.HUNGER_DAMAGE_CAUSE, 1f)
                else {
                    // 27 minutes to start starving at 0.1 starveFactor
                    // Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

                    // Starve slowly if inactive
                    var starve = 0.03f

                    // Walking drains you
                    if (this.traitVelocity.velocity.length() > 0.3) {
                        starve = 0.06f
                        // Running is even worse
                        if (this.traitVelocity.velocity.length() > 0.7)
                            starve = 0.15f
                    }

                    val newfoodLevel = traitFoodLevel.getValue() - starve
                    traitFoodLevel.setValue(newfoodLevel)
                }
            }

            // It restores hp
            // TODO move to trait
            if (traitFoodLevel.getValue() > 20 && !traitHealth.isDead) {
                if (traitHealth.getHealth() < traitHealth.maxHealth) {
                    traitHealth.setHealth(traitHealth.getHealth() + 0.01f)

                    val newfoodLevel = traitFoodLevel.getValue() - 0.01f
                    traitFoodLevel.setValue(newfoodLevel)
                }
            }

            // Being on a ladder resets your jump height
            if (onLadder)
                traits[TraitTakesFallDamage::class]?.resetFallDamage()

            // So does flying
            if (traitFlyingMode.get())
                traits[TraitTakesFallDamage::class]?.resetFallDamage()
        }

        super.tick()

    }
}
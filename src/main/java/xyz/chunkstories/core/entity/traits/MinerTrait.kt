//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.voxel.MiningTool
import xyz.chunkstories.core.item.BlockMiningOperation

class MinerTrait(entity: Entity) : Trait(entity) {

    var progress: BlockMiningOperation? = null
        private set

    private val hands: MiningTool = object : MiningTool {

        override val miningEfficiency: Float
            get() = 1f

        override val toolTypeName: String
            get() = "hands"

    }

    init {

        if (entity !is WorldModificationCause)
            throw RuntimeException("Sorry but only entities implementing WorldModificationCause may be miners.")
    }

    fun tickTrait() {
        val tool = hands

        val world = entity.world

        val controller = entity.traits[TraitControllable::class]?.controller

        if (controller is Player) {
            val inputs = controller.inputsManager

            var lookingAt = entity.traits[TraitSight::class]?.getSelectableBlockLookingAt(5.0)?.location

            if (lookingAt != null && lookingAt.distance(entity.location) > 7f)
                lookingAt = null

            if (lookingAt != null && inputs.getInputByName("mouse.left")!!.isPressed) {

                val cell = world.peek(lookingAt)

                // Cancel mining if looking away or the block changed by itself
                if (progress != null && (lookingAt.distance(progress!!.loc) > 0 || !cell.voxel!!.sameKind(progress!!.voxel!!))) {
                    progress = null
                }

                if (progress == null) {
                    // Try starting mining something
                    progress = BlockMiningOperation(world.peek(lookingAt), tool)
                } else {
                    progress!!.keepGoing(entity, controller)
                }
            } else {
                progress = null
            }
        }


    }
}

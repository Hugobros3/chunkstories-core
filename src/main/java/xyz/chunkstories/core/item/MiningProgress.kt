//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityGroundItem
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.WorldException
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.materials.VoxelMaterial
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.cell.FutureCell

class MiningProgress(val context: CellData, internal var tool: MiningTool) {
    val voxel: Voxel?
    val material: VoxelMaterial
    val loc: Location

    var progress: Float = 0.toFloat()
    val started: Long

    val materialHardnessForThisTool: Float
    internal var timesSoundPlayed = 0

    init {
        this.loc = context.location
        // toolType = tool != null ? tool.toolType : "hand";

        voxel = context.voxel
        material = voxel!!.voxelMaterial
        var hardnessString: String? = null

        // First order, check the voxel itself if it states a certain hardness for this
        // tool type
        hardnessString = voxel.definition.resolveProperty("hardnessFor" + tool.toolTypeName)

        // Then check if the voxel states a general hardness multiplier
        if (hardnessString == null)
            hardnessString = voxel.definition.resolveProperty("hardness")

        // if the voxel is devoid of information, we do the same on the material
        if (hardnessString == null)
            hardnessString = material.resolveProperty("materialHardnessFor" + tool.toolTypeName)

        // Eventually we default to 1.0
        if (hardnessString == null)
            hardnessString = material.resolveProperty("materialHardness", "1.0")

        this.materialHardnessForThisTool = java.lang.Float.parseFloat(hardnessString)

        this.progress = 0.0f
        this.started = System.currentTimeMillis()
    }

    fun keepGoing(owner: Entity, controller: Controller): MiningProgress? {
        // Progress using efficiency / ticks per second
        progress += tool.miningEfficiency / 60f / materialHardnessForThisTool

        if (progress >= 1.0f) {
            if (owner.world is WorldMaster) {

                val future = FutureCell(context)
                future.voxel = owner.world.content.voxels().air()

                // Check no one minds
                val event = PlayerVoxelModificationEvent(context, future, owner as WorldModificationCause, controller as Player)
                owner.world.gameContext.pluginManager.fireEvent(event)

                // Break the block
                if (!event.isCancelled) {
                    spawnBlockDestructionParticles(loc, context.world)

                    context.world.soundManager.playSoundEffect("sounds/gameplay/voxel_remove.ogg", Mode.NORMAL, loc, 1.0f, 1.0f)

                    val itemSpawnLocation = Location(context.world, loc)
                    itemSpawnLocation.add(0.5, 0.0, 0.5)

                    // Drop loot !
                    for (drop in context.voxel!!.getLoot(context, owner as WorldModificationCause)) {
                        val thrownItem = context.world.content.entities().getEntityDefinition("groundItem")!!.newEntity<EntityGroundItem>(itemSpawnLocation.world)
                        thrownItem.traitLocation.set(itemSpawnLocation)
                        thrownItem.entityVelocity.setVelocity(Vector3d(Math.random() * 0.125 - 0.0625, 0.1, Math.random() * 0.125 - 0.0625))
                        thrownItem.traits[TraitInventory::class]!!.inventory.addItem(drop.first, drop.second)
                        context.world.addEntity(thrownItem)
                    }

                    try {
                        context.world.poke(future, owner as WorldModificationCause)
                    } catch (e: WorldException) {
                        // Didn't work
                        // TODO make some ingame effect so as to clue in the player why it failed
                    }

                }
            }

            return null
        }

        return this
    }

    companion object {

        fun spawnBlockDestructionParticles(loc: Location, world: World) {
            val rnd = Vector3d()
            for (i in 0..39) {
                rnd.set(loc)
                rnd.add(Math.random() * 0.98, Math.random() * 0.98, Math.random() * 0.98)
                world.particlesManager.spawnParticleAtPosition("voxel_frag", rnd)
            }
        }
    }

}

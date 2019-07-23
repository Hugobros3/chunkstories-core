//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.voxel.MiningTool
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.materials.VoxelMaterial
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.CellData

/** Represents a mining operation in progress, keeps all the state related to that */
class BlockMiningOperation(val cell: CellData, internal var tool: MiningTool) {
    val voxel: Voxel
    val material: VoxelMaterial
    val loc: Location

    var progress: Float = 0.toFloat()
    val started: Long

    val materialHardnessForThisTool: Float
    internal var timesSoundPlayed = 0

    init {
        this.loc = cell.location

        voxel = cell.voxel
        material = voxel.voxelMaterial
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

    fun keepGoing(owner: Entity, controller: Controller): BlockMiningOperation? {
        // Progress using efficiency / ticks per second
        progress += tool.miningEfficiency / 60f / materialHardnessForThisTool

        if (progress >= 1.0f) {
            if (owner.world is WorldMaster) {
                voxel.breakBlock(cell, tool, owner)
            }

            return null
        }

        return this
    }

}

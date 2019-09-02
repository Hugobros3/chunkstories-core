//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.voxel.MiningTool
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.materials.VoxelMaterial
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell

/** Represents a mining operation in progress, keeps all the state related to that */
class BlockMiningOperation(val cell: Cell, internal var tool: MiningTool) {
    val voxel: Voxel
    val material: VoxelMaterial
    val location: Location

    var progress: Double = 0.0
    val started: Long
    var lastSound: Long = 0

    val miningDifficulty: Double
    val toolAppropriate: Boolean
    val miningEfficiency: Double
    //val materialHardnessForThisTool: Float
    internal var timesSoundPlayed = 0

    init {
        this.location = cell.location

        voxel = cell.voxel
        material = voxel.voxelMaterial

        miningDifficulty = voxel.definition.resolveProperty("miningDifficulty", material.resolveProperty("miningDifficulty", "1.0")).toDouble()
        val preferredTool = voxel.definition.resolveProperty("mineUsing", material.resolveProperty("mineUsing", "nothing_in_particular"))
        toolAppropriate = (preferredTool == "nothing_in_particular" || tool.toolTypeName == preferredTool)
        if(toolAppropriate)
            miningEfficiency = tool.miningEfficiency.toDouble()
        else
            miningEfficiency = 1.0

        println("$miningDifficulty $preferredTool $toolAppropriate $tool ${tool.toolTypeName} $miningEfficiency")
        //var hardnessString: String? = null

        // First order, check the voxel itself if it states a certain hardness for this
        // tool type
        /*hardnessString = voxel.definition.resolveProperty("hardnessFor" + tool.toolTypeName)

        // Then check if the voxel states a general hardness multiplier
        if (hardnessString == null)
            hardnessString = voxel.definition.resolveProperty("hardness")

        // if the voxel is devoid of information, we do the same on the material
        if (hardnessString == null)
            hardnessString = material.resolveProperty("materialHardnessFor" + tool.toolTypeName)

        // Eventually we default to 1.0
        if (hardnessString == null)
            hardnessString = material.resolveProperty("materialHardness", "1.0")*/

        //this.materialHardnessForThisTool = java.lang.Float.parseFloat(hardnessString)

        this.progress = 0.0
        this.started = System.currentTimeMillis()
    }

    fun keepGoing(entity: Entity, controller: Controller): BlockMiningOperation? {
        // Progress using efficiency / ticks per second
        progress += miningEfficiency / 60f / miningDifficulty

        if (progress >= 1.0f) {
            if (entity.world is WorldMaster) {
                val minedSound = voxel.voxelMaterial.resolveProperty("minedSounds")
                if(minedSound != null)
                    entity.world.soundManager.playSoundEffect(minedSound, SoundSource.Mode.NORMAL, location, 0.95f + Math.random().toFloat() * 0.10f, 1.0f)
                voxel.breakBlock(cell, tool, entity)
            }

            return null
        }

        val now = System.currentTimeMillis()
        if(now - lastSound > 300) {
            lastSound = now
            val miningSound = voxel.voxelMaterial.resolveProperty("miningSounds")
            //println("mining: $miningSound")
            if(miningSound != null)
                entity.world.soundManager.playSoundEffect(miningSound, SoundSource.Mode.NORMAL, location, 0.85f + Math.random().toFloat() * 0.10f, 1.0f)
        }

        return this
    }

}

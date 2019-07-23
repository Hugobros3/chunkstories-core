//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.ChunkMeshRenderingInterface
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.CellData

class VoxelVine(definition: VoxelDefinition) : Voxel(definition), VoxelClimbable {

    private val model: Model
    private val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

    init {
        model = definition.store.parent.models[definition.resolveProperty("model", "voxels/blockmodels/vine/vine.dae")]

        customRenderingRoutine = { cell ->
            render(cell, this)
        }
    }

    fun render(cell: CellData, mesher: ChunkMeshRenderingInterface) {
        val meta = cell.metaData
        /*val rotation = when (meta % 4) {
            0 -> 2
            1 -> 1
            2 -> 0
            3 -> 3
            else -> -1
        }*/
        for(rotation in 0..3) {
            if((meta shr rotation) and 0x1 == 0)
                continue

            val matrix = Matrix4f()
            matrix.translate(0.5f, 0.5f, 0.5f)
            matrix.rotate(Math.PI.toFloat() * 0.5f * (-rotation + 1), 0f, 1f, 0f)
            matrix.translate(-0.5f, -0.5f, -0.5f)
            mesher.addModel(model, matrix, mappedOverrides)
        }
    }

    override fun getCollisionBoxes(info: CellData): Array<Box>? {

        val meta = info.metaData
        if (meta == 1)
            return arrayOf(Box(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9))
        if (meta == 2)
            return arrayOf(Box(0.1, 1.0, 1.0).translate(0.0, 0.0, 0.0))
        if (meta == 4)
            return arrayOf(Box(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.0))
        return if (meta == 8) arrayOf(Box(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0)) else super.getCollisionBoxes(info)

    }
}

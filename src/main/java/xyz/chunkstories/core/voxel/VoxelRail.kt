//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.CellData


class VoxelRail(definition: VoxelDefinition) : Voxel(definition) {
    val model: Model

    init {
        model = definition.store.parent().models["voxels/blockmodels/rails/rails.dae"]

        //val mappedOverrides = mapOf(0 to MeshMaterial("material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

        customRenderingRoutine = { cell ->
            val matrix = Matrix4f()

            if (!cell.getNeightborVoxel(VoxelSide.FRONT.ordinal)!!.sameKind(this@VoxelRail)) {
                matrix.translate(0.5f , 0f, 0.5f)
                matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
                matrix.translate(-0.5f , 0f, -0.5f)
            }

            addModel(model, matrix)
        }
    }
}
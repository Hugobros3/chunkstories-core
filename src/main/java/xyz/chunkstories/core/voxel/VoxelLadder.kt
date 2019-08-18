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
import xyz.chunkstories.api.world.cell.Cell

class VoxelLadder(definition: VoxelDefinition) : Voxel(definition), VoxelClimbable {

    private val model: Model
    private val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

    init {
        model = definition.store.parent.models[definition.resolveProperty("model", "voxels/blockmodels/vine/vine.dae")]

        customRenderingRoutine = { cell ->
            render(cell, this)
        }
    }

    fun render(cell: Cell, mesher: ChunkMeshRenderingInterface) {
        val meta = cell.metaData
        val rotation = when (meta % 4) {
            0 -> 2
            1 -> 0
            2 -> 1
            3 -> 3
            else -> -1
        }
        val matrix = Matrix4f()

        matrix.translate(0.5f, 0.5f, 0.5f)
        matrix.rotate(Math.PI.toFloat() * 0.5f * rotation, 0f, 1f, 0f)
        matrix.translate(-0.5f, -0.5f, -0.5f)

        mesher.addModel(model, matrix, mappedOverrides)
    }

    /*@Override
	public VoxelModel getVoxelRenderer(CellData info) {
		int meta = info.getMetaData();

		if (meta == 2)
			return models[2];
		else if (meta == 3)
			return models[3];
		else if (meta == 4)
			return models[0];
		else if (meta == 5)
			return models[1];
		return models[0];
	}*/

    override fun getCollisionBoxes(cell: Cell): Array<Box>? {

        val meta = cell.metaData

        if (meta == 2)
            return arrayOf(Box.fromExtents(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9))
        if (meta == 3)
            return arrayOf(Box.fromExtents(1.0, 1.0, 0.1))
        if (meta == 4)
            return arrayOf(Box.fromExtents(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0))
        return if (meta == 5) arrayOf(Box.fromExtents(0.1, 1.0, 1.0)) else super.getCollisionBoxes(cell)

    }

}

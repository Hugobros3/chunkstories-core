//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.world.cell.Cell

class VoxelLadder(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content), VoxelClimbable {
	private val model: Model = content.models[definition["model"].asString ?: "voxels/blockmodels/vine/vine.dae"]
	private val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.textures[BlockSide.FRONT.ordinal].name}.png")))

	override fun loadRepresentation(): BlockRepresentation {
		return BlockRepresentation.Custom { cell ->
			render(cell, this)
		}
	}

	fun render(cell: Cell, mesher: BlockRepresentation.Custom.RenderInterface) {
		val meta = cell.data.extraData
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

	override fun getCollisionBoxes(cell: Cell) = when (cell.data.extraData) {
		2 -> arrayOf(Box.fromExtents(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9))
		3 -> arrayOf(Box.fromExtents(1.0, 1.0, 0.1))
		4 -> arrayOf(Box.fromExtents(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0))
		5 -> arrayOf(Box.fromExtents(0.1, 1.0, 1.0))
		else -> super.getCollisionBoxes(cell)
	}

}

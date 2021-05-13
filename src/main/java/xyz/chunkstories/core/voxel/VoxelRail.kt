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
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.world.WorldCell

class VoxelRail(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	val model: Model = content.models[definition["representation.model"].asString ?: ""]

	init {
		//val mappedOverrides = mapOf(0 to MeshMaterial("material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[BlockSide.FRONT.ordinal].name}.png")))
	}

	override fun loadRepresentation() = BlockRepresentation.Custom { cell ->
		val matrix = Matrix4f()

		if (cell is WorldCell && cell.getNeighbour(BlockSide.FRONT)?.data?.blockType?.sameKind(this@VoxelRail) == true) {
			matrix.translate(0.5f, 0f, 0.5f)
			matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
			matrix.translate(-0.5f, 0f, -0.5f)
		}

		addModel(model, matrix)
	}
}
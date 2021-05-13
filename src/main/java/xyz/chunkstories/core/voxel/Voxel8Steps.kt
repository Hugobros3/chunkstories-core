//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.world.cell.Cell

class Voxel8Steps(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	val meshes: Array<Pair<Model, Map<Int, MeshMaterial>>>

	init {
	    val steps = definition["steps"].asDict?.elements ?: emptyMap()

		val models = mutableListOf<Pair<Model, Map<Int, MeshMaterial>>>()

		for(i in 0..7) {
			val step = steps["$i"].asDict ?: Json.Dict(emptyMap())

			val representation = step["representation"].asDict ?: Json.Dict(emptyMap())
			val model = content.models[representation["model"].asString ?: "voxels/blockmodels/cube/cube.dae"]

			val mappedOverrides = deriveModelOverridesForFaceTextures(model)

			models.add(Pair(model, mappedOverrides))
		}

		meshes = models.toTypedArray()
	}

	override fun loadRepresentation(): BlockRepresentation {
		return BlockRepresentation.Custom { cell ->
			val (model, overrides) = meshes[cell.data.extraData % 8]
			addModel(model, materialsOverrides = overrides)
		}
	}

	override fun isFaceOpaque(cell: Cell, side: BlockSide) = when (side) {
		BlockSide.BOTTOM -> true
		BlockSide.TOP -> true
		else -> super.isFaceOpaque(cell, side)
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box> {
		val box2 = Box.fromExtents(1.0, (cell.data.extraData % 8 + 1) / 8.0, 1.0)
		return arrayOf(box2)
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

class Voxel8Steps(definition: VoxelDefinition) : Voxel(definition) {
	val meshes: Array<Pair<Model, Map<Int, MeshMaterial>>>

	init {
	    val steps = definition["steps"].asDict?.elements ?: emptyMap()

		val models = mutableListOf<Pair<Model, Map<Int, MeshMaterial>>>()

		for(i in 0..7) {
			val step = steps["$i"].asDict ?: Json.Dict(emptyMap())

			val representation = step["representation"].asDict ?: Json.Dict(emptyMap())
			val model = definition.store.parent.models[representation["model"].asString ?: "voxels/blockmodels/cube/cube.dae"]

			val mappedOverrides = deriveModelOverridesForFaceTextures(model)

			models.add(Pair(model, mappedOverrides))
		}

		meshes = models.toTypedArray()

		customRenderingRoutine = {
			cell ->
			val (model, overrides) = meshes[cell.metaData % 8]
			addModel(model, materialsOverrides = overrides)
		}
	}

	override fun isFaceOpaque(side: VoxelSide, metadata: Int): Boolean {
		if (side == VoxelSide.BOTTOM)
			return true
		return if (side == VoxelSide.TOP) true else super.isFaceOpaque(side, metadata)

	}

	override fun getCollisionBoxes(cell: Cell): Array<Box>? {
		val box2 = Box.fromExtents(1.0, (cell.metaData % 8 + 1) / 8.0, 1.0)
		return arrayOf(box2)
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

class VoxelHalfTile(definition: VoxelDefinition) : Voxel(definition) {
	private val topModel: Model = definition.store.parent.models["voxels/blockmodels/half_step/upper_half.dae"]
	private val bottomModel: Model = definition.store.parent.models["voxels/blockmodels/half_step/lower_half.dae"]

	init {

		val mappedOverridesTop = deriveModelOverridesForFaceTextures(topModel)
		val mappedOverridesBot = deriveModelOverridesForFaceTextures(topModel)

		customRenderingRoutine = { cell ->
			if (bottomOrTop(cell.metaData))
				addModel(bottomModel, materialsOverrides = mappedOverridesBot)
			else
				addModel(topModel, materialsOverrides = mappedOverridesTop)
		}
	}

	private fun bottomOrTop(meta: Int): Boolean {
		return meta % 2 == 0
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box>? {
		val box2 = Box.fromExtents(1.0, 0.5, 1.0)
		if (bottomOrTop(cell.metaData))
			box2.translate(0.0, -0.0, 0.0)
		else
			box2.translate(0.0, +0.5, 0.0)
		return arrayOf(box2)
	}

	override fun getLightLevelModifier(cell: Cell, out: Cell, side: VoxelSide): Int {
		val sideInt = side.ordinal

		// Special cases when half-tiles meet
		if (out.voxel is VoxelHalfTile && sideInt < 4) {
			// If they are the same type, allow the light to transfer
			return if (bottomOrTop(cell.metaData) == bottomOrTop(out.metaData))
				2
			else
				15
		}
		if (bottomOrTop(cell.metaData) && sideInt == 5)
			return 15
		return if (!bottomOrTop(cell.metaData) && sideInt == 4) 15 else 2
	}
}

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
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData

class VoxelHalfTile(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	private val topModel: Model = content.models["voxels/blockmodels/half_step/upper_half.dae"]
	private val bottomModel: Model = content.models["voxels/blockmodels/half_step/lower_half.dae"]

	override fun loadRepresentation(): BlockRepresentation {
		val mappedOverridesTop = deriveModelOverridesForFaceTextures(topModel)
		val mappedOverridesBot = deriveModelOverridesForFaceTextures(topModel)

		return BlockRepresentation.Custom { cell ->
			if (bottomOrTop(cell.data.extraData))
				addModel(bottomModel, materialsOverrides = mappedOverridesBot)
			else
				addModel(topModel, materialsOverrides = mappedOverridesTop)
		}
	}

	private fun bottomOrTop(meta: Int): Boolean {
		return meta % 2 == 0
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box> {
		val box2 = Box.fromExtents(1.0, 0.5, 1.0)
		if (bottomOrTop(cell.data.extraData))
			box2.translate(0.0, -0.0, 0.0)
		else
			box2.translate(0.0, +0.5, 0.0)
		return arrayOf(box2)
	}

	override fun getLightLevelModifier(cellData: CellData, neighborData: CellData, side: BlockSide): Int {
		val sideInt = side.ordinal

		// Special cases when half-tiles meet
		return when {
			neighborData.blockType is VoxelHalfTile && sideInt < 4 -> {
				// If they are the same type, allow the light to transfer
				when {
					bottomOrTop(cellData.extraData) == bottomOrTop(neighborData.extraData) -> 2
					else -> 15
				}
			}
			bottomOrTop(cellData.extraData) && sideInt == 5 -> 15
			!bottomOrTop(cellData.extraData) && sideInt == 4 -> 15
			else -> 2
		}
	}
}

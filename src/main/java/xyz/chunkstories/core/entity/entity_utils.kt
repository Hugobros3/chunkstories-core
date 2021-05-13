//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.world.cell.Cell

fun Entity.blocksWithin(): Sequence<Cell> {
	val entityBox = this.getTranslatedBoundingBox()

	return world.getCellsInBox(entityBox).mapNotNull {
		cell ->

		for (voxelBox in cell.data.blockType.getTranslatedCollisionBoxes(cell)) {
			if (voxelBox.collidesWith(entityBox))
				return@mapNotNull cell
		}
		null
	}
}
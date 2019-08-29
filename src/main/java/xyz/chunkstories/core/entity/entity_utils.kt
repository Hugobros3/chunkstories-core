package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.world.cell.Cell

fun Entity.blocksWithin(): Collection<Cell> {
    val entityBox = this.getTranslatedBoundingBox() ?: return emptyList()

    return world.getVoxelsWithin(entityBox).mapNotNull {
        if (it == null)
            return@mapNotNull null

        val cell = world.peek(it.x, it.y, it.z)

        for (voxelBox in cell.voxel.getTranslatedCollisionBoxes(cell) ?: return@mapNotNull null) {
            if (voxelBox.collidesWith(entityBox))
                return@mapNotNull cell
        }
        null
    }
}
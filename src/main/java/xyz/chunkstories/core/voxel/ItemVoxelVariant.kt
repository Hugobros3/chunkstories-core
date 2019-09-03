//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.world.cell.FutureCell

class ItemVoxelVariant(definition: ItemDefinition) : ItemVoxel(definition) {
	val metadata = definition["metaData"].asInt ?: 0
	val variant = definition["variant"].asString

	override fun getTextureName(): String {
		return "voxels/textures/" + voxel.name + "/" + variant + ".png"
	}

	override fun changeBlockData(cell: FutureCell, placingEntity: Entity): Boolean {
		super.changeBlockData(cell, placingEntity)
		cell.metaData = metadata
		return true
	}
}
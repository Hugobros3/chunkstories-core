//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.world.chunk.MutableChunkCell

class VoxelWater(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {

	override fun tick(cell: MutableChunkCell) {

	}
}
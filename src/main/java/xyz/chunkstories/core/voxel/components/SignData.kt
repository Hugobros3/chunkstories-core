//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel.components

import xyz.chunkstories.api.block.BlockAdditionalData
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.world.GameInstance
import xyz.chunkstories.api.world.chunk.ChunkCell

class SignData(cell: ChunkCell) : BlockAdditionalData(cell) {

	var signText = ""
		set(value) {
			field = value
			TODO("refresh")
		    // cell.refreshRepresentation()
		}

	override fun serialize(gameInstance: GameInstance): Json = Json.Value.Text(signText)

	override fun deserialize(gameInstance: GameInstance, json: Json) {
		signText = json.asString ?: signText
	}
}

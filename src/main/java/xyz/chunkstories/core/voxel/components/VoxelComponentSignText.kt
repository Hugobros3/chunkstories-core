//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel.components

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.voxel.components.VoxelComponent
import xyz.chunkstories.api.world.cell.CellComponents
import xyz.chunkstories.api.world.serialization.StreamSource
import xyz.chunkstories.api.world.serialization.StreamTarget

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class VoxelComponentSignText(holder: CellComponents) : VoxelComponent(holder) {

	var signText = ""
		set(value) {
			field = value
			holder.cell.refreshRepresentation()
		}

	override fun serialize(): Json? = Json.Value.Text(signText)

	override fun deserialize(json: Json) {
		signText = json.asString ?: signText
	}
}

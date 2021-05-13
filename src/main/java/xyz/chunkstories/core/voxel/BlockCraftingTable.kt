package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.world.chunk.MutableChunkCell

class BlockCraftingTable(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
    val craftingAreaSize = definition["craftingAreaSize"]?.asInt ?: 1

    override fun onInteraction(entity: Entity, cell: MutableChunkCell, input: Input): Boolean {
        if(input.name == "mouse.right") {
            TODO("???")
        }

        return super.onInteraction(entity, cell, input)
    }
}
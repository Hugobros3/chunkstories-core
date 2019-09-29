package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.world.chunk.ChunkCell

class BlockCraftingTable(definition: VoxelDefinition) : Voxel(definition) {
    val craftingAreaSize = definition["craftingAreaSize"]?.asInt ?: 1

    override fun handleInteraction(entity: Entity, voxelContext: ChunkCell, input: Input): Boolean {
        if(input.name == "mouse.right") {
        }

        return super.handleInteraction(entity, voxelContext, input)
    }
}
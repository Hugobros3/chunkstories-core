//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.converter.Mapper
import xyz.chunkstories.api.world.cell.MutableCellData

class Slab(blockType: BlockType) : Mapper(blockType) {

    override fun output(minecraftId: Int, minecraftMeta: Byte, data: MutableCellData) {
        data.blockType = blockType
        if (minecraftMeta >= 8)
            data.extraData = 1
    }
}
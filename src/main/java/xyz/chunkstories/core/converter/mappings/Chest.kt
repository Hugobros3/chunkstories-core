//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import io.xol.enklume.MinecraftRegion
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.converter.NonTrivialMapper
import xyz.chunkstories.api.world.World

class Chest(blockType: BlockType) : NonTrivialMapper(blockType) {

    override fun output(csWorld: World, csX: Int, csY: Int, csZ: Int, minecraftBlockId: Int, minecraftMetaData: Int,
                        region: MinecraftRegion, minecraftCurrentChunkXinsideRegion: Int, minecraftCurrentChunkZinsideRegion: Int,
                        x: Int, y: Int, z: Int) {
        csWorld.getCellMut(csX, csY, csZ)!!.data.blockType = blockType
    }

}
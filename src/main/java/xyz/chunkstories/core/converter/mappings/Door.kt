//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.voxel.VoxelDoor
import io.xol.enklume.MinecraftRegion
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.converter.NonTrivialMapper

import xyz.chunkstories.api.util.compatibility.getSideMcDoor
import xyz.chunkstories.api.world.cell.CellData

class Door(blockType: BlockType) : NonTrivialMapper(blockType) {

    override fun output(csWorld: World, csX: Int, csY: Int, csZ: Int, minecraftBlockId: Int, minecraftMetaData: Int,
                        region: MinecraftRegion, minecraftCuurrentChunkXinsideRegion: Int, minecraftCuurrentChunkZinsideRegion: Int,
                        x: Int, y: Int, z: Int) {

        val upper = minecraftMetaData and 0x8 shr 3
        val open = minecraftMetaData and 0x4 shr 2

        // We only place the lower half of the door and the other half is created by the
        // placing logic of chunk stories
        if (upper != 1) {
            val upperMeta = region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion).getBlockMeta(x, y + 1, z)

            val hingeSide = upperMeta and 0x01
            val direction = minecraftMetaData and 0x3

            val cell = csWorld.getCellMut(csX, csY, csZ)!!
            cell.data = CellData(
                    blockType = blockType,
                    extraData = VoxelDoor.computeMeta(open == 1, hingeSide == 1, getSideMcDoor(direction))
            )
        }

    }
}
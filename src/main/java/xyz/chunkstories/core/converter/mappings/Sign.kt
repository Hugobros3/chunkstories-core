//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import io.xol.enklume.MinecraftChunk
import io.xol.enklume.MinecraftRegion
import io.xol.enklume.nbt.NBTCompound
import io.xol.enklume.nbt.NBTInt
import io.xol.enklume.nbt.NBTList
import io.xol.enklume.nbt.NBTString
import io.xol.enklume.util.SignParseUtil
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.converter.NonTrivialMapper
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.PodCellData
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.core.voxel.VoxelSign
import xyz.chunkstories.core.voxel.components.SignData

class Sign(blockType: BlockType) : NonTrivialMapper(blockType) {

    override fun output(csWorld: World, csX: Int, csY: Int, csZ: Int, minecraftBlockId: Int, minecraftMetaData: Int,
                        region: MinecraftRegion, minecraftCuurrentChunkXinsideRegion: Int, minecraftCuurrentChunkZinsideRegion: Int,
                        x: Int, y: Int, z: Int) {
        var minecraftMetaData = minecraftMetaData

        if (blockType is VoxelSign) {

            if (!blockType.name.endsWith("_post")) {
                when (minecraftMetaData) {
                    2 -> minecraftMetaData = 8
                    3 -> minecraftMetaData = 0
                    4 -> minecraftMetaData = 4
                    5 -> minecraftMetaData = 12
                }
            }

            csWorld.setCellData(csX, csY, csZ, PodCellData(blockType))

            (csWorld.getCellMut(csX, csY, csZ) as ChunkCell).additionalData["signData"]?.let {
                translateSignText(it as SignData, region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion), x, y, z)
            }
        } else {
            throw Exception("Inconsistent behavior, expected sign")
        }
    }

    private fun translateSignText(target: SignData, minecraftChunk: MinecraftChunk, x: Int, y: Int, z: Int) {
        val root = minecraftChunk.rootTag ?: return

        val entitiesList = root.getTag("Level.TileEntities") as NBTList

        target.signText = "<corresponding sign not found :(>"

        for (element in entitiesList.elements) {
            val entity = element as NBTCompound
            val entityId = entity.getTag("id") as NBTString

            val tileX = (entity.getTag("x") as NBTInt).data
            val tileY = (entity.getTag("y") as NBTInt).data
            val tileZ = (entity.getTag("z") as NBTInt).data

            if (entityId.data.toLowerCase() == "sign" || entityId.data.toLowerCase() == "minecraft:sign") {
                if (tileX and 0xF != x || tileY != y || tileZ and 0xF != z) {
                    continue
                }

                val text1 = SignParseUtil.parseSignData((entity.getTag("Text1") as NBTString).data)
                val text2 = SignParseUtil.parseSignData((entity.getTag("Text2") as NBTString).data)
                val text3 = SignParseUtil.parseSignData((entity.getTag("Text3") as NBTString).data)
                val text4 = SignParseUtil.parseSignData((entity.getTag("Text4") as NBTString).data)

                val textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4
                target.signText = textComplete
                break

            }
        }
    }
}
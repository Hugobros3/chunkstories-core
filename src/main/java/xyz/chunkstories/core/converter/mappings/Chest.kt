//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import io.xol.enklume.MinecraftRegion
import xyz.chunkstories.api.converter.mappings.NonTrivialMapper
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.cell.FutureCell

class Chest(voxel: Voxel) : NonTrivialMapper(voxel) {

    override fun output(csWorld: World, csX: Int, csY: Int, csZ: Int, minecraftBlockId: Int, minecraftMetaData: Int,
                        region: MinecraftRegion, minecraftCurrentChunkXinsideRegion: Int, minecraftCurrentChunkZinsideRegion: Int,
                        x: Int, y: Int, z: Int) {
        csWorld.pokeSimpleSilently(FutureCell(csWorld, csX, csY, csZ, voxel))
    }

}
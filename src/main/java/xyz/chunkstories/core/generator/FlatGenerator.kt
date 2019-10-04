//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition

import java.util.Random

class FlatGenerator(definition: WorldGeneratorDefinition, world: World) : WorldGenerator(definition, world) {
    internal var rnd = Random()

    private var worldsize: Int = 0

    private val cellSize: Int

    private val GROUND_VOXEL: Voxel
    private val WALL_VOXEL: Voxel
    private val WALL_TOP_VOXEL: Voxel

    init {
        worldsize = world.sizeInChunks * 32

        this.GROUND_VOXEL = world.gameContext.content.voxels.getVoxel("grass")!!
        this.WALL_VOXEL = world.gameContext.content.voxels.getVoxel("cobble")!!
        this.WALL_TOP_VOXEL = world.gameContext.content.voxels.getVoxel("iron_bars")!!

        this.cellSize = definition["cellSize"].asInt ?: 0
    }

    override fun generateWorldSlice(chunks: Array<Chunk>) {
        for (chunkY in chunks.indices) {
            generateChunk(chunks[chunkY])
        }
    }

    private fun generateChunk(chunk: Chunk) {
        val cx = chunk.chunkX
        val cy = chunk.chunkY
        val cz = chunk.chunkZ

        rnd.setSeed((cx * 32 + cz + 48716148).toLong())

        for (x in 0..31)
            for (z in 0..31) {
                var type = WALL_VOXEL // cobble

                var terrainHeight = 21

                if (cellSize != 0 && onWall(cx * 32 + x, cz * 32 + z)) {
                    terrainHeight = 30
                } else {
                    type = GROUND_VOXEL
                }

                var y = cy * 32
                while (y < cy * 32 + 32 && y <= terrainHeight) {
                    if (y == 30)
                        type = WALL_TOP_VOXEL
                    chunk.pokeSimpleSilently(x, y, z, type, -1, -1, 0)
                    y++
                }
            }
    }

    private fun onWall(x: Int, z: Int) = ((x) % 256 == 0 || (z) % 256 == 0)
}

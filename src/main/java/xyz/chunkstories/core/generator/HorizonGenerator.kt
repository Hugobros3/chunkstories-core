//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import org.joml.Vector3i
import org.joml.Vector4f
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.structures.Prefab
import xyz.chunkstories.api.block.structures.PrefabPasteFlags
import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asDouble
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.converter.MinecraftBlocksTranslator
import xyz.chunkstories.api.math.random.SeededSimplexNoiseGenerator
import xyz.chunkstories.api.math.random.WeightedSet
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import java.util.*
import kotlin.math.abs
import xyz.chunkstories.api.math.MathUtils.clamp
import xyz.chunkstories.api.world.cell.CellData

open class HorizonGenerator(definition: WorldGeneratorDefinition, world: World) : WorldGenerator(definition, world) {
    private val ssng = SeededSimplexNoiseGenerator(world.properties.seed)
    private val worldSizeInBlocks = world.properties.size.sizeInChunks * 32

    private val waterHeight = definition.properties["waterHeight"].asInt ?: 48
    private val mountainScale = definition.properties["mountainScale"].asDouble ?: 128.0
    private val mountainOffset = definition.properties["mountainOffset"].asDouble ?: 0.3
    private val baseHeightScale = definition.properties["baseHeightScale"].asInt ?: 64
    private val plateauHeightScale = definition.properties["plateauHeightScale"].asInt ?: 24

    private val airVoxel: BlockType = world.gameInstance.content.blockTypes.air
    private val stoneVoxel: BlockType = world.gameInstance.content.blockTypes.get("stone")!!
    private val waterVoxel: BlockType = world.gameInstance.content.blockTypes.get("water")!!

    private val translator = MinecraftBlocksTranslator(world.gameInstance, world.gameInstance.content.getAsset("converter_mapping.txt")!!)
    private val biomes = loadBiomesFromJson(world.gameInstance.content, definition.properties["biomes"].asDict
            ?: throw Exception("World generator ${definition.name} lacks a 'biomes' section."), translator)
    private val caveBuilder = Caves(world, this)

    private val spawnableMinerals = definition.properties["minerals"].asArray?.let { loadOresSpawningSection(it, world.gameInstance.content) } ?: emptyList()

    interface Biome {
        val surfaceBlock: BlockType
        val groundBlock: BlockType
        val underwaterGround: BlockType

        val surfaceDecorationsDensity: Double
        val surfaceDecorations: WeightedSet<BlockType>

        val treesDensity: Double
        val treesVariants: WeightedSet<Prefab>

        val additionalStructures: WeightedSet<Prefab>
    }

    internal inner class SliceData(private val cx: Int, private val cz: Int) {
        val heights: IntArray = IntArray(1024)
        val biomes: Array<Biome>

        val caveBits = mutableListOf<CaveSegmentToPaste>()
        val prefabs = mutableListOf<PrefabToPaste>()

        init {
            for (x in 0..31) {
                for (z in 0..31) {
                    heights[x * 32 + z] = getHeightAtInternal(cx * 32 + x, cz * 32 + z)
                }
            }
            biomes = Array(1024) { xz ->
                val x = xz shr 5
                val z = xz and 0x1f
                decideBiome(cx * 32 + x, cz * 32 + z, heights[x * 32 + z])
            }
        }
    }

    private val tempRngSeed = Vector4f(666.0f, -78.0f, 31.0f, 98.0f)

    fun decideBiome(x: Int, z: Int, worldHeight: Int): Biome {
        val temperature = fractalNoise(x, z, 2, 0.25f, 0.5f, tempRngSeed) * 0.5 + 0.5

        var biome: Biome = when {
            temperature < 0.3 -> biomes["snowland"]
            temperature > 0.7 -> biomes["desert"]
            else -> biomes["grassland"]
        }!!

        //TODO more complex logic than this !
        if (worldHeight < waterHeight + 3) {
            biome = biomes["beach"] ?: biome
        }

        return biome
    }

    internal class PrefabToPaste(var prefab: Prefab, var position: Vector3i, var flags: PrefabPasteFlags)
    internal class CaveSegmentToPaste(var segment: CaveSnakeSegment, var position: Vector3i)

    override fun generateWorldSlice(chunks: Array<PreChunk>) {
        val cx = chunks[0].chunkX
        val cz = chunks[0].chunkZ

        val sliceData = SliceData(cx, cz)

        caveBuilder.generateCaves(cx, cz, sliceData)

        for (chunkY in chunks.indices) {
            val chunk = chunks[chunkY]
            val cy = chunk.chunkY
            for (x in 0..31) {
                for (z in 0..31) {
                    val groundHeight = sliceData.heights[x * 32 + z]
                    var y = cy * 32
                    while (y < cy * 32 + 32 && y <= groundHeight) {
                        chunk.setCellData(x, y, z, CellData(stoneVoxel))
                        y++
                    }
                }
            }
            sliceData.caveBits.forEach {
                it.segment.paste(chunk, it.position)
            }
        }

        sliceData.caveBits.clear()
    }

    override fun generateWorldSlicePhaseII(chunks: Array<Chunk>) {
        val cx = chunks[0].chunkX
        val cz = chunks[0].chunkZ
        val sliceData = SliceData(cx, cz)
        val rng = Random("$cx$cz".hashCode().toLong())

        // Ores
        for (oreType in spawnableMinerals) {
            val nSamples = 32 // Try 32 locations (out of 1024 possible)
            for (n in 0 until nSamples) {
                val x = rng.nextInt(32)
                val z = rng.nextInt(32)
                val h = getHeightAtInternal(cx * 32 + x, cz * 32 + z)

                // Sample one 32th the total possible space
                for (n2 in 0 until h / 32) {
                    val y = rng.nextInt(h)
                    val chance = rng.nextDouble()

                    if(y !in oreType.heightRange)
                        continue

                    // Boost freq by 32*32 since we didn't sampler every block and frequency is given per-block
                    if(chance < oreType.frequency * 32.0 * 32.0 ) {
                        val spawnAmount = oreType.amount.random()
                        for(s in 0 until spawnAmount) {
                            val dx = -1 + rng.nextInt(4)
                            val dy = -1 + rng.nextInt(4)
                            val dz = -1 + rng.nextInt(4)
                            if(chunks.getOrNull((y + dy) / 32)?.getCellData(x + dx, y + dy, z + dz)?.blockType == stoneVoxel)
                                chunks.getOrNull((y + dy) / 32)?.setCellData(x + dx, y + dy, z + dz, CellData(oreType.voxel))
                        }
                    }
                }
            }
        }

        val maxheight = chunks.size * 32 - 1
        for (x in 0..31) {
            for (z in 0..31) {
                val biome = sliceData.biomes[x * 32 + z]

                var y = maxheight
                val worldY = sliceData.heights[x * 32 + z]

                while (y > 0 && chunks[y / 32].getCellData(x, y, z).blockType == airVoxel) {
                    y--
                }
                val groundHeightActual = y

                // It's flooded!
                if (groundHeightActual < waterHeight && worldY < waterHeight) {
                    var waterY = waterHeight
                    while (waterY > 0 && chunks[waterY / 32].getCellData(x, waterY, z).blockType == airVoxel) {
                        chunks[waterY / 32].setCellData(x, waterY, z, CellData(waterVoxel))
                        waterY--
                    }

                    // Replace the stone with whatever is the ground block
                    var undergroundDirt = groundHeightActual
                    while (undergroundDirt >= 0 && undergroundDirt >= groundHeightActual - 3 && chunks[undergroundDirt / 32].getCellData(x, undergroundDirt, z).blockType == stoneVoxel) {
                        chunks[undergroundDirt / 32].setCellData(x, undergroundDirt, z, CellData(biome.underwaterGround))
                        undergroundDirt--
                    }
                } else {
                    // Top soil
                    chunks[groundHeightActual / 32].setCellData(x, groundHeightActual, z, CellData(biome.surfaceBlock))

                    // Replace the stone with whatever is the ground block
                    var undergroundDirt = groundHeightActual - 1
                    while (undergroundDirt >= 0 && undergroundDirt >= groundHeightActual - 3 && chunks[undergroundDirt / 32].getCellData(x, undergroundDirt, z).blockType == stoneVoxel) {
                        chunks[undergroundDirt / 32].setCellData(x, undergroundDirt, z, CellData(biome.groundBlock))
                        undergroundDirt--
                    }

                    // Decoration shrubs flowers etc
                    val surface = groundHeightActual + 1
                    if (surface > maxheight)
                        continue

                    val bushChance = rng.nextDouble()
                    if (bushChance < biome.surfaceDecorationsDensity) {
                        chunks[surface / 32].setCellData(x, surface, z, CellData(biome.surfaceDecorations.random(rng)))
                    }
                }
            }
        }

        addTrees(cx, cz, sliceData)

        for (chunkY in chunks.indices) {
            sliceData.prefabs.forEach { stp ->
                // TODO ???
                world.pastePrefab(stp.position.x, stp.position.y, stp.position.z, stp.prefab)
                // stp.prefab.paste(chunks[chunkY], stp.position, stp.flags)
            }
        }
    }

    private fun addTrees(cx: Int, cz: Int, data: SliceData) {
        val rnd = Random()
        // take into account the nearby chunks
        for (gcx in cx - 1..cx + 1)
            for (gcz in cz - 1..cz + 1) {

                // Consistency
                rnd.setSeed((gcx * 32 + gcz + 48716148).toLong())

                // Sample that many locations and try to spawn a tree there
                val ntrees = 32
                for (i in 0 until ntrees) {
                    val x = gcx * 32 + rnd.nextInt(32)
                    val z = gcz * 32 + rnd.nextInt(32)

                    val worldHeight = getHeightAtInternal(x, z)
                    val biome = decideBiome(x, z, worldHeight)

                    if (rnd.nextDouble() < biome.treesDensity) {
                        val y = getHeightAtInternal(x, z) - 1
                        if (y > waterHeight) {
                            val treeStructure = biome.treesVariants.random(rnd)
                            data.prefabs.add(PrefabToPaste(treeStructure, Vector3i(x, y, z), PrefabPasteFlags()))
                        }
                    }
                }
            }
    }

    private fun fractalNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float, offset: Vector4f): Float {
        val x = x / worldSizeInBlocks.toFloat()
        val z = z / worldSizeInBlocks.toFloat()

        var frequency = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        frequency *= (worldSizeInBlocks / (64 * 32)).toFloat()
        for (i in 0 until octaves) {
            total += ssng.looped_noise(x, z, offset, Vector4f(frequency * worldSizeInBlocks.toFloat() / 4096.0f)) * amplitude
            frequency *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    private fun ridgedNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float, offset: Vector4f): Float {
        val x = x / worldSizeInBlocks.toFloat()
        val z = z / worldSizeInBlocks.toFloat()

        var frequency = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        frequency *= (worldSizeInBlocks / (64 * 32)).toFloat()

        for (i in 0 until octaves) {
            total += (1.0f - abs(ssng.looped_noise(x, z, offset, Vector4f(frequency * worldSizeInBlocks.toFloat() / 4096.0f)))) * amplitude
            frequency *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    val rng1 = Vector4f(-47.0f, 154.0f, 126.0f, 148.0f)
    val rng2 = Vector4f(0.0f, 154.0f, 121.0f, -48.0f)
    val rng3 = Vector4f(245.0f, 87.0f, -33.0f, -88.0f)
    val rng4 = Vector4f(0.0f, 154.0f, 121.0f, -48.0f)
    val rng5 = Vector4f(-5752.0f, -2200.0f, -457.0f, 948.0f)

    open fun getHeightAtInternal(x: Int, z: Int): Int {
        var height = 0.0

        val maxHeight = fractalNoise(x, z, 1, 1.0f, 0.5f, rng1)
        height += (32f + 48 * maxHeight + 48f * maxHeight * maxHeight) * ridgedNoise(x, z, 2, 1.0f, 0.5f, rng2)

        var roughness = fractalNoise(x, z, 1, 1.0f, 0.5f, rng3)
        roughness = clamp(roughness * 2.5f - 0.33f, 0.25f, 1.0f)

        height += 32f * roughness * fractalNoise(x, z, 4, 8.0f, 0.5f, rng4)

        height += 32 * clamp(
                ridgedNoise(x, z, 2, 1.0f, 0.5f, rng5) * 2.0 - 1.0, 0.0, 1.0)

        return height.toInt()
    }
}
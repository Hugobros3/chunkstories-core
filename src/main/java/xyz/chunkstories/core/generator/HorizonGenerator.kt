//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import org.joml.Vector3i
import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asDouble
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.converter.MinecraftBlocksTranslator
import xyz.chunkstories.api.math.Math2
import xyz.chunkstories.api.math.random.SeededSimplexNoiseGenerator
import xyz.chunkstories.api.math.random.WeightedSet
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.structures.Structure
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import java.util.*
import kotlin.math.abs

open class HorizonGenerator(definition: WorldGeneratorDefinition, world: World) : WorldGenerator(definition, world) {
    private val ssng = SeededSimplexNoiseGenerator(world.worldInfo.seed)
    private val worldSizeInBlocks = world.sizeInChunks * 32

    private val waterHeight = definition["waterHeight"].asInt ?: 48
    private val mountainScale = definition["mountainScale"].asDouble ?: 128.0
    private val mountainOffset = definition["mountainOffset"].asDouble ?: 0.3
    private val baseHeightScale = definition["baseHeightScale"].asInt ?: 64
    private val plateauHeightScale = definition["plateauHeightScale"].asInt ?: 24

    private val airVoxel: Voxel = world.gameContext.content.voxels.air
    private val stoneVoxel: Voxel = world.gameContext.content.voxels.getVoxel("stone")!!
    private val waterVoxel: Voxel = world.gameContext.content.voxels.getVoxel("water")!!

    private val translator = MinecraftBlocksTranslator(world.gameContext, world.content.getAsset("converter_mapping.txt"))
    private val biomes = loadBiomesFromJson(world.content, definition["biomes"].asDict
            ?: throw Exception("World generator ${definition.name} lacks a 'biomes' section."), translator)
    private val caveBuilder = CaveBuilder(world, this)

    private val spawnableMinerals = definition["minerals"].asArray?.let { loadOresSpawningSection(it, world.content) } ?: emptyList()

    interface Biome {
        val surfaceBlock: Voxel
        val groundBlock: Voxel
        val underwaterGround: Voxel

        val surfaceDecorationsDensity: Double
        val surfaceDecorations: WeightedSet<Voxel>

        val treesDensity: Double
        val treesVariants: WeightedSet<Structure>

        val additionalStructures: WeightedSet<Structure>
    }

    internal inner class SliceData(private val cx: Int, private val cz: Int) {
        val heights = IntArray(1024)
        val structures = mutableListOf<StructureToPaste>()
        val biome = Array<Biome>(1024) { xz ->
            val x = xz shr 5
            val z = xz and 0x1f
            decideBiome(cx * 32 + x, cz * 32 + z)
        }
    }

    fun decideBiome(x: Int, z: Int): Biome {
        val worldHeight = getHeightAtInternal(x, z)

        var biome: Biome = biomes["grassland"]!!

        //TODO more complex logic than this !
        if (worldHeight < waterHeight + 3) {
            biome = biomes["beach"] ?: biome
        }

        return biome
    }

    internal class StructureToPaste(var structure: Structure, var position: Vector3i, var flags: Int)

    override fun generateWorldSlice(chunks: Array<Chunk>) {
        val cx = chunks[0].chunkX
        val cz = chunks[0].chunkZ

        val sliceData = SliceData(cx, cz)

        // Generate the heights in advance!
        for (x in 0..31) {
            for (z in 0..31) {
                sliceData.heights[x * 32 + z] = getHeightAtInternal(cx * 32 + x, cz * 32 + z)
            }
        }

        caveBuilder.generateCaves(cx, cz, sliceData)

        for (chunkY in chunks.indices) {
            val chunk = chunks[chunkY]
            val cy = chunk.chunkY
            for (x in 0..31) {
                for (z in 0..31) {
                    val groundHeight = sliceData.heights[x * 32 + z]
                    var y = cy * 32
                    while (y < cy * 32 + 32 && y <= groundHeight) {
                        chunk.pokeSimpleSilently(x, y, z, stoneVoxel, -1, -1, 0)
                        y++
                    }
                }
            }
            sliceData.structures.forEach {
                it.structure.paste(chunk, it.position, it.flags)
            }
        }

        sliceData.structures.clear()

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
                            if(chunks.getOrNull((y + dy) / 32)?.peekSimple(x + dx, y + dy, z + dz) == stoneVoxel)
                                chunks.getOrNull((y + dy) / 32)?.pokeSimple(x + dx, y + dy, z + dz, oreType.voxel, 0, 0, 0)
                        }
                    }
                }
            }
        }

        val maxheight = chunks.size * 32 - 1
        for (x in 0..31) {
            for (z in 0..31) {
                val biome = sliceData.biome[x * 32 + z]

                var y = maxheight
                val worldY = sliceData.heights[x * 32 + z]

                while (y > 0 && chunks[y / 32].peekSimple(x, y, z) === airVoxel) {
                    y--
                }
                val groundHeightActual = y

                // It's flooded!
                if (groundHeightActual < waterHeight && worldY < waterHeight) {
                    var waterY = waterHeight
                    while (waterY > 0 && chunks[waterY / 32].peekSimple(x, waterY, z) === airVoxel) {
                        chunks[waterY / 32].pokeSimpleSilently(x, waterY, z, waterVoxel, -1, -1, 0)
                        waterY--
                    }

                    // Replace the stone with whatever is the ground block
                    var undergroundDirt = groundHeightActual
                    while (undergroundDirt >= 0 && undergroundDirt >= groundHeightActual - 3 && chunks[undergroundDirt / 32].peekSimple(x, undergroundDirt, z) === stoneVoxel) {
                        chunks[undergroundDirt / 32].pokeSimpleSilently(x, undergroundDirt, z, biome.underwaterGround, -1, -1, 0)
                        undergroundDirt--
                    }
                } else {
                    // Top soil
                    chunks[groundHeightActual / 32].pokeSimpleSilently(x, groundHeightActual, z, biome.surfaceBlock, -1, -1, 0)

                    // Replace the stone with whatever is the ground block
                    var undergroundDirt = groundHeightActual - 1
                    while (undergroundDirt >= 0 && undergroundDirt >= groundHeightActual - 3 && chunks[undergroundDirt / 32].peekSimple(x, undergroundDirt, z) === stoneVoxel) {
                        chunks[undergroundDirt / 32].pokeSimpleSilently(x, undergroundDirt, z, biome.groundBlock, -1, -1, 0)
                        undergroundDirt--
                    }

                    // Decoration shrubs flowers etc
                    val surface = groundHeightActual + 1
                    if (surface > maxheight)
                        continue

                    val bushChance = rng.nextDouble()
                    if (bushChance < biome.surfaceDecorationsDensity) {
                        chunks[surface / 32].pokeSimpleSilently(x, surface, z, biome.surfaceDecorations.random(rng), -1, -1, 0)
                    }
                }
            }
        }

        addTrees(cx, cz, sliceData)

        for (chunkY in chunks.indices) {
            sliceData.structures.forEach { stp -> stp.structure.paste(chunks[chunkY], stp.position, stp.flags) }
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

                    val biome = decideBiome(x, z)

                    if (rnd.nextDouble() < biome.treesDensity) {
                        val y = getHeightAtInternal(x, z) - 1
                        if (y > waterHeight) {
                            val treeStructure = biome.treesVariants.random(rnd)
                            data.structures.add(StructureToPaste(treeStructure, Vector3i(x, y, z), Structure.FLAG_USE_OFFSET or Structure.FLAG_DONT_OVERWRITE_AIR))
                        }
                    }
                }
            }
    }

    private fun fractalNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float): Float {
        var frequency = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        frequency *= (worldSizeInBlocks / (64 * 32)).toFloat()
        for (i in 0 until octaves) {
            total += ssng.looped_noise(x * frequency, z * frequency, worldSizeInBlocks.toFloat()) * amplitude
            frequency *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    private fun ridgedNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float): Float {
        var frequency = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        frequency *= (worldSizeInBlocks / (64 * 32)).toFloat()
        for (i in 0 until octaves) {
            total += (1.0f - abs(ssng.looped_noise(x * frequency, z * frequency, worldSizeInBlocks.toFloat()))) * amplitude
            frequency *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    open fun getHeightAtInternal(x: Int, z: Int): Int {
        var height = 0.0

        val baseHeight = ridgedNoise(x, z, 2,  0.5f, 0.5f)
        height += baseHeight * 128
        //val baseHeight = ridgedNoise(x, z, 5, 1.0f, 0.5f)
        //height += baseHeight * baseHeightScale

        /*var mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f)
        mountainFactor *= (1.0 + 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f)).toFloat()
        mountainFactor -= mountainOffset.toFloat()
        mountainFactor /= (1 - mountainOffset).toFloat()
        mountainFactor = Math2.clamp(mountainFactor.toDouble(), 0.0, 1.0)

        height += mountainFactor * mountainScale

        var plateauHeight = Math2.clamp((fractalNoise(x + 225, z + 321, 3, 1f, 0.5f) * 32.0f - 8.0f).toDouble(), 0.0, 1.0)
        plateauHeight *= Math2.clamp((fractalNoise(x + 3158, z + 9711, 3, 0.125f, 0.5f) * 0.5f + 0.5f).toDouble(), 0.0, 1.0)

        if (height >= waterHeight)
            height += plateauHeight * plateauHeightScale
        else
            height += plateauHeight * baseHeight * plateauHeightScale.toFloat()*/

        return height.toInt()
    }

    /*private fun getForestness(x: Int, z: Int): Float {
        var mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f)
        mountainFactor *= (1.0 + 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f)).toFloat()

        mountainFactor -= 0.3f
        mountainFactor *= 2.0f

        mountainFactor = Math2.clamp(mountainFactor.toDouble(), 0.0, 2.0)

        var f = 0.1f + Math2.clamp(4.0 * fractalNoise(x + 1397, z + 321, 3, 0.5f, 0.5f), 0.0, 1.0)

        f -= (mountainFactor * 0.45).toFloat()

        return Math2.clamp(f.toDouble(), 0.0, 1.0)
    }*/
}

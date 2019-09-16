//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import xyz.chunkstories.api.converter.MinecraftBlocksTranslator
import xyz.chunkstories.api.math.Math2
import xyz.chunkstories.api.math.random.SeededSimplexNoiseGenerator
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.structures.McSchematicStructure
import xyz.chunkstories.api.voxel.structures.Structure
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import org.joml.Vector3i
import xyz.chunkstories.api.content.json.asDouble
import xyz.chunkstories.api.content.json.asInt

import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Random

open class HorizonGenerator(definition: WorldGeneratorDefinition, world: World) : WorldGenerator(definition, world) {
	internal var ssng: SeededSimplexNoiseGenerator

	internal var worldSizeInBlocks: Int = 0

	protected var WATER_HEIGHT: Int = 0
	protected var MOUNTAIN_SCALE: Int = 0
	protected var BASE_HEIGHT_SCALE: Int = 0
	protected var PLATEAU_HEIGHT_SCALE: Int = 0
	protected var MOUNTAIN_OFFSET: Double = 0.toDouble()

	private val AIR_VOXEL: Voxel
	private val STONE_VOXEL: Voxel?
	private val UNDERGROUND_VOXEL: Voxel?
	private val WATER_VOXEL: Voxel?

	private val GROUND_VOXEL: Voxel?
	private val FOREST_GROUND_VOXEL: Voxel?
	//private val DRY_GROUND_VOXEL: Voxel?

	private val SAND: Voxel?
	private val GRAVEL: Voxel?

	private val TALLGRASS: Voxel?
	private val SURFACE_DECORATIONS: Array<Voxel>

	internal val OKA_TREE_VARIANTS = 6
	internal val oakTrees = mutableListOf<Structure>()

	internal val FALLEN_TREE_VARIANTS = 2
	internal val fallenTrees = mutableListOf<Structure>()

	internal val REDWOOD_TREE_VARIANTS = 3
	internal val redwoodTrees = mutableListOf<Structure>()

	internal var treeTypes = arrayOf<List<Structure>>(oakTrees, fallenTrees, redwoodTrees)

	internal var caveBuilder: CaveBuilder

	init {
		ssng = SeededSimplexNoiseGenerator(world.worldInfo.seed)
		worldSizeInBlocks = world.sizeInChunks * 32

		caveBuilder = CaveBuilder(world, this)

		this.WATER_HEIGHT = definition["waterHeight"].asInt ?: 48
		this.MOUNTAIN_OFFSET = definition["mountainOffset"].asDouble ?: 0.3
		this.MOUNTAIN_SCALE = definition["mountainScale"].asInt ?: 128
		this.BASE_HEIGHT_SCALE = definition["baseHeightScale"].asInt ?: 64
		this.PLATEAU_HEIGHT_SCALE = definition["plateauHeightScale"].asInt ?: 64

		this.AIR_VOXEL = world.gameContext.content.voxels.air
		this.STONE_VOXEL = world.gameContext.content.voxels.getVoxel("stone")
		this.WATER_VOXEL = world.gameContext.content.voxels.getVoxel("water")
		this.GROUND_VOXEL = world.gameContext.content.voxels.getVoxel("grass")
		this.FOREST_GROUND_VOXEL = world.gameContext.content.voxels.getVoxel("forestgrass")
		//this.DRY_GROUND_VOXEL = world.gameContext.content.voxels.getVoxel("drygrass")
		this.UNDERGROUND_VOXEL = world.gameContext.content.voxels.getVoxel("dirt")
		this.TALLGRASS = world.gameContext.content.voxels.getVoxel("grass_prop")

		this.SAND = world.gameContext.content.voxels.getVoxel("sand")
		this.GRAVEL = world.gameContext.content.voxels.getVoxel("gravel")

		try {
			val translator = MinecraftBlocksTranslator(world.gameContext, world.content.getAsset("converter_mapping.txt"))

			for (i in 1..OKA_TREE_VARIANTS)
				oakTrees.add (McSchematicStructure
						.fromAsset(world.content.getAsset("./structures/oak_tree$i.schematic"), translator)!!)

			for (i in 1..FALLEN_TREE_VARIANTS)
				fallenTrees.add (McSchematicStructure.fromAsset(
						world.content.getAsset("./structures/oak_fallen$i.schematic"), translator)!!)

			for (i in 1..REDWOOD_TREE_VARIANTS)
				redwoodTrees.add (McSchematicStructure.fromAsset(
						world.content.getAsset("./structures/redwood_tree$i.schematic"), translator)!!)

			val decorations = arrayOf("flower_yellow", "flower_red", "flower_orange", "flower_blue", "flower_purple", "flower_white", "mushroom_red", "mushroom_brown")
			val surfaceDecorations = mutableListOf<Voxel>()
			for (i in 1..REDWOOD_TREE_VARIANTS)
				surfaceDecorations.add( world.gameContext.content.voxels.getVoxel(decorations[i])!! )
			SURFACE_DECORATIONS = surfaceDecorations.toTypedArray()

		} catch (e: IOException) {
			e.printStackTrace()
			throw e
		}
	}

	internal class SliceData {
		var heights = IntArray(1024)

		var forestness = FloatArray(1024)
		var dryness = FloatArray(1024)

		var structures: MutableList<StructureToPaste> = ArrayList()
	}

	internal class StructureToPaste(var structure: Structure, var position: Vector3i, var flags: Int)

	override fun generateWorldSlice(chunks: Array<Chunk>) {

		val cx = chunks[0].chunkX
		val cz = chunks[0].chunkZ

		val rnd = Random()
		val sliceData = SliceData()

		// Generate the heights in advance!
		for (x in 0..31)
			for (z in 0..31) {
				sliceData.heights[x * 32 + z] = getHeightAtInternal(cx * 32 + x, cz * 32 + z)
				sliceData.forestness[x * 32 + z] = getForestness(cx * 32 + x, cz * 32 + z)
			}

		caveBuilder.generateCaves(cx, cz, rnd, sliceData)

		for (chunkY in chunks.indices) {
			generateChunk(chunks[chunkY], rnd, sliceData)
		}

		val maxheight = chunks.size * 32 - 1
		for (x in 0..31)
			for (z in 0..31) {
				var y = maxheight
				val worldY = sliceData.heights[x * 32 + z]

				while (y > 0 && chunks[y / 32].peekSimple(x, y, z) === AIR_VOXEL) {
					y--
				}
				val groundHeightActual = y

				// It's flooded!
				if (groundHeightActual < WATER_HEIGHT && worldY < WATER_HEIGHT) {
					var waterY = WATER_HEIGHT
					while (waterY > 0 && chunks[waterY / 32].peekSimple(x, waterY, z) === AIR_VOXEL) {
						chunks[waterY / 32].pokeSimpleSilently(x, waterY, z, WATER_VOXEL, -1, -1, 0)
						waterY--
					}
				} else {
					// Top soil
					var topVoxel: Voxel?
					val forestIntensity = sliceData.forestness[x * 32 + z]

					if (Math.random() < (forestIntensity - 0.5) * 1.5f && forestIntensity > 0.5)
						topVoxel = FOREST_GROUND_VOXEL // ground
					else
						topVoxel = GROUND_VOXEL

					var groundVoxel = UNDERGROUND_VOXEL

					// if we're close to water level make the ground sand
					if (worldY < WATER_HEIGHT + 3) {
						topVoxel = SAND
						groundVoxel = SAND
					}

					chunks[groundHeightActual / 32].pokeSimpleSilently(x, groundHeightActual, z, topVoxel, -1, -1, 0)
					// 3 blocks of dirt underneath it
					var undergroundDirt = groundHeightActual - 1
					while (undergroundDirt >= 0 && undergroundDirt >= groundHeightActual - 3
							&& chunks[undergroundDirt / 32].peekSimple(x, undergroundDirt, z) === STONE_VOXEL) {
						chunks[undergroundDirt / 32].pokeSimpleSilently(x, undergroundDirt, z, groundVoxel, -1, -1, 0)
						undergroundDirt--
					}

					// Decoration shrubs flowers etc
					val surface = groundHeightActual + 1
					if (surface > maxheight || topVoxel === SAND)
						continue

					val bushChance = Math.random()
					if (topVoxel !== FOREST_GROUND_VOXEL || Math.random() > 0.8) {
						if (bushChance > 0.5) {
							if (bushChance > 0.95) {
								val surfaceVoxel = SURFACE_DECORATIONS[rnd.nextInt(SURFACE_DECORATIONS.size)]
								chunks[surface / 32].pokeSimpleSilently(x, surface, z, surfaceVoxel, -1, -1, 0)
							} else
								chunks[surface / 32].pokeSimpleSilently(x, surface, z, TALLGRASS, -1, -1, 0)
						}
					}
				}
			}

		sliceData.structures.clear()
		addTrees(cx, cz, rnd, sliceData)

		for (chunkY in chunks.indices) {
			sliceData.structures.forEach { stp -> stp.structure.paste(chunks[chunkY], stp.position, stp.flags) }
		}
	}

	private fun generateChunk(chunk: Chunk, rnd: Random, sliceData: SliceData) {

		val cy = chunk.chunkY

		if (chunk.chunkY * 32 >= 384)
			return

		var voxel: Voxel? = null
		for (x in 0..31)
			for (z in 0..31) {
				val groundHeight = sliceData.heights[x * 32 + z]/* getHeightAtInternal(cx * 32 + x, cz * 32 + z) */

				var groundVoxel = UNDERGROUND_VOXEL

				// if we're close to water level make the ground sand
				if (groundHeight > WATER_HEIGHT - 9 && groundHeight < WATER_HEIGHT) {
					groundVoxel = GRAVEL
				}
				if (groundHeight > WATER_HEIGHT - 3 && groundHeight < WATER_HEIGHT) {
					groundVoxel = SAND
				}

				var y = cy * 32
				while (y < cy * 32 + 32 && y <= groundHeight) {
					if (groundHeight - y > 3)
					// more than 3 blocks underground => deep ground
						voxel = STONE_VOXEL
					else
						voxel = groundVoxel

					chunk.pokeSimpleSilently(x, y, z, voxel, -1, -1, 0)
					y++
				}
			}

		for (pm in sliceData.structures) {
			pm.structure.paste(chunk, pm.position, pm.flags)
		}
	}

	private fun addTrees(cx: Int, cz: Int, rnd: Random, data: SliceData) {
		// take into account the nearby chunks
		for (gcx in cx - 1..cx + 1)
			for (gcz in cz - 1..cz + 1) {
				rnd.setSeed((gcx * 32 + gcz + 48716148).toLong())

				// how many trees are there supposed to be in that chunk
				// float treenoise = 0.5f + fractalNoise(gcx * 32 + 2, gcz * 32 + 32, 3, 0.25f,
				// 0.85f);

				val ntrees = 25

				for (i in 0 until ntrees) {
					val x = gcx * 32 + rnd.nextInt(32)
					val z = gcz * 32 + rnd.nextInt(32)

					val forestness = getForestness(x, z)
					if (rnd.nextFloat() < forestness) {

						val y = getHeightAtInternal(x, z) - 1
						if (y > WATER_HEIGHT) {

							val type = rnd.nextInt(treeTypes.size)

							val treeType = treeTypes[type]

							val variant = rnd.nextInt(treeType.size)
							data.structures.add(StructureToPaste(treeType[variant], Vector3i(x, y, z),
									Structure.FLAG_USE_OFFSET or Structure.FLAG_DONT_OVERWRITE_AIR))
							// treeType[variant].paste(chunk, new Vector3i(x, y, z),
							// Structure.FLAG_USE_OFFSET | Structure.FLAG_DONT_OVERWRITE_AIR);
						}

					}
				}
			}
	}

	internal fun fractalNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float): Float {
		var freq = freq
		var total = 0.0f
		var maxAmplitude = 0.0f
		var amplitude = 1.0f
		freq *= (worldSizeInBlocks / (64 * 32)).toFloat()
		for (i in 0 until octaves) {
			total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks.toFloat()) * amplitude
			freq *= 2.0f
			maxAmplitude += amplitude
			amplitude *= persistence
		}
		return total / maxAmplitude
	}

	internal fun ridgedNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float): Float {
		var freq = freq
		var total = 0.0f
		var maxAmplitude = 0.0f
		var amplitude = 1.0f
		freq *= (worldSizeInBlocks / (64 * 32)).toFloat()
		for (i in 0 until octaves) {
			total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks.toFloat()))) * amplitude
			freq *= 2.0f
			maxAmplitude += amplitude
			amplitude *= persistence
		}
		return total / maxAmplitude
	}

	open fun getHeightAtInternal(x: Int, z: Int): Int {
		var height = 0.0f

		val baseHeight = ridgedNoise(x, z, 5, 1.0f, 0.5f)

		height += baseHeight * BASE_HEIGHT_SCALE

		var mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f)
		mountainFactor *= (1.0 + 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f)).toFloat()
		mountainFactor -= MOUNTAIN_OFFSET.toFloat()
		mountainFactor /= (1 - MOUNTAIN_OFFSET).toFloat()
		mountainFactor = Math2.clamp(mountainFactor.toDouble(), 0.0, 1.0)

		height += mountainFactor * MOUNTAIN_SCALE

		var plateauHeight = Math2.clamp((fractalNoise(x + 225, z + 321, 3, 1f, 0.5f) * 32.0f - 8.0f).toDouble(), 0.0, 1.0)
		plateauHeight *= Math2.clamp((fractalNoise(x + 3158, z + 9711, 3, 0.125f, 0.5f) * 0.5f + 0.5f).toDouble(), 0.0, 1.0)

		if (height >= WATER_HEIGHT)
			height += plateauHeight * PLATEAU_HEIGHT_SCALE
		else
			height += plateauHeight * baseHeight * PLATEAU_HEIGHT_SCALE.toFloat()

		// height = 64 + plateauHeight * PLATEAU_HEIGHT_SCALE;

		return height.toInt()
	}

	private fun getForestness(x: Int, z: Int): Float {

		var mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f)
		mountainFactor *= (1.0 + 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f)).toFloat()

		mountainFactor -= 0.3f
		mountainFactor *= 2.0f

		mountainFactor = Math2.clamp(mountainFactor.toDouble(), 0.0, 2.0)

		var f = 0.1f + Math2.clamp(4.0 * fractalNoise(x + 1397, z + 321, 3, 0.5f, 0.5f), 0.0, 1.0)

		f -= (mountainFactor * 0.45).toFloat()

		return Math2.clamp(f.toDouble(), 0.0, 1.0)
	}
}

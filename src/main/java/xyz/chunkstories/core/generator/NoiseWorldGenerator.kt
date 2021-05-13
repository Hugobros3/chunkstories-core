//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import xyz.chunkstories.api.math.random.SeededSimplexNoiseGenerator
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.chunk.Chunk
import xyz.chunkstories.api.world.generator.WorldGenerator
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition
import org.joml.Vector3f
import xyz.chunkstories.api.block.BlockType

import java.util.Random

class NoiseWorldGenerator(type: WorldGeneratorDefinition, world: World) : WorldGenerator(type, world) {
    private val ssng: SeededSimplexNoiseGenerator

    private val ws: Int
    private val STONE_VOXEL: BlockType
    private val WATER_VOXEL: BlockType

    init {
        ssng = SeededSimplexNoiseGenerator(world.properties.seed)
        ws = world.properties.size.sizeInChunks * 32

        this.STONE_VOXEL = world.gameInstance.content.blockTypes["stone"]!!
        this.WATER_VOXEL = world.gameInstance.content.blockTypes["water"]!!
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
        // rnd.setSeed(cx * 32 + cz + 48716148);

        // CubicChunk chunk = new CubicChunk(region, cx, cy, cz);
        val position = Vector3f()

        val wx = 8
        val wy = 4
        val wz = 8

        // Real width of array, we need a x+1 array so we can properly interpolate at
        // the far end
        val wwx = wx + 1
        val wwy = wy + 1
        val wwz = wz + 1
        val generated = FloatArray(wwx * wwy * wwz)
        for (a in 0 until wwx)
            for (b in 0 until wwy)
                for (c in 0 until wwz) {
                    val x = a * (32 / wx)
                    val y = b * (32 / wy)
                    val z = c * (32 / wz)

                    position.x = (cx * 32 + x).toFloat()
                    position.y = (cy * 32 + y).toFloat()
                    position.z = (cz * 32 + z).toFloat()

                    position.mul(0.05f)
                    generated[wwx * (wwy * c + b) + a] = ssng.noise(position.x(), position.y(), position.z())
                }

        for (x in 0..31)
            for (z in 0..31) {
                var finalHeight = 128.0f
                finalHeight += ridgedNoise(cx * 32 + x, cz * 32 + z, 5, 1.0f, 0.5f) * 128 // * (64 + 128 *
                // mountainFactor));
                for (y in 0..31) {
                    // Assertion : a, b and c are not the last element in their dimension in the
                    // array
                    // Put simply : you can access a+1, b+1 and c+1 even all of them at once,
                    // without issues.
                    val a = Math.floor((x / (32 / wx)).toDouble()).toInt()
                    val b = Math.floor((y / (32 / wy)).toDouble()).toInt()
                    val c = Math.floor((z / (32 / wz)).toDouble()).toInt()

                    // Unlerped value, for debug purposes
                    var value = generated[wwx * (wwy * c + b) + a]

                    // Lerped on X axis, 4 values
                    val lerpedX_y0z0 = lerp(x, generated[wwx * (wwy * c + b) + a], generated[wwx * (wwy * c + b) + a + 1], a, a + 1, 32 / wx)
                    val lerpedX_y1z0 = lerp(x, generated[wwx * (wwy * c + b + 1) + a], generated[wwx * (wwy * c + b + 1) + a + 1], a, a + 1, 32 / wx)

                    val lerpedX_y0z1 = lerp(x, generated[wwx * (wwy * (c + 1) + b) + a], generated[wwx * (wwy * (c + 1) + b) + a + 1], a, a + 1, 32 / wx)
                    val lerpedX_y1z1 = lerp(x, generated[wwx * (wwy * (c + 1) + b + 1) + a], generated[wwx * (wwy * (c + 1) + b + 1) + a + 1], a, a + 1,
                            32 / wx)
                    // Lerp that about the Y axis
                    val lerpedXY_z0 = lerp(y, lerpedX_y0z0, lerpedX_y1z0, b, b + 1, 32 / wy)
                    val lerpedXY_z1 = lerp(y, lerpedX_y0z1, lerpedX_y1z1, b, b + 1, 32 / wy)
                    // Lerp moar
                    val lerpedXYZ = lerp(z, lerpedXY_z0, lerpedXY_z1, c, c + 1, 32 / wz)

                    value = lerpedXYZ

                    // Apply gradient so values decrease near ground and thus create air
                    var gradient = clamp((finalHeight - 32f - (cy * 32 + y).toFloat()) / 64f, 0.0f, 1.0f)
                    gradient += clamp((finalHeight - (cy * 32 + y)) / 128f, 0.0f, 0.35f) + 0.00f

                    val noiseMult = clamp((finalHeight + 64 - (cy * 32 + y)) / 32f, 0.0f, 1.0f)

                    // gradient = gradient;
                    value = gradient - clamp(value * noiseMult, -0.15f, 0.5f) * 0.15f

                    // Blocks writing
                    if (value > 0.0f)
                        chunk.pokeSimpleSilently(x, y, z, STONE_VOXEL, -1, -1, 0)
                    else if (cy * 32 + y < 256)
                        chunk.pokeSimpleSilently(x, y, z, WATER_VOXEL, -1, -1, 0)// Water
                }
            }
    }

    internal fun lerp(x: Int, val0: Float, val1: Float, i0: Int, i1: Int, granularity: Int): Float {
        return if (x % granularity == 0) val0 else (val1 * (x - i0 * granularity) + val0 * (i1 * granularity - x)) / granularity

    }

    internal fun clamp(value: Float, lower: Float, upper: Float): Float {
        if (value < lower)
            return lower
        return if (value > upper) upper else value
    }

    internal fun fractalNoise(x: Int, z: Int, octaves: Int, freq: Float, persistence: Float): Float {
        var freq = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        freq *= (ws / (64 * 32)).toFloat()
        for (i in 0 until octaves) {
            total += ssng.looped_noise(x * freq, z * freq, ws.toFloat()) * amplitude
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
        freq *= (ws / (64 * 32)).toFloat()
        for (i in 0 until octaves) {
            total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, ws.toFloat()))) * amplitude
            freq *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }
}

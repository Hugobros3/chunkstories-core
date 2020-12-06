//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import xyz.chunkstories.api.math.MathUtils
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.generator.HorizonGenerator.SliceData
import xyz.chunkstories.core.generator.HorizonGenerator.StructureToPaste
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3i
import xyz.chunkstories.api.math.MathUtils.mixd
import xyz.chunkstories.api.math.MathUtils.mixf
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.structures.Structure
import xyz.chunkstories.api.world.chunk.Chunk

import java.util.Random

internal class Caves(private val world: World, private val generator: HorizonGenerator) {
    fun generateCaves(cx: Int, cz: Int, sliceData: SliceData) {
        val rx = cx shr 3
        val rz = cz shr 3

        val sir = world.worldInfo.size.sizeInChunks shr 3

        val rnd = Random()

        for (irx in rx - 2..rx + 2)
            for (irz in rz - 2..rz + 2) {
                val arx = irx % sir
                val arz = irz % sir

                val seed = (arx * sir + arz) % 44873 + arx * 1848 + arz * 874
                rnd.setSeed(seed.toLong())

                val maxNumberOfSnakesPerRegion = rnd.nextInt(8)
                for (i in 0 until maxNumberOfSnakesPerRegion) {
                    val pos = Vector3i(arx * 256 + rnd.nextInt(256), 0, arz * 256 + rnd.nextInt(256))
                    val groundHeight = generator.getHeightAtInternal(pos.x, pos.z)
                    if (groundHeight <= 32)
                        continue

                    pos.y = 32 + rnd.nextInt(groundHeight)

                    val length = rnd.nextInt(64)
                    propagateSnake(cx, cz, pos, length, 0, sliceData)
                }
            }
    }

    private fun propagateSnake(cx: Int, cz: Int, pos: Vector3i, length: Int, forkdepth: Int, sliceData: SliceData) {
        if (length <= 0)
            return

        val rnd = Random()
        rnd.setSeed((pos.x * 1548 + pos.y * 487 + pos.z).toLong())

        val direction = Vector3d(rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0)
        direction.normalize()

        val step = 8

        var nextFork = 2 + rnd.nextInt(1 + (length / 1.5).toInt())
        if (forkdepth > 3)
            nextFork = -1

        // center of the focus chunk
        val ccx = cx * 32 + 16
        val ccz = cz * 32 + 16
        val chunkpos = Vector2i(ccx, ccz)

        val minimumSize = 1.2
        val maximumSize = 5.5

        var size = rnd.nextDouble() * (maximumSize - minimumSize) + minimumSize

        for (j in 0 until length) {
            val oldPosition = Vector3i(pos)

            val delta = Vector3i()
            delta.x = (direction.x * step).toInt()
            delta.y = (direction.y * step).toInt()
            delta.z = (direction.z * step).toInt()
            pos.add(delta)

            val oldsize = size
            size += rnd.nextDouble() * 0.25 - 0.5
            size = MathUtils.clampd(size, minimumSize, maximumSize)

            val newdirection = Vector3d(rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0)
            newdirection.normalize()

            val newdirectioninfluence = rnd.nextDouble()

            direction.x = mixd(direction.x, direction.x + newdirection.x, newdirectioninfluence)
            direction.y = mixd(direction.y, direction.y + newdirection.y, newdirectioninfluence)
            direction.z = mixd(direction.z, direction.z + newdirection.z, newdirectioninfluence)
            direction.normalize()

            val posDelta = Vector2i(pos.x, pos.z)
            posDelta.sub(chunkpos)

            if (posDelta.length() < 64) {
                // Touching terrain we want to generate
                sliceData.structures.add(StructureToPaste(CaveSnakeSegment.createSegment(pos, oldPosition, size, oldsize), pos, 0))
            }

            if (nextFork > 0)
                nextFork--
            if (nextFork == 0) {
                propagateSnake(cx, cz, pos, length - rnd.nextInt(j), forkdepth + 1, sliceData)
                nextFork = 2 + rnd.nextInt(1 + (length / 1.5).toInt())
            }
        }
    }
}

internal class CaveSnakeSegment private constructor(internal val from: Vector3i, internal val to: Vector3i, private val startingSize: Double, private val endSize: Double, width: Int, height: Int, length: Int) : Structure(width, height, length) {
    override fun paste(chunk: Chunk, position: Vector3i, flags: Int) {
        val whereTho = Vector3i()
        val d = from.distance(to)
        var i = 0
        while (i <= d) {
            whereTho.x = mixd(from.x.toDouble(), to.x.toDouble(), i / d).toInt()
            whereTho.y = mixd(from.y.toDouble(), to.y.toDouble(), i / d).toInt()
            whereTho.z = mixd(from.z.toDouble(), to.z.toDouble(), i / d).toInt()
            // System.out.println("pasting cave @"+from+" to "+to +"prog="+i / d);
            sphereOfDoom(chunk, whereTho, mixd(startingSize, endSize, i / d))
            i++
        }
        sphereOfDoom(chunk, to, endSize)
        // cubeOfDoom(chunk, from, 3);
        // cubeOfDoom(chunk, to, 3);
    }

    private fun sphereOfDoom(chunk: Chunk, position: Vector3i, radius: Double) {
        val size = Math.ceil(radius).toInt()
        val air = chunk.world.content.voxels.air
        for (x in position.x - size..position.x + size)
            for (y in position.y - size..position.y + size)
                for (z in position.z - size..position.z + size) {
                    if (inChunk(x, y, z, chunk)) {
                        val rx = position.x - x
                        val ry = position.y - y
                        val rz = position.z - z

                        if (rx * rx + ry * ry + rz * rz <= size * size) {
                            var voxel: Voxel? = air
                            if (y == 0)
                                voxel = chunk.world.content.voxels.getVoxel("stone")
                            else if (y < 10)
                                voxel = chunk.world.content.voxels.getVoxel("lava")

                            chunk.pokeSimpleSilently(x, y, z, voxel, 0, 0, 0)
                        }
                    }
                }
    }

    private fun inChunk(x: Int, y: Int, z: Int, chunk: Chunk): Boolean {
        return (x >= chunk.chunkX * 32 && x < chunk.chunkX * 32 + 32 && y >= chunk.chunkY * 32 && y < chunk.chunkY * 32 + 32
                && z >= chunk.chunkZ * 32 && z < chunk.chunkZ * 32 + 32)
    }

    companion object {
        fun createSegment(fromP: Vector3i, toP: Vector3i, fromS: Double, toS: Double): CaveSnakeSegment {
            val from = Vector3i(fromP)
            val to = Vector3i(toP)

            val size = Vector3i()
            size.x = 6 + Math.abs(from.x - to.x)
            size.y = 6 + Math.abs(from.y - to.y)
            size.z = 6 + Math.abs(from.z - to.z)

            return CaveSnakeSegment(from, to, fromS, toS, size.x, size.y, size.z)
        }
    }
}

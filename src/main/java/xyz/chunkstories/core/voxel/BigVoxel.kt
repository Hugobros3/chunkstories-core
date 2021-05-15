//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.MutableChunkCell

// don't trust the lies of BIG VOXEL !!!!
class BigVoxel(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {

	val xWidth: Int
	val yWidth: Int
	val zWidth: Int

	val xBits: Int
	val yBits: Int
	val zBits: Int
	val xMask: Int
	val yMask: Int
	val zMask: Int
	val xShift: Int
	val yShift: Int
	val zShift: Int

	init {

		this.xWidth = definition["xWidth"].asInt ?: 1//Integer.parseInt(type.resolveProperty("xWidth", "1"));
		this.yWidth = definition["yWidth"].asInt ?: 1//Integer.parseInt(type.resolveProperty("yWidth", "1"));
		this.zWidth = definition["zWidth"].asInt ?: 1//Integer.parseInt(type.resolveProperty("zWidth", "1"));

		xBits = Math.ceil(Math.log(xWidth.toDouble()) / Math.log(2.0)).toInt()
		yBits = Math.ceil(Math.log(yWidth.toDouble()) / Math.log(2.0)).toInt()
		zBits = Math.ceil(Math.log(zWidth.toDouble()) / Math.log(2.0)).toInt()

		xMask = Math.pow(2.0, xBits.toDouble()).toInt() - 1
		yMask = Math.pow(2.0, yBits.toDouble()).toInt() - 1
		zMask = Math.pow(2.0, zBits.toDouble()).toInt() - 1

		xShift = 0
		yShift = xBits
		zShift = yShift + yBits

		if (xBits + yBits + zBits > 8) {
			throw RuntimeException(
					"Metadata requirements can't allow you to have more than a total of 8 bits to describe the length of those")
		}
	}

	// TODO move to item
	/*fun onPlace(context: FutureCell, cause: WorldModificationCause?) {
		// Be cool with the system doing it's thing
		if (cause == null)
			return

		val x = context.x
		val y = context.y
		val z = context.z

		// Check if there is space for it ...
		for (a in x until x + xWidth) {
			for (b in y until y + yWidth) {
				for (c in z until z + zWidth) {
					val chunk = context.world.chunksManager.getChunkWorldCoordinates(a, b, c) ?: throw IllegalBlockModificationException(context,
							"All chunks upon wich this block places itself must be fully loaded !")

					val stuff = context.world.peek(a, b, c)
					if (stuff.voxel == null || stuff.voxel.isAir()
							|| !stuff.voxel.solid) {
						// These blocks are replaceable
						continue
					} else
						throw IllegalBlockModificationException(context,
								"Can't overwrite block at $a: $b: $c")
				}
			}
		}

		// Actually build the thing then
		for (a in 0 until 0 + xWidth) {
			for (b in 0 until 0 + yWidth) {
				for (c in 0 until 0 + zWidth) {
					val metadata = (a and xMask shl xShift or (b and yMask shl yShift) or (c and zMask shl zShift)).toByte().toInt()

					context.world.pokeSimple(a + x, b + y, c + z, this, -1, -1, metadata)
				}
			}
		}
	}*/

	override fun onRemove(cell: MutableChunkCell): Boolean {
		val x = cell.x
		val y = cell.y
		val z = cell.z

		// Backpedal to find the root block
		val meta = cell.data.extraData

		val ap = meta shr xShift and xMask
		val bp = meta shr yShift and yMask
		val cp = meta shr zShift and zMask

		println("Removing $ap: $bp: $cp")

		val startX = x - ap
		val startY = y - bp
		val startZ = z - cp

		val empty = CellData(cell.world.gameInstance.content.blockTypes.air)

		for (a in startX until startX + xWidth) {
			for (b in startY until startY + yWidth) {
				for (c in startZ until startZ + zWidth) {
					// poke zero where the big voxel used to be
					cell.world.setCellData(a, b, c, empty)
				}
			}
		}

		return true
	}

	companion object {

		/**
		* Test out the auto-partitionning logic for the 8 bits of metadata
		*/
		@JvmStatic
		fun main(args: Array<String>) {

			val xWidth = 16
			val height = 4
			val zWidth = 4

			val xBits = Math.ceil(Math.log(xWidth.toDouble()) / Math.log(2.0)).toInt()
			val yBits = Math.ceil(Math.log(height.toDouble()) / Math.log(2.0)).toInt()
			val zBits = Math.ceil(Math.log(zWidth.toDouble()) / Math.log(2.0)).toInt()

			println("$xBits : $yBits  $zBits")

			val xMask = Math.pow(2.0, xBits.toDouble()).toInt() - 1
			val yMask = Math.pow(2.0, yBits.toDouble()).toInt() - 1
			val zMask = Math.pow(2.0, zBits.toDouble()).toInt() - 1

			println("$xMask : $yMask  $zMask")

			val xShift = 0
			val zShift = xBits + yBits

			val xMask_shifted = xMask shl xShift
			val yMask_shifted = yMask shl xBits
			val zMask_shifted = zMask shl zShift

			println("$xMask_shifted : $yMask_shifted  $zMask_shifted")

			for (a in 0 until xWidth) {
				for (b in 0 until height) {
					for (c in 0 until zWidth) {
						val test = (a and xMask shl xShift or (b and yMask shl xBits) or (c and zMask shl zShift))

						val ap = (test shr xShift) and xMask
						val bp = (test shr xBits) and yMask
						val cp = (test shr zShift) and zMask

						if (a == ap && b == bp && c == cp) {
							// System.out.println("All is good with the world rn");
						} else {
							println("test: $test")
							println("a: $a ap: $ap")
							println("b: $b bp: $bp")
							println("c: $c cp: $cp")
						}
					}
				}
			}
		}
	}
}

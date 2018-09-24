package io.xol.chunkstories.core.voxel

import org.junit.Test

class BigVoxelLogicTest {
    @Test
    fun testBigVoxel() {
        val xWidth = 16
        val height = 4
        val zWidth = 4

        val xBits = Math.ceil(Math.log(xWidth.toDouble()) / Math.log(2.0)).toInt()
        val yBits = Math.ceil(Math.log(height.toDouble()) / Math.log(2.0)).toInt()
        val zBits = Math.ceil(Math.log(zWidth.toDouble()) / Math.log(2.0)).toInt()

        println(xBits.toString() + " : " + yBits + "  " + zBits)

        val xMask = Math.pow(2.0, xBits.toDouble()).toInt() - 1
        val yMask = Math.pow(2.0, yBits.toDouble()).toInt() - 1
        val zMask = Math.pow(2.0, zBits.toDouble()).toInt() - 1

        println(xMask.toString() + " : " + yMask + "  " + zMask)

        val xShift = 0
        val zShift = xBits + yBits

        val xMask_shifted = xMask shl xShift
        val yMask_shifted = yMask shl xBits
        val zMask_shifted = zMask shl zShift

        println(xMask_shifted.toString() + " : " + yMask_shifted + "  " + zMask_shifted)

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
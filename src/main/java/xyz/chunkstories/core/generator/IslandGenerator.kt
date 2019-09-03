//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator

import xyz.chunkstories.api.math.Math2
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.generator.WorldGeneratorDefinition

class IslandGenerator(type: WorldGeneratorDefinition, world: World)//		BufferedImage image = new BufferedImage(this.worldSizeInBlocks, worldSizeInBlocks, BufferedImage.TYPE_4BYTE_ABGR);
//
//		for(int x = 0; x < worldSizeInBlocks; x++) {
//			for(int y = 0; y < worldSizeInBlocks; y++) {
//				int height = this.getHeightAtInternal(x, y);
//
//				//System.out.println(height);
//				int rgb = 0xFF << 24 | (((height << 8) | height ) << 8 | height);
//				if(height < WATER_HEIGHT)
//					rgb = 0xFF << 24 | (((height << 8) | height ) << 8 | 0x5F);
//
//				//image.getRaster().
//				image.setRGB(x, y, rgb);
//			}
//		}
//
//		try {
//			ImageIO.write(image, "PNG", new File("output.png"));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println(new File("output.png").getAbsolutePath());
    : HorizonGenerator(type, world) {

    internal fun fractalNoise(x: Float, z: Float, octaves: Int, freq: Float, persistence: Float): Float {
        var freq = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        // freq *= worldSizeInBlocks / (64 * 32);
        for (i in 0 until octaves) {
            total += ssng.noise(x * freq, z * freq) * amplitude
            // total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks) *
            // amplitude;
            freq *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    internal fun ridgedNoise(x: Float, z: Float, octaves: Int, freq: Float, persistence: Float): Float {
        var freq = freq
        var total = 0.0f
        var maxAmplitude = 0.0f
        var amplitude = 1.0f
        // freq *= worldSizeInBlocks / (64 * 32);
        for (i in 0 until octaves) {
            total += (1.0f - Math.abs(ssng.noise(x * freq, z * freq))) * amplitude
            // total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq,
            // worldSizeInBlocks))) * amplitude;
            freq *= 2.0f
            maxAmplitude += amplitude
            amplitude *= persistence
        }
        return total / maxAmplitude
    }

    override fun getHeightAtInternal(x: Int, z: Int): Int {
        return getHeightAtInternal(x.toFloat(), z.toFloat())
    }

    internal fun getHeightAtInternal(x: Float, z: Float): Int {
        var x = x
        var z = z

        val nx = x / worldSizeInBlocks
        val nz = z / worldSizeInBlocks

        x /= 256f
        z /= 256f

        var centerness = (1.0f - Math.sqrt(((nx - 0.5f) * (nx - 0.5f) + (nz - 0.5f) * (nz - 0.5f)).toDouble())).toFloat()
        centerness *= centerness
        // centerness -= 0.35f;
        // centerness *= 2.0f;
        centerness = Math2.clamp(centerness.toDouble(), 0.0, 1.0)

        // System.out.println("nx"+nx+": nz"+nz);

        var height = (-20.0f + Math.sqrt(centerness.toDouble()) * 150).toFloat()

        // height += centerness * 128 - 64;

        val baseHeight = fractalNoise(x, z, 5, 1.0f, 0.5f)

        height += baseHeight * BASE_HEIGHT_SCALE.toFloat() * 2f - BASE_HEIGHT_SCALE

        var mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f)
        mountainFactor *= (1.0 + 0.25 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f)).toFloat()
        // mountainFactor -= MOUNTAIN_OFFSET;
        // mountainFactor /= (1 - MOUNTAIN_OFFSET);
        mountainFactor = Math2.clamp(mountainFactor.toDouble(), 0.0, 100.0)

        height += (mountainFactor.toDouble() * MOUNTAIN_SCALE.toDouble() * (0.0 + centerness * 2.0)).toFloat()

        var plateauHeight = Math2.clamp((fractalNoise(x + 225, z + 321, 3, 1f, 0.5f) * 32.0f - 8.0f).toDouble(), 0.0, 1.0)
        plateauHeight *= Math2.clamp((fractalNoise(x + 3158, z + 9711, 3, 0.125f, 0.5f) * 0.5f + 0.5f).toDouble(), 0.0, 1.0)

        // height = 64 + plateauHeight * PLATEAU_HEIGHT_SCALE;

        // height = centerness * 255;

        if (height < 0)
            height = 0f
        if (height > 255)
            height = 255f

        var edge = centerness
        edge -= 0.26f
        edge *= 8.0f
        edge = Math2.clamp(edge.toDouble(), 0.0, 1.0)

        height = Math2.mix(20.0, height.toDouble(), edge.toDouble())

        // height = edge > 0 ? edge * 255 : 0;

        return height.toInt()
    }
}

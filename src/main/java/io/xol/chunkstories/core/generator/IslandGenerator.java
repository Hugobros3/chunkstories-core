//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.generator;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.generator.WorldGeneratorDefinition;

public class IslandGenerator extends HorizonGenerator {

    public IslandGenerator(WorldGeneratorDefinition type, World world) {
        super(type, world);

//		BufferedImage image = new BufferedImage(this.worldSizeInBlocks, worldSizeInBlocks, BufferedImage.TYPE_4BYTE_ABGR);
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
    }

    float fractalNoise(float x, float z, int octaves, float freq, float persistence) {
        float total = 0.0f;
        float maxAmplitude = 0.0f;
        float amplitude = 1.0f;
        // freq *= worldSizeInBlocks / (64 * 32);
        for (int i = 0; i < octaves; i++) {
            total += ssng.noise(x * freq, z * freq) * amplitude;
            // total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks) *
            // amplitude;
            freq *= 2.0f;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }
        return total / maxAmplitude;
    }

    float ridgedNoise(float x, float z, int octaves, float freq, float persistence) {
        float total = 0.0f;
        float maxAmplitude = 0.0f;
        float amplitude = 1.0f;
        // freq *= worldSizeInBlocks / (64 * 32);
        for (int i = 0; i < octaves; i++) {
            total += (1.0f - Math.abs(ssng.noise(x * freq, z * freq))) * amplitude;
            // total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq,
            // worldSizeInBlocks))) * amplitude;
            freq *= 2.0f;
            maxAmplitude += amplitude;
            amplitude *= persistence;
        }
        return total / maxAmplitude;
    }

    @Override
    int getHeightAtInternal(int x, int z) {
        return getHeightAtInternal((float) x, (float) z);
    }

    int getHeightAtInternal(float x, float z) {

        float nx = (float) x / worldSizeInBlocks;
        float nz = (float) z / worldSizeInBlocks;

        x /= 256;
        z /= 256;

        float centerness = (float) (1.0f - Math.sqrt((nx - 0.5f) * (nx - 0.5f) + (nz - 0.5f) * (nz - 0.5f)));
        centerness *= centerness;
        // centerness -= 0.35f;
        // centerness *= 2.0f;
        centerness = Math2.clamp(centerness, 0.0, 1.0);

        // System.out.println("nx"+nx+": nz"+nz);

        float height = (float) (-20.0f + Math.sqrt(centerness) * 150);

        // height += centerness * 128 - 64;

        float baseHeight = fractalNoise(x, z, 5, 1.0f, 0.5f);

        height += baseHeight * BASE_HEIGHT_SCALE * 2 - BASE_HEIGHT_SCALE;

        float mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f);
        mountainFactor *= 1.0 + 0.25 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f);
        // mountainFactor -= MOUNTAIN_OFFSET;
        // mountainFactor /= (1 - MOUNTAIN_OFFSET);
        mountainFactor = Math2.clamp(mountainFactor, 0.0f, 100.0f);

        height += mountainFactor * MOUNTAIN_SCALE * (0.0 + centerness * 2.0);

        float plateauHeight = Math2.clamp(fractalNoise(x + 225, z + 321, 3, 1, 0.5f) * 32.0f - 8.0f, 0.0f, 1.0f);
        plateauHeight *= Math2.clamp(fractalNoise(x + 3158, z + 9711, 3, 0.125f, 0.5f) * 0.5f + 0.5f, 0.0f, 1.0f);

        // height = 64 + plateauHeight * PLATEAU_HEIGHT_SCALE;

        // height = centerness * 255;

        if (height < 0)
            height = 0;
        if (height > 255)
            height = 255;

        float edge = centerness;
        edge -= 0.26;
        edge *= 8.0f;
        edge = Math2.clamp(edge, 0.0, 1.0);

        height = Math2.mix(20, height, edge);

        // height = edge > 0 ? edge * 255 : 0;

        return (int) height;
    }
}

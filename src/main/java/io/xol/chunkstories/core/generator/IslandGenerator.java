//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.generator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.math.random.SeededSimplexNoiseGenerator;

public class IslandGenerator {
	public static void main(String[] a) throws IOException {
		new IslandGenerator();
	}
	
	SeededSimplexNoiseGenerator ssng;
	
	public IslandGenerator() throws IOException {
		ssng = new SeededSimplexNoiseGenerator(/*world.getWorldInfo().getSeed()*/ "484301319400031197119904322214741360655671"+System.currentTimeMillis());
		
		
		BufferedImage image = new BufferedImage(1024, 1024, BufferedImage.TYPE_4BYTE_ABGR);
		
		for(int x = 0; x < 1024; x++) {
			for(int y = 0; y < 1024; y++) {
				int height = this.getHeightAtInternal(x, y);
				
				//System.out.println(height);
				int rgb = 0xFF << 24 | (((height << 8) | height ) << 8 | height);
				if(height < WATER_HEIGHT)
					rgb = 0xFF << 24 | (((height << 8) | height ) << 8 | 0x5F);
				
				//image.getRaster().
				image.setRGB(x, y, rgb);
			}
		}
		
		ImageIO.write(image, "PNG", new File("output.png"));
		System.out.println(new File("output.png").getAbsolutePath());
	}
	
	int worldSizeInBlocks = 1024;
	
	private int WATER_HEIGHT = 48, MOUNTAIN_SCALE = 128, BASE_HEIGHT_SCALE = 76, PLATEAU_HEIGHT_SCALE = 32;
	private double MOUNTAIN_OFFSET = 0.3;
	
	float fractalNoise(float x, float z, int octaves, float freq, float persistence) {
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		//freq *= worldSizeInBlocks / (64 * 32);
		for (int i = 0; i < octaves; i++) {
			total += ssng.noise(x * freq, z * freq) * amplitude;
			//total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks) * amplitude;
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
		//freq *= worldSizeInBlocks / (64 * 32);
		for (int i = 0; i < octaves; i++) {
			total += (1.0f - Math.abs(ssng.noise(x * freq, z * freq))) * amplitude;
			//total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks))) * amplitude;
			freq *= 2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}

	int getHeightAtInternal(float x, float z) {
		
		float nx = x / worldSizeInBlocks;
		float nz = z / worldSizeInBlocks;
		
		x /= 256;
		z /= 256;
		
		float centerness = (float) (1.0f - Math.sqrt((nx - 0.5f) * (nx - 0.5f) + (nz - 0.5f) * (nz - 0.5f)));
		centerness *= centerness * 1.0f;
		centerness -= 0.35f;
		centerness *= 2.0f;
		centerness = Math2.clamp(centerness, 0.0, 1.0);
		
		float height = 0.0f;
		
		height += centerness * 128 - 64;
		
		float baseHeight = ridgedNoise(x, z, 5, 1.0f, 0.5f);
		
		height += baseHeight * BASE_HEIGHT_SCALE; //* centerness;

		float mountainFactor = fractalNoise(x + 548, z + 330, 3, 0.5f, 0.5f);
		mountainFactor *= 1.0 + 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f);
		mountainFactor -= MOUNTAIN_OFFSET;
		mountainFactor /= (1 - MOUNTAIN_OFFSET);
		mountainFactor = Math2.clamp(mountainFactor, 0.0f, 1.0f);
		
		height += mountainFactor * MOUNTAIN_SCALE;// * Math2.clamp(centerness * 10, 0.0, 1.0);
		
		float plateauHeight = Math2.clamp(fractalNoise(x + 225, z + 321, 3, 1, 0.5f) * 32.0f - 8.0f, 0.0f, 1.0f);
		plateauHeight *= Math2.clamp(fractalNoise(x + 3158, z + 9711, 3, 0.125f, 0.5f) * 0.5f + 0.5f, 0.0f, 1.0f);

		/*if(height >= WATER_HEIGHT)
			height += plateauHeight * PLATEAU_HEIGHT_SCALE;
		else
			height += plateauHeight * baseHeight * PLATEAU_HEIGHT_SCALE;*/
		
		//height = 64 + plateauHeight * PLATEAU_HEIGHT_SCALE;
		
		//height = centerness * 255;
		
		if(height < 0)
			height = 0;
		if(height > 255)
			height = 255;
		
		return (int) height;
	}
}

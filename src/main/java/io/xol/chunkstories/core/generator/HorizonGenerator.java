//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.generator;

import java.util.Random;

import io.xol.chunkstories.api.content.Content.WorldGenerators.WorldGeneratorDefinition;
import io.xol.chunkstories.api.math.random.SeededSimplexNoiseGenerator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.generator.environment.DefaultWorldEnvironment;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.generator.environment.WorldEnvironment;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.chunk.Chunk;

public class HorizonGenerator extends WorldGenerator
{
	DefaultWorldEnvironment worldEnv;
	Random rnd = new Random();
	SeededSimplexNoiseGenerator ssng;
	
	int worldSizeInBlocks;
	private Voxel WATER_VOXEL;
	private Voxel GROUND_VOXEL;
	private Voxel UNDERGROUND_VOXEL;
	private Voxel STONE_VOXEL;
	
	public HorizonGenerator(WorldGeneratorDefinition type, World w)
	{
		super(type, w);
		ssng = new SeededSimplexNoiseGenerator(w.getWorldInfo().getSeed());
		worldSizeInBlocks = world.getSizeInChunks() * 32;
		worldEnv = new DefaultWorldEnvironment(world);

		this.STONE_VOXEL = world.getGameContext().getContent().voxels().getVoxelByName("stone");
		this.WATER_VOXEL = world.getGameContext().getContent().voxels().getVoxelByName("water");
		this.GROUND_VOXEL = world.getGameContext().getContent().voxels().getVoxelByName("grass");
		this.UNDERGROUND_VOXEL = world.getGameContext().getContent().voxels().getVoxelByName("dirt");
	}
	
	@Override
	public Chunk generateChunk(Chunk chunk)
	{
		int cx = chunk.getChunkX();
		int cy = chunk.getChunkY();
		int cz = chunk.getChunkZ();
		rnd.setSeed(cx * 32 + cz + 48716148);
		
		//CubicChunk c = new CubicChunk(region, cx, cy, cz);
		Voxel type = null;
		for(int x = 0; x < 32; x++)
			for(int z = 0; z < 32; z++)
			{
				//int v = getHeightAt(cx * 32 + x, cz * 32 + z);
				int v = world.getRegionsSummariesHolder().getHeightAtWorldCoordinates(cx * 32 + x, cz * 32 + z);
				//int v = 250;
				int y = cy * 32;
				while(y < cy * 32 + 32 && y < v)
				{
					if(v - y >= 3)
						type = STONE_VOXEL;
					else if(v - y > 1 || y + 1 < 60)
						type = UNDERGROUND_VOXEL;
					else
						type = GROUND_VOXEL;
					chunk.pokeSimpleSilently(x, y, z, type, -1, -1, 0);
					y++;
				}
				while(y < cy * 32 + 32 && y < 60)
				{
					chunk.pokeSimpleSilently(x, y, z, WATER_VOXEL, -1, -1, 0);
					y++;
				}
			}
		return chunk;
	}
	
	float fractalNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= worldSizeInBlocks / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	float ridgedNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= worldSizeInBlocks / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks))) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	private int getHeightAtInternal(int x, int z)
	{
		float finalHeight = 0.0f;
		
		float mountainFactor = fractalNoise(x + 5487, z + 33320, 3, 1f, 0.5f);
		mountainFactor *= 1.5f * mountainFactor;
		if(mountainFactor > 1.0f)
			mountainFactor = 1f;
		//Mountains
		finalHeight += (ridgedNoise(x, z, 5, 1.0f, 0.5f) * (64 + 128 * mountainFactor));
		
		return (int) finalHeight;
	}
	
	@Override
	public int getHeightAt(int x, int z)
	{
		int finalHeight = getHeightAtInternal(x, z);
		//if(finalHeight < 60)
		//	return 60;
		return finalHeight;
	}
	
	@Override
	public int getTopDataAt(int x, int z)
	{
		//int finalHeight = getHeightAtInternal(x, z);
		//if(finalHeight < 60)
		//	return 128;
		return 2;
	}

	@Override
	public WorldEnvironment getEnvironment() {
		return worldEnv;
	}
}

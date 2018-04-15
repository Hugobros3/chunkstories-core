//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.generator;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.joml.Vector3i;

import io.xol.chunkstories.api.content.Content.WorldGenerators.WorldGeneratorDefinition;
import io.xol.chunkstories.api.converter.MinecraftBlocksTranslator;
import io.xol.chunkstories.api.math.random.SeededSimplexNoiseGenerator;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.structures.McSchematicStructure;
import io.xol.chunkstories.api.voxel.structures.Structure;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.generator.environment.DefaultWorldEnvironment;
import io.xol.chunkstories.api.world.generator.environment.WorldEnvironment;

public class HorizonGenerator extends WorldGenerator
{
	DefaultWorldEnvironment worldEnv;
	SeededSimplexNoiseGenerator ssng;
	
	int worldSizeInBlocks;
	private Voxel WATER_VOXEL;
	private Voxel GROUND_VOXEL;
	private Voxel UNDERGROUND_VOXEL;
	private Voxel STONE_VOXEL;
	private Voxel TALLGRASS;
	
	private Voxel[] SURFACE_DECORATIONS;
	
	private int WATER_HEIGHT = 48;
	
	final int OKA_TREE_VARIANTS = 6;
	Structure oakTrees[] = new Structure[OKA_TREE_VARIANTS];
	
	final int FALLEN_TREE_VARIANTS = 2;
	Structure fallenTrees[] = new Structure[FALLEN_TREE_VARIANTS];
	
	final int REDWOOD_TREE_VARIANTS = 3;
	Structure redwoodTrees[] = new Structure[REDWOOD_TREE_VARIANTS];
	
	Structure treeTypes[][] = {oakTrees, fallenTrees, redwoodTrees};
	
	public HorizonGenerator(WorldGeneratorDefinition type, World w)
	{
		super(type, w);
		ssng = new SeededSimplexNoiseGenerator(w.getWorldInfo().getSeed());
		worldSizeInBlocks = world.getSizeInChunks() * 32;
		worldEnv = new DefaultWorldEnvironment(world);

		this.STONE_VOXEL = world.getGameContext().getContent().voxels().getVoxel("stone");
		this.WATER_VOXEL = world.getGameContext().getContent().voxels().getVoxel("water");
		this.GROUND_VOXEL = world.getGameContext().getContent().voxels().getVoxel("grass");
		this.UNDERGROUND_VOXEL = world.getGameContext().getContent().voxels().getVoxel("dirt");
		this.TALLGRASS = world.getGameContext().getContent().voxels().getVoxel("grass_prop");
		
		try {
			MinecraftBlocksTranslator translator = new MinecraftBlocksTranslator(w.getGameContext(), new File("converter_mapping.txt"));
			
			for(int i = 1; i <= OKA_TREE_VARIANTS; i++)
				oakTrees[i-1] = McSchematicStructure.fromAsset(w.getContent().getAsset("./structures/oak_tree"+i+".schematic"), translator);
			
			for(int i = 1; i <= FALLEN_TREE_VARIANTS; i++)
				fallenTrees[i-1] = McSchematicStructure.fromAsset(w.getContent().getAsset("./structures/oak_fallen"+i+".schematic"), translator);

			for(int i = 1; i <= REDWOOD_TREE_VARIANTS; i++)
				redwoodTrees[i-1] = McSchematicStructure.fromAsset(w.getContent().getAsset("./structures/redwood_tree"+i+".schematic"), translator);
			
			String decorations[] = new String[] {"flower_yellow", "flower_red", "flower_orange", "flower_blue", "flower_purple", "flower_white", "mushroom_red", "mushroom_brown"};
			SURFACE_DECORATIONS = new Voxel[decorations.length];
			for(int i = 1; i <= REDWOOD_TREE_VARIANTS; i++)
				SURFACE_DECORATIONS[i] = world.getGameContext().getContent().voxels().getVoxel(decorations[i]);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void generateChunk(Chunk chunk)
	{
		Random rnd = new Random();
		
		int cx = chunk.getChunkX();
		int cy = chunk.getChunkY();
		int cz = chunk.getChunkZ();
		
		if(chunk.getChunkY() * 32 >= 384)
			return;
		
		Voxel voxel = null;
		for(int x = 0; x < 32; x++)
			for(int z = 0; z < 32; z++)
			{
				int groundHeight = getHeightAtInternal(cx * 32 + x, cz * 32 + z);
				
				int y = cy * 32;
				while(y < cy * 32 + 32 && y <= groundHeight)
				{
					if(groundHeight - y > 3) // more than 3 blocks underground => deep ground
						voxel = STONE_VOXEL;
					else if(y < groundHeight) // dirt
						voxel = UNDERGROUND_VOXEL;
					else if(y < WATER_HEIGHT)
						voxel = UNDERGROUND_VOXEL;
					else
						voxel = GROUND_VOXEL; // ground
					
					chunk.pokeSimpleSilently(x, y, z, voxel, -1, -1, 0);
					y++;
				}
				
				if(y < cy * 32 + 32 && y == groundHeight + 1 && y > WATER_HEIGHT) {
					//Top soil!
					double woab = Math.random();
					if(woab > 0.5) {
						if(woab > 0.95) {
							Voxel surfaceVoxel = SURFACE_DECORATIONS[rnd.nextInt(SURFACE_DECORATIONS.length)];
							chunk.pokeSimpleSilently(x, y, z, surfaceVoxel, -1, -1, 0);
						} else
							chunk.pokeSimpleSilently(x, y, z, TALLGRASS, -1, -1, 0); 
					}
				}
				
				while(y < cy * 32 + 32 && y <= WATER_HEIGHT)
				{
					chunk.pokeSimpleSilently(x, y, z, WATER_VOXEL, -1, -1, 0);
					y++;
				}
			}
		
		addTrees(chunk, rnd);
	}
	
	private void addTrees(Chunk chunk, Random rnd) {
		
		int cx = chunk.getChunkX();
		int cy = chunk.getChunkY();
		int cz = chunk.getChunkZ();

		//take into account the nearby chunks
		for(int gcx = cx - 1; gcx <= cx + 1; gcx++)
				for(int gcz = cz - 1; gcz <= cz + 1; gcz++) {
					rnd.setSeed(gcx * 32 + gcz + 48716148);
					
					//how many trees are there supposed to be in that chunk
					float treenoise = 0.5f + fractalNoise(gcx * 32 + 2, gcz * 32 + 32, 3, 0.25f, 0.85f);
					int ntrees = (int) Math.max(0, treenoise * 25);
					//ntrees = 5;
					//System.out.println(fractalNoise(gcx * 32 + 2, gcz * 32 + 32, 3, 0.25f, 0.85f));
					for(int i = 0; i < ntrees; i++) {
						int x = gcx * 32 + rnd.nextInt(32);
						int z = gcz * 32 + rnd.nextInt(32);
						
						int y = getHeightAtInternal(x, z) - 1;
						if(y > WATER_HEIGHT) {
							
							int type = rnd.nextInt(treeTypes.length);
							
							Structure[] treeType = treeTypes[type];
							
							int variant = rnd.nextInt(treeType.length);
							treeType[variant].paste(chunk, new Vector3i(x, y, z), Structure.FLAG_USE_OFFSET | Structure.FLAG_DONT_OVERWRITE_AIR);
						}
					}
				}
	}
	
	float fractalNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= worldSizeInBlocks / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks * freq) * amplitude;
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
			total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, worldSizeInBlocks ))) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	private int getHeightAtInternal(int x, int z)
	{
		float finalHeight = 0.0f;
		
		float mountainFactor = fractalNoise(x + 5487, z + 33320, 3, 0.125f, 0.5f);
		mountainFactor *= 1.0 - 0.125 * ridgedNoise(x + 14, z + 9977, 2, 4.0f, 0.7f);
		if(mountainFactor > 1.0f)
			mountainFactor = 1f;
		
		//Mountains
		finalHeight += (ridgedNoise(x, z, 5, 1.0f, 0.5f) * 64 + 128 * mountainFactor);
		
		return (int) finalHeight;
	}

	@Override
	public WorldEnvironment getEnvironment() {
		return worldEnv;
	}
}

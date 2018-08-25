//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.generator;

import io.xol.chunkstories.api.content.Content.WorldGenerators.WorldGeneratorDefinition;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.generator.WorldGenerator;
import io.xol.chunkstories.api.world.generator.environment.DefaultWorldEnvironment;
import io.xol.chunkstories.api.world.generator.environment.WorldEnvironment;

import java.util.Random;

public class FlatGenerator extends WorldGenerator {
	DefaultWorldEnvironment worldEnv;
	Random rnd = new Random();

	public FlatGenerator(WorldGeneratorDefinition type, World world) {
		super(type, world);
		worldsize = world.getSizeInChunks() * 32;
		worldEnv = new DefaultWorldEnvironment(world);

		this.GROUND_VOXEL = world.getGameContext().getContent().voxels().getVoxel("grass");
		this.WALL_VOXEL = world.getGameContext().getContent().voxels().getVoxel("cobble");
		this.WALL_TOP_VOXEL = world.getGameContext().getContent().voxels().getVoxel("ironGrill");
	}

	int worldsize;
	private final Voxel GROUND_VOXEL;
	private final Voxel WALL_VOXEL;
	private final Voxel WALL_TOP_VOXEL;

	@Override
	public void generateChunk(Chunk chunk) {
		int cx = chunk.getChunkX();
		int cy = chunk.getChunkY();
		int cz = chunk.getChunkZ();

		rnd.setSeed(cx * 32 + cz + 48716148);

		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++) {
				Voxel type = WALL_VOXEL; // cobble

				int v = 21; // base height
				if ((cx * 32 + x) % 256 == 0 || (cz * 32 + z) % 256 == 0) {
					v = 30; // wall height
				} else
					type = GROUND_VOXEL;
				// int v = 250;
				int y = cy * 32;
				while (y < cy * 32 + 32 && y <= v) {
					if (y == 30)
						type = WALL_TOP_VOXEL;
					chunk.pokeSimpleSilently(x, y, z, type, -1, -1, 0);
					y++;
				}
			}
	}

	@Override
	public WorldEnvironment getEnvironment() {
		return worldEnv;
	}
}

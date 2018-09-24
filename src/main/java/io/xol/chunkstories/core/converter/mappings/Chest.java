//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.NonTrivialMapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.enklume.MinecraftRegion;

public class Chest extends NonTrivialMapper {

	public Chest(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion,
			int x, int y, int z) {

		Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;

		csWorld.pokeSimpleSilently(new FutureCell(csWorld, csX, csY, csZ, voxel));
	}

}
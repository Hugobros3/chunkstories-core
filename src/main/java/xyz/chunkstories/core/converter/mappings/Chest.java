//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings;

import xyz.chunkstories.api.converter.mappings.NonTrivialMapper;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.Chunk;
import io.xol.enklume.MinecraftRegion;

public class Chest extends NonTrivialMapper {

	public Chest(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion,
			int x, int y, int z) {

		Chunk chunk = csWorld.getChunksManager().getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;

		csWorld.pokeSimpleSilently(new FutureCell(csWorld, csX, csY, csZ, voxel));
	}

}
//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings;

import xyz.chunkstories.api.converter.mappings.NonTrivialMapper;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.core.voxel.VoxelDoor;
import io.xol.enklume.MinecraftRegion;

import static xyz.chunkstories.api.util.compatibility.MinecraftSidesKt.getSideMcDoor;

public class Door extends NonTrivialMapper {

	public Door(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion,
			int x, int y, int z) {

		Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;

		int upper = (minecraftMetaData & 0x8) >> 3;
		int open = (minecraftMetaData & 0x4) >> 2;

		// We only place the lower half of the door and the other half is created by the
		// placing logic of chunk stories
		if (upper != 1) {
			int upperMeta = region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion)
					.getBlockMeta(x, y + 1, z);

			int hingeSide = upperMeta & 0x01;
			int direction = minecraftMetaData & 0x3;

			csWorld.pokeSimple(csX, csY, csZ, voxel, -1, -1,
					VoxelDoor.computeMeta(open == 1, hingeSide == 1, getSideMcDoor(direction)));
		} else
			return;

	}
}
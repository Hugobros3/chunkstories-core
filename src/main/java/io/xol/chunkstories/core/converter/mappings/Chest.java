//
// This file is a part of the Chunk Stories API codebase
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
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion,
			int minecraftCuurrentChunkZinsideRegion, int x, int y, int z) {

		Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;
		
		/*int baked = voxelID;
		
		if (voxel instanceof VoxelChest)
			try {
				baked = ((VoxelChest) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
			} catch (WorldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else
			System.out.println("fuck you 666");
		
		csWorld.pokeRawSilently(csX, csY, csZ, baked);*/
		csWorld.pokeSimple(new FutureCell(csWorld, csX, csY, csZ, voxel));
	}
	
}
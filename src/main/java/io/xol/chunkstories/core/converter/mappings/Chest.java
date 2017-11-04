package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.NonTrivialMapper;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.VoxelChest;
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
		
		int baked = voxelID;
		
		if (voxel instanceof VoxelChest)
			try {
				baked = ((VoxelChest) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
			} catch (WorldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else
			System.out.println("fuck you 666");
		
		csWorld.pokeSimpleSilently(csX, csY, csZ, baked);
	}
	
}
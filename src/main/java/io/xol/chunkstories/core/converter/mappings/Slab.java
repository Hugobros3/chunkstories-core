package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.FutureVoxelContext;

public class Slab extends Mapper {
	
	public Slab(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(int minecraftId, byte minecraftMeta, FutureVoxelContext fvc) {
		fvc.setVoxel(voxel);
		if(minecraftMeta >= 8)
			fvc.setMetaData(1);
			//return VoxelFormat.changeMeta(voxelID, 1);
		//else return voxelID;
	}
}
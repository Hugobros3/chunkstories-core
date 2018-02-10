package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.cell.FutureCell;

public class Slab extends Mapper {
	
	public Slab(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(int minecraftId, byte minecraftMeta, FutureCell cell) {
		cell.setVoxel(voxel);
		if(minecraftMeta >= 8)
			cell.setMetaData(1);
			//return VoxelFormat.changeMeta(voxelID, 1);
		//else return voxelID;
	}
}
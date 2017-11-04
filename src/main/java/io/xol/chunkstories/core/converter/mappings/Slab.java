package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;

public class Slab extends Mapper {
	
	public Slab(Voxel voxel) {
		super(voxel);
	}

	@Override
	public int output(int minecraftId, byte minecraftMeta) {
		if(minecraftMeta >= 8)
			return VoxelFormat.changeMeta(voxelID, 1);
		else return voxelID;
	}
}
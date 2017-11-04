package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;

public class KeepMeta extends Mapper {
	
	public KeepMeta(Voxel voxel) {
		super(voxel);
	}

	@Override
	public int output(int minecraftId, byte minecraftMeta) {
		return VoxelFormat.changeMeta(voxelID, minecraftMeta);
	}
}

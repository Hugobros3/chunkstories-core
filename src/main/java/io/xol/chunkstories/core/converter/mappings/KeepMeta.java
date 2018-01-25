package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.FutureVoxelContext;

public class KeepMeta extends Mapper {
	
	public KeepMeta(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(int minecraftId, byte minecraftMeta, FutureVoxelContext fvc) {
		fvc.setVoxel(voxel);
		fvc.setMetaData(minecraftMeta);
	}
}

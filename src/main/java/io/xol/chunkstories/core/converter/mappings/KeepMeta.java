//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.Mapper;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.cell.FutureCell;

public class KeepMeta extends Mapper {
	
	public KeepMeta(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(int minecraftId, byte minecraftMeta, FutureCell cell) {
		cell.setVoxel(voxel);
		cell.setMetaData(minecraftMeta);
	}
}

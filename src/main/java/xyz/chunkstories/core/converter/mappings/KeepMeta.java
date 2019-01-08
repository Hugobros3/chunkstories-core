//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings;

import xyz.chunkstories.api.converter.mappings.Mapper;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.world.cell.FutureCell;

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

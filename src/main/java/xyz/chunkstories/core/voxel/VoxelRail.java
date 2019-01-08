//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.world.cell.CellData;

public class VoxelRail extends Voxel {
	public VoxelRail(VoxelDefinition type) {
		super(type);
	}

	/*@Override
	public VoxelModel getVoxelRenderer(CellData info) {
		if (info.getNeightborVoxel(VoxelSide.FRONT.ordinal()).sameKind(this))
			return store.models().getVoxelModel("rails.alt");

		return store.models().getVoxelModel("rails.default");
	}*/
}

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelRail extends Voxel
{
	public VoxelRail(VoxelDefinition type)
	{
		super(type);
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		if(info.getNeightborVoxel(VoxelSides.FRONT.ordinal()).sameKind(this))
			return store.models().getVoxelModelByName("rails.alt");

		return store.models().getVoxelModelByName("rails.default");
	}
}

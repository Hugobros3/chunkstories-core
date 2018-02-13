package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.voxel.renderers.VoxelWaterRenderer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelLiquid extends Voxel
{
	VoxelWaterRenderer surface;
	VoxelWaterRenderer inside;

	public VoxelLiquid(VoxelDefinition type)
	{
		super(type);
		inside = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.inside"));
		surface = new VoxelWaterRenderer(store.models().getVoxelModelByName("water.surface"));
	}

	@Override
	public VoxelWaterRenderer getVoxelRenderer(CellData info)
	{
		//Return the surface only if the top block isn't liquid
		if(!info.getNeightborVoxel(4).getDefinition().isLiquid()) {
			return surface;
		}
		
		else return inside;
	}
}

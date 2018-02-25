//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelHalfTile extends Voxel
{

	VoxelModel bot;
	VoxelModel top;

	public VoxelHalfTile(VoxelDefinition type)
	{
		super(type);
		bot = store.models().getVoxelModelByName("halftile.bottom");
		top = store.models().getVoxelModelByName("halftile.top");
	}

	boolean bottomOrTop(int meta)
	{
		return meta % 2 == 0;
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		int meta = info.getMetaData();
		if (bottomOrTop(meta))
			return bot;
		return top;
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		// System.out.println("kek");
		CollisionBox box2 = new CollisionBox(1, 0.5, 1);
		if (bottomOrTop(info.getMetaData()))
			box2.translate(0.0, -0, 0.0);
		else
			box2.translate(0.0, +0.5, 0.0);
		return new CollisionBox[] { box2 };
	}
	
	@Override
	public int getLightLevelModifier(CellData dataFrom, CellData dataTo, VoxelSides side2)
	{
		int side = side2.ordinal();
		
		//Special cases when half-tiles meet
		if(dataTo.getVoxel() instanceof VoxelHalfTile && side < 4)
		{
			//If they are the same type, allow the light to transfer
			if(bottomOrTop(dataFrom.getMetaData()) == bottomOrTop(dataTo.getMetaData()))
				return 2;
			else
				return 15;
		}
		if (bottomOrTop(dataFrom.getMetaData()) && side == 5)
			return 15;
		if (!bottomOrTop(dataFrom.getMetaData()) && side == 4)
			return 15;
		return 2;
	}
}

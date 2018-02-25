//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.core.entity.EntityPlayer;

public class VoxelStairs extends Voxel
{
	VoxelModel[] models = new VoxelModel[8];

	public VoxelStairs(VoxelDefinition type)
	{
		super(type);
		for (int i = 0; i < 8; i++)
			models[i] = store.models().getVoxelModelByName("stairs.m" + i);
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		int meta = info.getMetaData();
		return models[meta % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		int meta = info.getMetaData();
		CollisionBox[] boxes = new CollisionBox[2];
		boxes[0] = new CollisionBox(1, 0.5, 1);//.translate(0.5, -1, 0.5);
		switch (meta % 4)
		{
		case 0:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.0);
			break;
		case 1:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.0, -0.0, 0.0);
			break;
		case 2:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.5);
			break;
		case 3:
			boxes[1] = new CollisionBox(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.0);
			break;
		default:
			boxes[1] = new CollisionBox(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.25);
			break;
		}
		
		if (meta / 4 == 0)
		{
			boxes[0].translate(0.0, 0.0, 0.0);
			boxes[1].translate(0.0, 0.5, 0.0);
		}
		else
		{
			boxes[0].translate(0.0, 0.5, 0.0);
			boxes[1].translate(0.0, 0.0, 0.0);
		}

		return boxes;
	}

	@Override
	public void onPlace(FutureCell cell, WorldModificationCause cause)
	{
		// id+dir of slope
		// 0LEFT x-
		// 1RIGHT x+
		// 2BACK z-
		// 3FRONT z+
		
		int stairsSide = 0;
		if (cause != null && cause instanceof Entity)
		{
			Entity entity = (Entity)cause;
			Location loc = entity.getLocation();
			double dx = loc.x() - (cell.getX() + 0.5);
			double dz = loc.z() - (cell.getZ() + 0.5);

			//System.out.println("dx: "+dx+" dz:" + dz);
			
			if (Math.abs(dx) > Math.abs(dz))
			{
				if(dx > 0)
					stairsSide = 1;
				else
					stairsSide = 0;
			}
			else
			{
				if(dz > 0)
					stairsSide = 3;
				else
					stairsSide = 2;
			}
			
			if(entity instanceof EntityPlayer)
			{
				if(((EntityPlayer)entity).getEntityRotationComponent().getVerticalRotation() < 0)
					stairsSide += 4;
			}
			
			cell.setMetaData(stairsSide);
		}
	}
}

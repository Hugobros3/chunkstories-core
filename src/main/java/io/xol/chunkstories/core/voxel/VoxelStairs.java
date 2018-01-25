package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.FutureVoxelContext;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;
import io.xol.chunkstories.core.entity.EntityPlayer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelStairs extends Voxel implements VoxelLogic
{
	VoxelModel[] models = new VoxelModel[8];

	public VoxelStairs(VoxelDefinition type)
	{
		super(type);
		for (int i = 0; i < 8; i++)
			models[i] = store.models().getVoxelModelByName("stairs.m" + i);
	}

	@Override
	public VoxelModel getVoxelRenderer(VoxelContext info)
	{
		int meta = info.getMetaData();
		return models[meta % 8];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(VoxelContext info)
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
	public FutureVoxelContext onPlace(ChunkVoxelContext context, FutureVoxelContext voxelData, WorldModificationCause cause)
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
			double dx = loc.x() - (context.getX() + 0.5);
			double dz = loc.z() - (context.getZ() + 0.5);

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
			
			voxelData.setMetaData(stairsSide);
		}
		return voxelData;
	}

	@Override
	public void onRemove(ChunkVoxelContext context, WorldModificationCause cause)
	{
		//System.out.println("on remove stairs");
	}
	
	@Override
	public FutureVoxelContext onModification(ChunkVoxelContext context, FutureVoxelContext voxelData, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		return voxelData;
	}
}

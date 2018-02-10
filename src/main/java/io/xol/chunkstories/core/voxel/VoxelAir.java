package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelAir extends Voxel {

	public VoxelAir(VoxelDefinition type)
	{
		super(type);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		return noCollisionBoxes;
	}
	
	final static CollisionBox[] noCollisionBoxes = new CollisionBox[] {};
}

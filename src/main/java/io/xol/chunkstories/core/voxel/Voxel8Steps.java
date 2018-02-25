//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.voxel.renderers.SmoothStepVoxelRenderer;

public class Voxel8Steps extends Voxel
{
	VoxelModel[] steps = new VoxelModel[8];
	SmoothStepVoxelRenderer nextGen;

	public Voxel8Steps(VoxelDefinition type)
	{
		super(type);
		for(int i = 0; i < 8; i++)
			steps[i] = store().models().getVoxelModelByName("steps.m"+i);
		
		nextGen = new SmoothStepVoxelRenderer(this, steps);
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(CellData info)
	{
		//return nextGen;
		return steps[info.getMetaData() % 8];
	}

	@Override
	public boolean isFaceOpaque(VoxelSides side, int data) {
		if(side == VoxelSides.BOTTOM)
			return true;
		if(side == VoxelSides.TOP)
			return true;
		
		return super.isFaceOpaque(side, data);
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		CollisionBox box2 = new CollisionBox(1, (info.getMetaData() % 8 + 1) / 8f, 1);
		return new CollisionBox[] { box2 };
	}
}

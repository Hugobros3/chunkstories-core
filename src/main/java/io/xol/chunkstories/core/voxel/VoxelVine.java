//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelVine extends Voxel implements VoxelClimbable
{
	VoxelModel[] models = new VoxelModel[4];

	public VoxelVine(VoxelDefinition type)
	{
		super(type);
		for (int i = 0; i < 4; i++)
			models[i] = store.models().getVoxelModelByName("dekal.m" + i);
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		int meta = info.getMetaData();
		if(meta == 1)
			return models[2];
		else if(meta == 2)
			return models[1];
		else if(meta == 4)
			return models[3];
		else if(meta == 8)
			return models[0];
		return models[0];
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info) {
		
		int meta = info.getMetaData();
		if(meta == 1)
			return new CollisionBox[]{new CollisionBox(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9)};
		if(meta == 2)
			return new CollisionBox[]{new CollisionBox(0.1, 1.0, 1.0).translate(0.0, 0.0, 0.0)};
		if(meta == 4)
			return new CollisionBox[]{new CollisionBox(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.0)};
		if(meta == 8)
			return new CollisionBox[]{new CollisionBox(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0)};
		
		return super.getCollisionBoxes(info);
	}
}

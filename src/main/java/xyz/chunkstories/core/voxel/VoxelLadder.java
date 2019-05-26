//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.physics.Box;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.world.cell.CellData;

public class VoxelLadder extends Voxel implements VoxelClimbable {
	//VoxelModel[] models = new VoxelModel[4];

	public VoxelLadder(VoxelDefinition definition) {
		super(definition);
		//for (int i = 0; i < 4; i++)
		//	models[i] = store.models().getVoxelModel("dekal.m" + i);
	}

	/*@Override
	public VoxelModel getVoxelRenderer(CellData info) {
		int meta = info.getMetaData();

		if (meta == 2)
			return models[2];
		else if (meta == 3)
			return models[3];
		else if (meta == 4)
			return models[0];
		else if (meta == 5)
			return models[1];
		return models[0];
	}*/

	@Override
	public Box[] getCollisionBoxes(CellData info) {

		int meta = info.getMetaData();

		if (meta == 2)
			return new Box[] { new Box(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9) };
		if (meta == 3)
			return new Box[] { new Box(1.0, 1.0, 0.1) };
		if (meta == 4)
			return new Box[] { new Box(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0) };
		if (meta == 5)
			return new Box[] { new Box(0.1, 1.0, 1.0) };

		return super.getCollisionBoxes(info);
	}

}

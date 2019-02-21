//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.physics.Box;
//import xyz.chunkstories.api.rendering.voxel.VoxelRenderer;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.world.cell.CellData;

public class VoxelHalfTile extends Voxel {
	//VoxelRenderer bot;
	//VoxelRenderer top;

	public VoxelHalfTile(VoxelDefinition type) {
		super(type);
		//bot = store.models().getVoxelModel("halftile.bottom");
		//top = store.models().getVoxelModel("halftile.top");
	}

	boolean bottomOrTop(int meta) {
		return meta % 2 == 0;
	}

	/*@Override
	public VoxelRenderer getVoxelRenderer(CellData info) {
		int meta = info.getMetaData();
		if (bottomOrTop(meta))
			return bot;
		return top;
	}*/

	@Override
	public Box[] getCollisionBoxes(CellData info) {
		// System.out.println("kek");
		Box box2 = new Box(1, 0.5, 1);
		if (bottomOrTop(info.getMetaData()))
			box2.translate(0.0, -0, 0.0);
		else
			box2.translate(0.0, +0.5, 0.0);
		return new Box[] { box2 };
	}

	@Override
	public int getLightLevelModifier(CellData dataFrom, CellData dataTo, VoxelSide side2) {
		int side = side2.ordinal();

		// Special cases when half-tiles meet
		if (dataTo.getVoxel() instanceof VoxelHalfTile && side < 4) {
			// If they are the same type, allow the light to transfer
			if (bottomOrTop(dataFrom.getMetaData()) == bottomOrTop(dataTo.getMetaData()))
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
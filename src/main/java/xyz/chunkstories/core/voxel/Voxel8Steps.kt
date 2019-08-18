//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

class Voxel8Steps
//VoxelModel[] steps = new VoxelModel[8];

(definition: VoxelDefinition)//for (int i = 0; i < 8; i++)
//	steps[i] = store().models().getVoxelModel("steps.m" + i);
    : Voxel(definition) {

    /*@Override
	public VoxelRenderer getVoxelRenderer(CellData info) {
		// return nextGen;
		return steps[info.getMetaData() % 8];
	}*/

    override fun isFaceOpaque(side: VoxelSide, metadata: Int): Boolean {
        if (side == VoxelSide.BOTTOM)
            return true
        return if (side == VoxelSide.TOP) true else super.isFaceOpaque(side, metadata)

    }

    override fun getCollisionBoxes(info: Cell): Array<Box>? {
        val box2 = Box.fromExtents(1.0, (info.metaData % 8 + 1) / 8.0, 1.0)
        return arrayOf(box2)
    }
}

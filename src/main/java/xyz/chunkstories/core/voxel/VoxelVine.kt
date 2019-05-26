//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.world.cell.CellData

class VoxelVine(definition: VoxelDefinition) : Voxel(definition), VoxelClimbable {

    /*@Override
	public VoxelRenderer getVoxelRenderer(CellData info) {
		int meta = info.getMetaData();
		if (meta == 1)
			return models[2];
		else if (meta == 2)
			return models[1];
		else if (meta == 4)
			return models[3];
		else if (meta == 8)
			return models[0];
		return models[0];
	}*/

    override fun getCollisionBoxes(info: CellData): Array<Box>? {

        val meta = info.metaData
        if (meta == 1)
            return arrayOf(Box(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.9))
        if (meta == 2)
            return arrayOf(Box(0.1, 1.0, 1.0).translate(0.0, 0.0, 0.0))
        if (meta == 4)
            return arrayOf(Box(1.0, 1.0, 0.1).translate(0.0, 0.0, 0.0))
        return if (meta == 8) arrayOf(Box(0.1, 1.0, 1.0).translate(0.9, 0.0, 0.0)) else super.getCollisionBoxes(info)

    }
}

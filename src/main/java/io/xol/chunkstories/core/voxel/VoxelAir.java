//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.Box;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelAir extends Voxel {

    public VoxelAir(VoxelDefinition definition) {
        super(definition);
    }

    @Override
    public Box[] getCollisionBoxes(CellData info) {
        return noCollisionBoxes;
    }

    final static Box[] noCollisionBoxes = new Box[]{};
}

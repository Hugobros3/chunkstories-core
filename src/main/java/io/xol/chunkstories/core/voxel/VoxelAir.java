//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelAir extends Voxel {

    public VoxelAir(Content.Voxels store) {
        super(store);
    }

    @Override
    public CollisionBox[] getCollisionBoxes(CellData info) {
        return noCollisionBoxes;
    }

    final static CollisionBox[] noCollisionBoxes = new CollisionBox[]{};
}

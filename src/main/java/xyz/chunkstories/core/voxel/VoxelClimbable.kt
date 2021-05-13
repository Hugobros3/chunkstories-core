//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.core.entity.blocksWithin

/**
* Interface telling the entities they can climb it Not in the API because it's
* specific to gameplay and don't have anything to do there
*/
interface VoxelClimbable

fun Entity.isOnLadder() : Boolean = this.blocksWithin().any { it.data.blockType is VoxelClimbable }
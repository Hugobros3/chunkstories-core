//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.core.entity.blocksWithin

fun Entity.isInLiquid() : Boolean = this.blocksWithin().any { it.data.blockType.liquid }
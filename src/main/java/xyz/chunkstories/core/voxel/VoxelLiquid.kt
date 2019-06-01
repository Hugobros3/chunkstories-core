package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.core.entity.blocksWithin

interface VoxelLiquid

fun Entity.isInLiquid() : Boolean = this.blocksWithin().any { it.voxel is VoxelLiquid }
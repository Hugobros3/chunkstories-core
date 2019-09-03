//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.world.cell.EditableCell

class VoxelWater(definition: VoxelDefinition) : Voxel(definition), VoxelLiquid {

	override fun tick(cell: EditableCell) {

	}
}
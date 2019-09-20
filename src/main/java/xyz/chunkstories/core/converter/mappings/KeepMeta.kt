//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings

import xyz.chunkstories.api.converter.mappings.Mapper
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.world.cell.FutureCell

class KeepMeta(voxel: Voxel) : Mapper(voxel) {

    override fun output(minecraftId: Int, minecraftMeta: Byte, cell: FutureCell) {
        cell.voxel = voxel
        cell.metaData = minecraftMeta.toInt()
    }
}

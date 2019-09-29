package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell
import kotlin.math.abs

class BlockFurnace(definition: VoxelDefinition) : Voxel(definition) {
    override fun getVoxelTexture(cell: Cell, side: VoxelSide): VoxelTexture {
        val actualSide = VoxelSide.values()[cell.metaData]//getSideMcStairsChestFurnace(cell.metaData)

        if (side == VoxelSide.TOP)
            return voxelTextures[VoxelSide.TOP.ordinal]
        if (side == VoxelSide.BOTTOM)
            return voxelTextures[VoxelSide.BOTTOM.ordinal]

        return if (side == actualSide) voxelTextures[VoxelSide.FRONT.ordinal] else voxelTextures[VoxelSide.LEFT.ordinal]
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
        val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
                "voxel" to Json.Value.Text(name),
                "class" to Json.Value.Text(ItemFurnace::class.java.canonicalName!!)
        )))

        return listOf(definition)
    }
}

class ItemFurnace(definition: ItemDefinition) : ItemVoxel(definition) {
    override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
        super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)

        val loc = placingEntity.location
        val dx = (cell.x + 0.5) - loc.x()
        val dz = (cell.z + 0.5) - loc.z()

        val facing = if (abs(dx) > abs(dz)) {
            if (dx > 0)
                VoxelSide.LEFT
            else
                VoxelSide.RIGHT
        } else {
            if (dz > 0)
                VoxelSide.BACK
            else
                VoxelSide.FRONT
        }

        cell.metaData = facing.ordinal

        return true
    }
}

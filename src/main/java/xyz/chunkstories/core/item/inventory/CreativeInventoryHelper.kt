package xyz.chunkstories.core.item.inventory

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.InventoryCallbacks
import xyz.chunkstories.api.item.inventory.ItemPile
import java.util.ArrayList

fun Content.Voxels.createCreativeInventory() : Inventory {
    val allItems = ArrayList<ItemVoxel>()

    val voxels = this

    for(voxel in voxels.all()) {

        // Ignore air
        if (voxel.definition.name == "air")
            continue

        allItems.addAll(voxel.enumerateItemsForBuilding())
    }

    val height = Math.ceil(allItems.size / 10.0).toInt()
    val width = 10

    return Inventory(width, height, null, magic).apply {
        for(item in allItems)
            addItem(item, 99)
    }
}

val magic = object : InventoryCallbacks {
    override val inventoryName: String
        get() = "All blocks"

    override fun isAccessibleTo(entity: Entity): Boolean = true

    override fun refreshCompleteInventory() {}

    override fun refreshItemSlot(x: Int, y: Int, pileChanged: ItemPile?) {}
}
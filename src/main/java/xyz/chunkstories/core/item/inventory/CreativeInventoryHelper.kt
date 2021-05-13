//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item.inventory

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.InventoryCallbacks
import xyz.chunkstories.api.item.inventory.ItemPile
import java.util.ArrayList

fun Content.BlockTypes.createCreativeInventory() : Inventory {
	val allItems = ArrayList<ItemBlock>()

	for (blockType in all) {

		// Ignore air
		if (blockType.isAir)
			continue

		allItems.addAll(blockType.enumerateItemsForBuilding())
	}

	val width = Math.ceil(allItems.size / 10.0).toInt()
	val height = 10

	return Inventory(width, height, null, magic).apply {
		for(item in allItems)
			addItem(item, item.definition.maxStackSize)
	}
}

val magic = object : InventoryCallbacks {
	override val inventoryName: String
		get() = "All blocks"

	override fun isAccessibleTo(entity: Entity): Boolean = true

	override fun refreshCompleteInventory() {}

	override fun refreshItemSlot(x: Int, y: Int, pileChanged: ItemPile?) {}
}
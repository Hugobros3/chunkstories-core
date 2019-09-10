//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.util.Specialized
import xyz.chunkstories.core.item.armor.ItemArmor

@Specialized // doesn't take the place of entity inventory
class TraitArmor(holder: Entity, width: Int, height: Int) : TraitInventory(holder, width, height) {
	override val traitName = "armor"

	override val inventoryName: String
		get() = "Armor"

	override fun isItemAccepted(item: Item): Boolean {
		return item is ItemArmor
	}

	fun getDamageMultiplier(bodyPartName: String?): Float {

		var multiplier = 1.0f

		for (itemPile in inventory.contents) {
			val a = itemPile.item as ItemArmor
			val bpa = a.bodyPartsAffected()
			if (bodyPartName == null && bpa == null)
				multiplier *= a.damageMultiplier(bodyPartName)
			else if (bodyPartName != null) {
				if (bpa == null || bpa.contains(bodyPartName))
					multiplier *= a.damageMultiplier(bodyPartName)
			}
			// Of BPN == null & BPA != null, we don't do shit
		}

		return multiplier
	}
}
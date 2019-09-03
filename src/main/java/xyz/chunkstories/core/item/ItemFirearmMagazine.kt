//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition

class ItemFirearmMagazine(definition: ItemDefinition) : Item(definition) {
	private val supportedWeaponsSet = definition["forWeapon"].asArray?.elements?.mapNotNull { it.asString }?.toSet() ?: definition["forWeapon"].asString?.let { setOf(it) } ?: emptySet()

	fun isSuitableFor(item: ItemFirearm): Boolean {
		return supportedWeaponsSet.contains(item.definition.name)
	}

}

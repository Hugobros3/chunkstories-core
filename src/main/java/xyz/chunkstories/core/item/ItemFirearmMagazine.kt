//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition

import java.util.Arrays
import java.util.HashSet

class ItemFirearmMagazine(type: ItemDefinition) : Item(type) {
    private val supportedWeaponsSet = type.resolveProperty("forWeapon", "").split(",").map { it.trim() }

    fun isSuitableFor(item: ItemFirearm): Boolean {
        return supportedWeaponsSet.contains(item.definition.name)
    }

}

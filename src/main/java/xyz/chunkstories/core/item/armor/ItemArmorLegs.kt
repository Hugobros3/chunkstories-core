//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item.armor

import java.util.Arrays

import xyz.chunkstories.api.item.ItemDefinition

class ItemArmorLegs(type: ItemDefinition) : ItemArmor(type) {

    override fun bodyPartsAffected(): Collection<String> {
        return bodyParts
    }

    companion object {
        val bodyParts: Collection<String> = Arrays.asList(*arrayOf("boneLegRU", "boneLegRD", "boneLegLU", "boneLegLD"))
    }

}

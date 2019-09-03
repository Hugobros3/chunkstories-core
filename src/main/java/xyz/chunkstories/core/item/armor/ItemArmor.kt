//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item.armor

import xyz.chunkstories.api.content.json.asFloat
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition

abstract class ItemArmor(definition: ItemDefinition) : Item(definition) {
    private val mutiplier: Float
    val overlayTextureName: String

    init {
        mutiplier = definition["damageMultiplier"].asFloat ?: 0.5f
        overlayTextureName =definition["overlay"].asString ?: "notexture"
    }

    /**
     * Returns either null (and affects the entire holder) or a list of body parts
     * it cares about
     */
    abstract fun bodyPartsAffected(): Collection<String>

    /**
     * Returns the multiplier, optional bodyPartName (might be null, handled that
     * case) for entities that support it
     */
    fun damageMultiplier(bodyPartName: String?): Float {
        return mutiplier
    }

}

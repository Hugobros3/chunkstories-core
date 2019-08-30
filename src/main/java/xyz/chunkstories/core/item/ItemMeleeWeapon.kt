//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.core.entity.MeleeWeapon

class ItemMeleeWeapon(definition: ItemDefinition) : Item(definition), MeleeWeapon {
    override val cooldownMillis: Int
    override val warmupMillis: Int
    override val damage: Float
    override val reach: Double
    override val attackSound: String?

    internal val hitTime: Long

    //internal val itemRenderScale: Float

    init {
        cooldownMillis = Integer.parseInt(definition.resolveProperty("cooldownMillis", "100"))
        warmupMillis = Integer.parseInt(definition.resolveProperty("warmupMillis", "0"))

        reach = java.lang.Double.parseDouble(definition.resolveProperty("reach", "3"))
        damage = java.lang.Float.parseFloat(definition.resolveProperty("damage", "100"))

        attackSound = definition.resolveProperty("attackSound")

        //TODO: Implement delayed hits in TraitMeleeCombat
        hitTime = Integer.parseInt(definition.resolveProperty("hitTime", "100")).toLong()

        //itemRenderScale = java.lang.Float.parseFloat(definition.resolveProperty("itemRenderScale", "2"))
    }
}

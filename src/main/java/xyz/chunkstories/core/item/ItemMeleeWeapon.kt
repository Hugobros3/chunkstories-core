//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.content.json.asDouble
import xyz.chunkstories.api.content.json.asFloat
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.core.entity.MeleeWeapon

class ItemMeleeWeapon(definition: ItemDefinition) : Item(definition), MeleeWeapon {
    override val cooldownMillis: Int
    override val warmupMillis: Int
    override val damage: Float
    override val reach: Double
    override val attackSound: String?

    init {
        cooldownMillis = definition["cooldownMillis"].asInt ?: 100//definition.resolveProperty("cooldownMillis", "100").toDouble().toInt()
        warmupMillis = definition["warmupMillis"].asInt ?: 0//definition.resolveProperty("warmupMillis", "0").toDouble().toInt()

        reach = definition["reach"].asDouble ?: 3.0//java.lang.Double.parseDouble(definition.resolveProperty("reach", "3"))
        damage = definition["damage"].asFloat ?: 100.0f//definition.resolveProperty("damage", "100").toFloat()

        attackSound = definition["attackSound"]?.asString//definition.resolveProperty("attackSound")

        //itemRenderScale = java.lang.Float.parseFloat(definition.resolveProperty("itemRenderScale", "2"))
    }
}

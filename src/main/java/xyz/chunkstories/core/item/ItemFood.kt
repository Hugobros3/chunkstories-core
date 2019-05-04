//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.traits.TraitFoodLevel

class ItemFood(type: ItemDefinition) : Item(type) {

    private val calories: Float = java.lang.Float.parseFloat(type.resolveProperty("calories", "10.0"))

    override fun onControllerInput(entity: Entity, itemPile: ItemPile, input: Input, controller: Controller): Boolean {
        if (entity.world is WorldMaster) {
            if (input.name == "mouse.right") {
                // Any entity with a food level can eat
                entity.traits[TraitFoodLevel::class]?.let {
                    if(it.getValue() > 100)
                        return@let

                    it.setValue(it.getValue() + calories)
                    itemPile.amount--
                }
            }
        }

        return false
    }

}

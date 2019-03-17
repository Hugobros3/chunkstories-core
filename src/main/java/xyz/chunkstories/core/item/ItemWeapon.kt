//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDamageCause
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.item.inventory.InventoryOwner
import xyz.chunkstories.api.item.inventory.ItemPile

open class ItemWeapon(type: ItemDefinition) : Item(type) {

    fun pileAsDamageCause(pile: ItemPile): DamageCause {
        val inventory = pile.inventory

        val holder = inventory.owner
        if (holder is Entity) {

            return object : EntityDamageCause {

                override val name: String
                    get() = this@ItemWeapon.name + " #{weildby} " + holder.toString()

                override val responsibleEntity: Entity
                    get() = holder

            }
        }

        // Damager: this !
        return object: DamageCause {
            override val name: String
                get() = this@ItemWeapon.name
        }
    }

}

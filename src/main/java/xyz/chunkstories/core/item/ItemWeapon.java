//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import xyz.chunkstories.api.entity.DamageCause;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDamageCause;
import xyz.chunkstories.api.item.Item;
import xyz.chunkstories.api.item.ItemDefinition;
import xyz.chunkstories.api.item.inventory.Inventory;
import xyz.chunkstories.api.item.inventory.InventoryHolder;
import xyz.chunkstories.api.item.inventory.ItemPile;

public class ItemWeapon extends Item {
    public ItemWeapon(ItemDefinition type) {
        super(type);
    }

    public DamageCause pileAsDamageCause(ItemPile pile) {
        Inventory inventory = pile.getInventory();
        if (inventory != null) {
            InventoryHolder holder = inventory.getHolder();
            if (holder instanceof Entity) {

                Entity entity = (Entity) holder;
                return new EntityDamageCause() {

                    @Override
                    public String getName() {
                        return ItemWeapon.this.getName() + " #{weildby} " + entity.toString();
                    }

                    @Override
                    public Entity getResponsibleEntity() {
                        return entity;
                    }

                };
            }
        }

        // Damager: this !
        return ItemWeapon.this::getName;
    }

}

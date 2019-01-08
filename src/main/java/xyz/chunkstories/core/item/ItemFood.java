//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.Item;
import xyz.chunkstories.api.item.ItemDefinition;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.core.entity.components.EntityFoodLevel;
import org.jetbrains.annotations.NotNull;

public class ItemFood extends Item {

	private final float calories;

	public ItemFood(ItemDefinition type) {
		super(type);
		calories = Float.parseFloat(type.resolveProperty("calories", "10.0"));
	}

	public boolean onControllerInput(@NotNull Entity entity, @NotNull ItemPile itemPile, @NotNull Input input, @NotNull Controller controller) {
		if (entity.getWorld() instanceof WorldMaster) {
			if (input.getName().equals("mouse.right")) {
				// Any entity with a food level can eat
				if (entity.traits.tryWithBoolean(EntityFoodLevel.class, efl -> {
					if (efl.getValue() >= 100)
						return true;

					System.out.println(entity + " ate " + itemPile);
					efl.setValue(efl.getValue() + calories);
					itemPile.setAmount(itemPile.getAmount() - 1);
					return true;
				}))
					return true;
			}
		}

		return false;
	}

}

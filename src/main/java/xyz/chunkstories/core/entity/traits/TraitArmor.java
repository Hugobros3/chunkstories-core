//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits;

import org.jetbrains.annotations.NotNull;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory;
import xyz.chunkstories.api.item.Item;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.util.Specialized;
import xyz.chunkstories.core.item.armor.ItemArmor;

import java.util.Collection;
import java.util.Iterator;

@Specialized // doesn't take the place of entity inventory
public class TraitArmor extends TraitInventory {

	public TraitArmor(Entity holder, int width, int height) {
		super(holder, width, height);
	}

	@Override public boolean isItemAccepted(@NotNull Item item) {
		return item instanceof ItemArmor;
	}

	@Override
	public String getInventoryName() {
		return "Armor";
	}

	public float getDamageMultiplier(String bodyPartName) {

		float multiplier = 1.0f;

		for (ItemPile itemPile : getInventory().getContents()) {
			ItemArmor a = (ItemArmor) itemPile.getItem();

			Collection<String> bpa = a.bodyPartsAffected();
			if (bodyPartName == null && bpa == null)
				multiplier *= a.damageMultiplier(bodyPartName);
			else if (bodyPartName != null) {
				if (bpa == null || bpa.contains(bodyPartName))
					multiplier *= a.damageMultiplier(bodyPartName);
			}
			// Of BPN == null & BPA != null, we don't do shit
		}

		return multiplier;
	}
}
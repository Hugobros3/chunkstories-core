//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.components;

import java.util.Collection;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.serializable.TraitInventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.util.Specialized;
import io.xol.chunkstories.core.item.armor.ItemArmor;

@Specialized // doesn't take the place of entity inventory
public class EntityArmorInventory extends TraitInventory {

	public EntityArmorInventory(Entity holder, int width, int height) {
		super(holder, width, height);
	}

	@Override
	public boolean canPlaceItemAt(int x, int y, ItemPile itemPile) {
		if (itemPile.getItem() instanceof ItemArmor) {
			return super.canPlaceItemAt(x, y, itemPile);
		}
		return false;
	}

	@Override
	public String getInventoryName() {
		return "Armor";
	}

	public float getDamageMultiplier(String bodyPartName) {

		float multiplier = 1.0f;

		Iterator<ItemPile> i = this.iterator();
		while (i.hasNext()) {
			ItemPile p = i.next();
			ItemArmor a = (ItemArmor) p.getItem();

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
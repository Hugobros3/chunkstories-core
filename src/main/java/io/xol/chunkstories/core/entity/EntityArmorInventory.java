//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import java.util.Collection;
import java.util.Iterator;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponentInventory;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.core.item.armor.ItemArmor;

public class EntityArmorInventory extends EntityComponentInventory {

	public EntityArmorInventory(EntityWithInventory holder, int width, int height)
	{
		super(holder, width, height);

		this.actualInventory = new OnlyArmorEntityInventory(width, height);
	}
	
	class OnlyArmorEntityInventory extends EntityInventory {

		public OnlyArmorEntityInventory(int width, int height)
		{
			super(width, height);
		}
		
		@Override
		public boolean canPlaceItemAt(int x, int y, ItemPile itemPile)
		{
			if(itemPile.getItem() instanceof ItemArmor)
			{
				return super.canPlaceItemAt(x, y, itemPile);
			}
			return false;
		}
		
		@Override
		public String getInventoryName()
		{
			return "Armor";
		}
		
		public float getDamageMultiplier(String bodyPartName) {
			
			float multiplier = 1.0f;
			
			Iterator<ItemPile> i = this.iterator();
			while(i.hasNext())
			{
				ItemPile p = i.next();
				ItemArmor a = (ItemArmor)p.getItem();

				Collection<String> bpa = a.bodyPartsAffected();
				if(bodyPartName == null && bpa == null)
					multiplier *= a.damageMultiplier(bodyPartName);
				else if(bodyPartName != null){
					if(bpa == null || bpa.contains(bodyPartName))
						multiplier *= a.damageMultiplier(bodyPartName);
				}
				//Of BPN == null & BPA != null, we don't do shit
			}
			
			return multiplier;
		}
	}
	
	interface EntityWithArmor extends Entity {
		
		public EntityArmorInventory getArmor();
	}
}
//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.item.ItemDefinition;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.EntityPlayer;

public class ItemFood extends Item {

	private final float calories;

	public ItemFood(ItemDefinition type) {
		super(type);
		calories = Float.parseFloat(type.resolveProperty("calories", "10.0"));
	}
	
	public boolean onControllerInput(Entity owner, ItemPile itemPile, Input input, Controller controller)
	{
		if(owner.getWorld() instanceof WorldMaster)
		{
			if(input.getName().equals("mouse.right") && owner instanceof EntityPlayer)
			{
				if(((EntityPlayer) owner).getFoodLevel() >= 100)
					return true;
				
				System.out.println(owner + " ate "+itemPile);
				
				((EntityPlayer)owner).setFoodLevel(((EntityPlayer) owner).getFoodLevel() + calories);
				itemPile.setAmount(itemPile.getAmount() - 1);
				return true;
			}
		}
		
		return false;
	}

}

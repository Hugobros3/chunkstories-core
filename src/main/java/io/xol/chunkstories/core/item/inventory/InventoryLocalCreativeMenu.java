//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.inventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.item.inventory.BasicInventory;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.World;

/** Creates a big inventory with all the possible building blocks */
public class InventoryLocalCreativeMenu extends BasicInventory
{
	//You can't touch this inventory, only the constructor can
	boolean initialized = false;
	
	public InventoryLocalCreativeMenu(World world)
	{
		super(world, 0, 0);
		List<ItemPile> allItems = new ArrayList<ItemPile>();
		
		Voxels voxels = world.getGameContext().getContent().voxels();
		
		if(voxels != null) {
			Iterator<Voxel> i = voxels.all();
			while(i.hasNext()) {
				Voxel voxel = i.next();
				
				//Ignore air
				if(voxel.getDefinition().getName().equals("air"))
					continue;
				
				for(ItemPile item : voxel.getItems())
					allItems.add(item);
			}
		}
		
		this.height = (int)Math.ceil(allItems.size() / 10.0);
		this.width = 10;
		this.contents = new ItemPile[width][height];
		
		for(ItemPile pile : allItems)
		{
			pile.setAmount(1);
			//pile.setAmount(pile.getItem().getType().getMaxStackSize());
			
			this.addItemPile(pile);
		}
		
		initialized = true;
	}
	
	@Override
	public String getInventoryName()
	{
		return "All voxels";
	}
	
	/*
	 * Following: Hacky stuff to make this inventory immutable
	 */
	
	@Override
	public ItemPile placeItemPileAt(int x, int y, ItemPile itemPile)
	{
		if(initialized)
			return null;
		else
			return super.placeItemPileAt(x, y, itemPile);
	}
	
	@Override
	public ItemPile addItemPile(ItemPile pile)
	{
		if(initialized)
			return null;
		else
			return super.addItemPile(pile);
	}
	
	@Override
	public boolean canPlaceItemAt(int x, int y, ItemPile itemPile)
	{
		if(initialized)
			return true;
		else
			return super.canPlaceItemAt(x, y, itemPile);
	}
	
	@Override
	public boolean setItemPileAt(int x, int y, ItemPile pile)
	{
		if(initialized)
			return true;
		else
			return super.setItemPileAt(x, y, pile);
	}
}

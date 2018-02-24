//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityComponent;
import io.xol.chunkstories.api.exceptions.NullItemException;
import io.xol.chunkstories.api.exceptions.UndefinedItemTypeException;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.serialization.StreamSource;
import io.xol.chunkstories.api.world.serialization.StreamTarget;

public class EntityGroundItemPileComponent extends EntityComponent
{
	ItemPile itemPile = null;

	public EntityGroundItemPileComponent(Entity entity)
	{
		super(entity);
	}

	public EntityGroundItemPileComponent(Entity entity, ItemPile actualItemPile)
	{
		super(entity);
		this.itemPile = actualItemPile;
	}
	
	public ItemPile getItemPile()
	{
		return itemPile;
	}
	
	/**
	 * Warning, setting the ItemPile isn't recommanded behaviour
	 * @param itemPile
	 */
	public void setItemPile(ItemPile itemPile)
	{
		this.itemPile = itemPile;
		if(entity.getWorld() instanceof WorldMaster)
			this.pushComponentEveryone();
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		if (itemPile == null)
			dos.writeInt(0);
		else
			itemPile.saveIntoStream(entity.getWorld().getContentTranslator(), dos);
		
		System.out.println("pushed"+itemPile+".");
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		try
		{
			itemPile = ItemPile.obtainItemPileFromStream(entity.getWorld().getContentTranslator(), dis);
		}
		catch (UndefinedItemTypeException | NullItemException e)
		{
			//Etc
		}
		System.out.println("pulled"+itemPile);
	}

}
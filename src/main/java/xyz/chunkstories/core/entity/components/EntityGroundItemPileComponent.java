//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.components;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable;
import xyz.chunkstories.api.exceptions.NullItemException;
import xyz.chunkstories.api.exceptions.UndefinedItemTypeException;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.api.world.serialization.StreamSource;
import xyz.chunkstories.api.world.serialization.StreamTarget;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EntityGroundItemPileComponent extends TraitSerializable {
	public ItemPile itemPile = null;

	public EntityGroundItemPileComponent(Entity entity) {
		super(entity);
	}

	public EntityGroundItemPileComponent(Entity entity, ItemPile actualItemPile) {
		super(entity);
		this.itemPile = actualItemPile;
	}

	public ItemPile getItemPile() {
		return itemPile;
	}

	/**
	 * Warning, setting the ItemPile isn't recommanded behaviour
	 * 
	 * @param itemPile
	 */
	public void setItemPile(ItemPile itemPile) {
		this.itemPile = itemPile;
		if (entity.getWorld() instanceof WorldMaster)
			this.pushComponentEveryone();
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException {
		if (itemPile == null)
			dos.writeInt(0);
		else
			itemPile.saveIntoStream(entity.getWorld().getContentTranslator(), dos);

		System.out.println("pushed" + itemPile + ".");
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException {
		try {
			itemPile = ItemPile.obtainItemPileFromStream(entity.getWorld().getContentTranslator(), dis);
		} catch (UndefinedItemTypeException | NullItemException e) {
			// Etc
		}
		System.out.println("pulled" + itemPile);
	}

}
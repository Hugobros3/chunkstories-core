package io.xol.chunkstories.core.logic;

import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.item.EventItemDroppedToWorld;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.entity.EntityGroundItem;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ItemsLogic implements Listener {
	private CoreContentPlugin core;
	
	public ItemsLogic(CoreContentPlugin core) {
		this.core = core;
	}

	@EventHandler
	public void onDroppedItem(EventItemDroppedToWorld event) {
		//Create an EntityGroundItem and add it to the event
		EntityGroundItem entity = new EntityGroundItem(core.getPluginExecutionContext().getContent().entities().getEntityTypeByName("groundItem"), event.getLocation(), event.getItemPile());
		event.setItemEntity(entity);
	}
}

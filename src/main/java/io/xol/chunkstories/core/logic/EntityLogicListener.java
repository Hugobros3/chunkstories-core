package io.xol.chunkstories.core.logic;

import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.entity.EntityTeleportEvent;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.entity.traits.TraitTakesFallDamage;

public class EntityLogicListener implements Listener {

	private final CoreContentPlugin core;

	public EntityLogicListener(CoreContentPlugin core) {
		this.core = core;
	}
	
	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent event) {
		//If the entity can take fall damage, reset that
		event.getEntity().traits.with(TraitTakesFallDamage.class, ttfd -> ttfd.resetFallDamage());
	}
}

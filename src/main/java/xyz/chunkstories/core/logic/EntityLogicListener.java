//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.logic;

import xyz.chunkstories.api.events.EventHandler;
import xyz.chunkstories.api.events.Listener;
import xyz.chunkstories.api.events.entity.EntityTeleportEvent;
import xyz.chunkstories.core.CoreContentPlugin;
import xyz.chunkstories.core.entity.traits.TraitTakesFallDamage;

public class EntityLogicListener implements Listener {

	private final CoreContentPlugin core;

	public EntityLogicListener(CoreContentPlugin core) {
		this.core = core;
	}

	@EventHandler
	public void onEntityTeleport(EntityTeleportEvent event) {
		// If the entity can take fall damage, reset that
		event.getEntity().traits.with(TraitTakesFallDamage.class, ttfd -> ttfd.resetFallDamage());
	}
}

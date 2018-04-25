//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityDefinition;
import io.xol.chunkstories.api.entity.components.EntityController;
import io.xol.chunkstories.api.entity.components.EntityRotation;
import io.xol.chunkstories.api.entity.components.EntityVelocity;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.core.entity.components.EntityHealth;
import io.xol.chunkstories.core.entity.traits.TraitBasicMovement;

public abstract class EntityLiving extends Entity {
	protected EntityRotation entityRotation;
	protected EntityVelocity entityVelocity;
	protected EntityHealth entityHealth;

	public EntityLiving(EntityDefinition t, Location location) {
		super(t, location);

		entityVelocity = new EntityVelocity(this);
		entityRotation = new EntityRotation(this);
		entityHealth = new EntityHealth(this);
	}

	@Override
	public void tick() {
		if (getWorld() == null)
			return;

		if (getWorld() instanceof WorldMaster)
			this.entityHealth.removeCorpseAfterDelay();

		// Are we the global authority ?
		boolean has_global_authority = getWorld() instanceof WorldMaster;

		// Does this entity has a controller ?
		Controller controller = this.components.tryWith(EntityController.class, ec -> ec.getController());

		// Are we a client, that controller ?
		boolean owns_entity = (getWorld() instanceof WorldClient) && controller == ((WorldClient) getWorld()).getClient().getPlayer();

		// Servers get to tick all the entities that aren't controlled; the entities
		// that are are updated by their respective clients
		boolean should_tick_movement = owns_entity ? true : has_global_authority && controller == null;

		if(should_tick_movement)
			this.traits.with(TraitBasicMovement.class, tbl -> tbl.tick());
	}
}

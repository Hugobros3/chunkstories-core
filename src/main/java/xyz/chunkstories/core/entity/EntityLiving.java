//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.EntityDefinition;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.WorldClient;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.core.entity.traits.TraitBasicMovement;

public abstract class EntityLiving extends Entity {
	protected TraitRotation entityRotation;
	protected TraitVelocity entityVelocity;
	protected TraitHealth entityHealth;

	public EntityLiving(EntityDefinition t, World world) {
		super(t, world);

		entityVelocity = new TraitVelocity(this);
		entityRotation = new TraitRotation(this);
		entityHealth = new TraitHealth(this);
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
		Controller controller = this.traits.tryWith(TraitControllable.class, TraitControllable::getController);

		// Are we a client, that controller ?
		boolean owns_entity = (getWorld() instanceof WorldClient)
				&& controller == ((WorldClient) getWorld()).getClient().getPlayer();

		// Servers get to tick all the entities that aren't controlled; the entities
		// that are are updated by their respective clients
		boolean should_tick_movement = owns_entity || has_global_authority && controller == null;

		if (should_tick_movement)
			this.traits.with(TraitBasicMovement.class, TraitBasicMovement::tick);
	}
}

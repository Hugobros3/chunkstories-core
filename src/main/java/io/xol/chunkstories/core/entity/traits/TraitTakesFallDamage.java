package io.xol.chunkstories.core.entity.traits;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.components.EntityHealth;
import io.xol.chunkstories.api.entity.traits.Trait;
import io.xol.chunkstories.api.entity.traits.TraitCollidable;

public class TraitTakesFallDamage extends Trait {

	public TraitTakesFallDamage(Entity entity) {
		super(entity);
	}

	protected double lastStandingHeight = Double.NaN;
	protected boolean wasStandingLastTick = true;
	
	public void resetFallDamage() {
		lastStandingHeight = Double.NaN;
	}
	
	public void tick() {
		TraitCollidable collisions = entity.traits.get(TraitCollidable.class);
		if(collisions == null)
			return;
		
		//TODO water & vines cancel that
		
		//Fall damage
		if (collisions.isOnGround()) {
			if (!wasStandingLastTick && !Double.isNaN(lastStandingHeight)) {
				double fallDistance = lastStandingHeight - entity.getLocation().y();
				if (fallDistance > 0) {
					if (fallDistance > 5) {
						float fallDamage = (float) (fallDistance * fallDistance / 2);
						System.out.println(this + "Took " + fallDamage + " hp of fall damage");

						entity.components.with(EntityHealth.class, eh -> eh.damage(DamageCause.DAMAGE_CAUSE_FALL, fallDamage));
					}
				}
			}
			lastStandingHeight = entity.getLocation().y();
		}
		this.wasStandingLastTick = collisions.isOnGround();
	}
}

package xyz.chunkstories.core.entity;

import xyz.chunkstories.api.entity.DamageCause;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.physics.EntityHitbox;
import xyz.chunkstories.api.sound.SoundSource;

class TraitPlayerHealth extends EntityHumanoidHealth {

	public TraitPlayerHealth(Entity entity) {
		super(entity);
	}

	@Override
	public float damage(DamageCause cause, EntityHitbox hitPart, float damage) {
		if (!isDead()) {
			int i = 1 + (int) Math.random() * 3;
			getEntity().world.getSoundManager().playSoundEffect("sounds/entities/human/hurt" + i + ".ogg", SoundSource.Mode.NORMAL,
					getEntity().getLocation(), (float) Math.random() * 0.4f + 0.8f, 5.0f);
		}

		return super.damage(cause, hitPart, damage);
	}
}

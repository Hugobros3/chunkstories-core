package io.xol.chunkstories.core.entity.traits;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.Trait;

public abstract class TraitEyeLevel extends Trait {

	public TraitEyeLevel(Entity entity) {
		super(entity);
	}

	public abstract double getEyeLevel();
}

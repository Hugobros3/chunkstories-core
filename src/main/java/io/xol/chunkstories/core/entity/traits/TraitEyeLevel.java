//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.traits;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.Trait;

public abstract class TraitEyeLevel extends Trait {

	public TraitEyeLevel(Entity entity) {
		super(entity);
	}

	public abstract double getEyeLevel();
}

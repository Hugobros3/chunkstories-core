//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.Trait;

public abstract class TraitEyeLevel extends Trait {

	public TraitEyeLevel(Entity entity) {
		super(entity);
	}

	public abstract double getEyeLevel();
}

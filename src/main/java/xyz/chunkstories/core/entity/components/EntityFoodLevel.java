//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.components;

import xyz.chunkstories.api.entity.DamageCause;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.generic.TraitSerializableFloat;

public class EntityFoodLevel extends TraitSerializableFloat {
	public EntityFoodLevel(Entity entity, float defaultValue) {
		super(entity, defaultValue);
	}

	public static DamageCause HUNGER_DAMAGE_CAUSE = new DamageCause() {

		@Override
		public String getName() {
			return "Hunger";
		}

	};
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.entity.components;

import io.xol.chunkstories.api.entity.DamageCause;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.generic.TraitSerializableFloat;

public class EntityFoodLevel extends TraitSerializableFloat
{
	public EntityFoodLevel(Entity entity, float defaultValue)
	{
		super(entity, defaultValue);
	}

	public static DamageCause HUNGER_DAMAGE_CAUSE = new DamageCause() {

		@Override
		public String getName()
		{
			return "Hunger";
		}
		
	};
}

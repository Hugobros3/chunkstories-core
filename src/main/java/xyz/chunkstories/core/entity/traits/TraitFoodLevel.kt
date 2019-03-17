//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.generic.TraitSerializableFloat

class TraitFoodLevel(entity: Entity, defaultValue: Float) : TraitSerializableFloat(entity, defaultValue) {
    companion object {

        var HUNGER_DAMAGE_CAUSE: DamageCause = object : DamageCause {

            override val name: String
                get() = "Hunger"

        }
    }
}

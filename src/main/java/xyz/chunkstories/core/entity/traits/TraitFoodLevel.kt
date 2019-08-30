//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.generic.TraitSerializableFloat
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.world.WorldMaster

class TraitFoodLevel(entity: Entity, defaultValue: Float) : TraitSerializableFloat(entity, defaultValue) {
    companion object {

        var HUNGER_DAMAGE_CAUSE: DamageCause = object : DamageCause {

            override val name: String
                get() = "Hunger"

        }
    }

    override fun tick() {
        val world = entity.world as? WorldMaster ?: return
        val traitHealth = entity.traits[TraitHealth::class] ?: throw Exception("TraitFoodLevel requires TraitHealth")
        val traitVelocity = entity.traits[TraitVelocity::class]?: throw Exception("TraitFoodLevel requires TraitVelocity")

        // Take damage when starving
        if (world.ticksElapsed % 100L == 0L) {
            if (getValue() == 0f)
                traitHealth.damage(HUNGER_DAMAGE_CAUSE, 1f)
            else {
                // 27 minutes to start starving at 0.1 starveFactor
                // Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

                // Starve slowly if inactive
                var starve = 0.03f

                // Walking drains you
                if (traitVelocity.velocity.length() > 0.3) {
                    starve = 0.06f
                    // Running is even worse
                    if (traitVelocity.velocity.length() > 0.7)
                        starve = 0.15f
                }

                val newfoodLevel = getValue() - starve
                setValue(newfoodLevel)
            }
        }

        // Having some food energy allows to restore HP, but also makes the entity go hungry
        if (getValue() > 20 && !traitHealth.isDead) {
            if (traitHealth.getHealth() < traitHealth.maxHealth) {
                traitHealth.setHealth(traitHealth.getHealth() + 0.01f)

                val newfoodLevel = getValue() - 0.01f
                setValue(newfoodLevel)
            }
        }
    }
}

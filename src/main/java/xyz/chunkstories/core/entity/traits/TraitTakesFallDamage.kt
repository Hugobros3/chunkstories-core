//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth

class TraitTakesFallDamage(entity: Entity) : Trait(entity) {

    private var lastStandingHeight = java.lang.Double.NaN
    private var wasStandingLastTick = true

    fun resetFallDamage() {
        lastStandingHeight = java.lang.Double.NaN
    }

    fun tick() {
        val collisions = entity.traits[TraitCollidable::class.java] ?: return

        // TODO water & vines cancel that

        // Fall damage
        if (collisions.isOnGround) {
            if (!wasStandingLastTick && !java.lang.Double.isNaN(lastStandingHeight)) {
                val fallDistance = lastStandingHeight - entity.location.y()
                if (fallDistance > 0) {
                    if (fallDistance > 5) {
                        val fallDamage = (fallDistance * fallDistance / 2).toFloat()
                        //println(this.toString() + "Took " + fallDamage + " hp of fall damage")
                        entity.traits[TraitHealth::class]?.damage(DamageCause.DAMAGE_CAUSE_FALL, fallDamage)
                    }
                }
            }
            lastStandingHeight = entity.location.y()
        }
        this.wasStandingLastTick = collisions.isOnGround
    }
}

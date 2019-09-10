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
import xyz.chunkstories.api.entity.traits.serializable.TraitFlyingMode
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.voxel.isInLiquid
import xyz.chunkstories.core.voxel.isOnLadder

class TraitTakesFallDamage(entity: Entity) : Trait(entity) {
	override val traitName = "fallDamage"

	private var lastStandingHeight = java.lang.Double.NaN
	private var wasStandingLastTick = true

	fun resetFallDamage() {
		lastStandingHeight = java.lang.Double.NaN
	}

	override fun tick() {
		if (entity.world is WorldMaster) {
			// Ladders, water and flying allows to bypass fall damage
			if (entity.isOnLadder() || entity.isInLiquid() || entity.traits[TraitFlyingMode::class]?.let { it.isAllowed && it.isFlying } == true)
				resetFallDamage()

			val collisions = entity.traits[TraitCollidable::class.java] ?: return
			if (collisions.isOnGround) {
				if (!wasStandingLastTick && !java.lang.Double.isNaN(lastStandingHeight)) {
					val fallDistance = lastStandingHeight - entity.location.y()
						if (fallDistance > 5) {
							val fallDamage = (fallDistance * fallDistance / 2).toFloat()
							entity.traits[TraitHealth::class]?.damage(DamageCause.DAMAGE_CAUSE_FALL, fallDamage)
						}

				}
				lastStandingHeight = entity.location.y()
			}
			this.wasStandingLastTick = collisions.isOnGround
		}
	}
}

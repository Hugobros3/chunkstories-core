//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.world.getCell
import xyz.chunkstories.core.entity.traits.TraitBasicMovement
import kotlin.math.sqrt

open class AiTaskGoAtEntity<E: Entity>(ai: AI<E>, val targetEntity: Entity, var maxDistance: Float, var previousTask: AiTask<E>, var entitySpeed: Double = 0.02) : AiTask<E>(ai) {
	override fun execute() {

		val delta = targetEntity.location.sub(entity.location)

		// When entity is too far (or dead), give up
		if (targetEntity.traits[TraitHealth::class.java]?.isDead == true || targetEntity.location.distance(entity.location) > maxDistance) {
			ai.currentTask = previousTask
			return
		}

		if(delta.length() == 0.0) {
			return
		}

		//makeEntityLookAt(entity, Vector3d(delta))
		entity.lookAt(delta)

		delta.y = 0.0

		delta.normalize().mul(entitySpeed)

		entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.x = delta.x()
		entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.z = delta.z()

		if (entity.traits[TraitCollidable::class.java]!!.isOnGround) {
			val rem = entity.traits[TraitCollidable::class.java]!!.canMoveWithCollisionRestrain(entity.traits[TraitBasicMovement::class.java]!!.targetVelocity)

			if (sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001) {
				// If they have their feet in water
				if (entity.world.getCell(entity.location.add(0.0, 0.0, 0.0))?.data?.blockType?.liquid == true) {
					entity.traits[TraitVelocity::class.java]!!.addVelocity(0.0, 0.20, 0.0)
				} else
					entity.traits[TraitVelocity::class.java]!!.addVelocity(0.0, 0.15, 0.0)
			}
		}
	}
}
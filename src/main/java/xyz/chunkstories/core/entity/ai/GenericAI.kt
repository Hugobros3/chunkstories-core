//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai

import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.core.entity.EntityHumanoid
import xyz.chunkstories.core.entity.traits.TraitBasicMovement
import org.joml.Vector2f
import org.joml.Vector3d
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.world.getCell

import java.util.Random
import kotlin.math.sqrt

//TODO refactor properly for components & traits
abstract class GenericAI<E: Entity>(entity: E) : AI<E>(entity) {

	val movementTrait: TraitBasicMovement
		get() = entity.traits[TraitBasicMovement::class]!!

	val velocityTrait: TraitVelocity
		get() = entity.traits[TraitVelocity::class]!!

	internal var counter: Long = 0

	init {
		currentTask = AiTaskLookArround(this, 5.0)
	}

	abstract fun bark()

	override fun tick() {
		if (entity.traits[TraitHealth::class]?.isDead == true) {
			// Dead entities shouldn't be moving
			movementTrait.targetVelocity.x = 0.0
			movementTrait.targetVelocity.z = 0.0
			return
		}

		counter++

		currentTask.execute()

		// Random bark
		if (rng.nextFloat() > 0.9990) {
			bark()
		}

		// Jump when in water
		if (entity.world.getCell(entity.location.add(0.0, 1.15, 0.0))?.data?.blockType?.liquid == true) {
			if (velocityTrait.velocity.y() < 0.0)
				velocityTrait.addVelocity(0.0, 0.10, 0.0)
		}

	}

	companion object {
		internal var rng = Random()
	}
}

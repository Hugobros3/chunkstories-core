//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth

class AiTaskAttackEntity<E>(ai: AI<E>, previousTask: AiTask<E>, target: Entity, internal val giveupDistance: Float,
							initGiveupDistance: Float, internal val attackCooldownMS: Int, internal val damage: Float) : AiTaskGoAtEntity<E>(ai, target, initGiveupDistance, previousTask)
		where E : Entity, E: DamageCause {

	internal var lastAttackMS: Long = 0

	override fun execute() {
		super.execute()

		val distance = this.targetEntity.location.distance(entity.location).toFloat()

		// Within the final give up distance ? Set the give up distance to be at that from then on
		if (giveupDistance - distance > 1) {
			this.maxDistance = giveupDistance
		}

		if (distance < 1.5) {
			if (System.currentTimeMillis() - lastAttackMS > attackCooldownMS) {
				// Attack, but make sure the target can be hurt !
				targetEntity.traits[TraitHealth::class]?.damage(entity, damage)
				lastAttackMS = System.currentTimeMillis()
			}
		}
	}
}
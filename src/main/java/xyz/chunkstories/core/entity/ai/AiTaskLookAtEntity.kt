package xyz.chunkstories.core.entity.ai

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.core.entity.EntityHumanoid
import xyz.chunkstories.core.entity.traits.TraitBasicMovement

class AiTaskLookAtEntity<E: Entity>(ai: AI<E>, val targetEntity: EntityHumanoid, var maxDistance: Float, val previousTask: AiTask<E>) : AiTask<E>(ai) {
    var timeBeforeDoingSomethingElse: Int = 0

    init {
        this.timeBeforeDoingSomethingElse = (60.0 * Math.random() * 30.0).toInt()
    }

    override fun execute() {
        timeBeforeDoingSomethingElse--

        if (timeBeforeDoingSomethingElse <= 0 || targetEntity.traits[TraitHealth::class.java]!!.isDead) {
            ai.currentTask = (previousTask)
            return
        }

        if (targetEntity.location.distance(entity.location) > maxDistance) {
            // System.out.println("too
            // far"+entityFollowed.getLocation().distanceTo(entity.getLocation()));
            ai.currentTask = (previousTask)
            return
        }

        val delta = entity.location.sub(targetEntity.location)
        entity.lookAt(delta)
        //makeEntityLookAt(entity, delta)

        entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.x = 0.0
        entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.z = 0.0
        // entity.traits.get(TraitVelocity.class).setVelocityX(0);
        // entity.traits.get(TraitVelocity.class).setVelocityZ(0);
    }

}
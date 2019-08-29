package xyz.chunkstories.core.entity.ai

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.core.entity.traits.TraitBasicMovement

class AiTaskGoSomewhere<E : Entity>(ai: AI<E>, internal var location: Location, internal var timeOut: Int = -1) : AiTask<E>(ai) {

    override fun execute() {
        if (timeOut > 0)
            timeOut--

        if (timeOut == 0) {
            ai.currentTask = (AiTaskLookArround(ai, 5.0))
            return
        }

        val delta = Vector3d(location).sub(entity.location)

        if (delta.length() < 0.25) {
            ai.currentTask = (AiTaskLookArround(ai, 5.0))
            return
        }

        //makeEntityLookAt(entity, Vector3d(delta))
        entity.lookAt(delta)

        delta.y = 0.0

        val entitySpeed = 0.02

        delta.normalize().mul(entitySpeed)

        entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.x = delta.x()
        entity.traits[TraitBasicMovement::class.java]!!.targetVelocity.z = delta.z()

        // entity.traits.get(TraitVelocity.class).setVelocityX(delta.getX());
        // entity.traits.get(TraitVelocity.class).setVelocityZ(delta.getZ());

        if (entity.traits[TraitCollidable::class.java]!!.isOnGround) {
            val rem = entity.traits[TraitCollidable::class.java]!!
                    .canMoveWithCollisionRestrain(entity.traits[TraitBasicMovement::class.java]!!.targetVelocity)
            // rem.setY(0.0D);
            if (Math.sqrt(rem.x() * rem.x() + rem.z() * rem.z()) > 0.001)
            // if(rem.length() > 0.001)
                entity.traits[TraitVelocity::class.java]!!.addVelocity(0.0, 0.15, 0.0)
        }
    }

}
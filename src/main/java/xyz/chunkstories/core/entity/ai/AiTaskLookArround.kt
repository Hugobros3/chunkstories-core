package xyz.chunkstories.core.entity.ai

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.ai.AI
import xyz.chunkstories.api.entity.ai.AiTask
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.core.entity.EntityHumanoid
import xyz.chunkstories.core.entity.traits.TraitBasicMovement

class AiTaskLookArround<E : Entity>(ai: AI<E>, var lookAtNearbyEntities: Double) : AiTask<E>(ai) {
    var targetH = 0.0
    var targetV = 0.0
    var lookAtEntityCoolDown = 60 * 5

    override fun execute() {
        // if(entity.traits.get(TraitRotation.class).getHorizontalRotation() ==
        // Float.NaN)
        // entity.traits.get(TraitRotation.class).setRotation(0.0, 0.0);

        if (Math.random() > 0.990) {
            targetH = entity.traits[TraitRotation::class.java]!!.horizontalRotation + (Math.random() * 2.0 - 1.0) * 30f

            if (Math.random() > 0.5)
                targetV = targetV / 2.0f + (Math.random() * 2.0 - 1.0) * 20f

            if (targetV > 90f)
                targetV = 90.0
            if (targetV < -90f)
                targetV = -90.0
        }

        val diffH = targetH - entity.traits[TraitRotation::class.java]!!.horizontalRotation
        val diffV = targetV - entity.traits[TraitRotation::class.java]!!.verticalRotation

        entity.traits[TraitRotation::class.java]!!.addRotation(diffH / 15f, diffV / 15f)

        if (lookAtEntityCoolDown > 0)
            lookAtEntityCoolDown--

        if (lookAtNearbyEntities > 0.0 && lookAtEntityCoolDown == 0) {
            for (entityToLook in entity.world.getEntitiesInBox(Box.Companion.fromExtentsCentered(Vector3d(lookAtNearbyEntities)).translate(entity.location))) {
                if (entityToLook != entity
                        && entityToLook.location
                                .distance(entity.location) <= lookAtNearbyEntities
                        && entityToLook is EntityHumanoid
                        && !entityToLook.traits[TraitHealth::class.java]!!.isDead) {
                    ai.currentTask = AiTaskLookAtEntity(ai, entityToLook, 10f, this)
                    lookAtEntityCoolDown = (Math.random() * 60.0 * 5.0).toInt()
                    return
                }
            }

            lookAtEntityCoolDown = (Math.random() * 60).toInt()
        }

        if (Math.random() > 0.9990) {
            ai.currentTask = AiTaskGoSomewhere(ai,
                    Location(entity.world, entity.location
                            .add((Math.random() * 2.0 - 1.0) * 10, 0.0, (Math.random() * 2.0 - 1.0) * 10)),
                    505)
            return
        }

        val movementTrait = entity.traits[TraitBasicMovement::class]!!
        movementTrait.targetVelocity.x = 0.0
        movementTrait.targetVelocity.z = 0.0
    }
}
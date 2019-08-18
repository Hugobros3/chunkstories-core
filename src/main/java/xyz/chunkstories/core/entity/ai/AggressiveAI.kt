//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.ai

import org.joml.Vector3d
import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.core.entity.EntityHumanoid
import xyz.chunkstories.core.entity.EntityPlayer
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance.HumanoidStance

abstract class AggressiveAI<E>(entity: E, private val targetsTypes: Collection<Class<out Entity>>)
    : GenericAI<E>(entity) where E : Entity, E : DamageCause {

    open var attackEntityCooldown = 60 * 5

    abstract val aggroRadius: Double

    abstract fun aggroBark()

    open fun pickTarget(): Entity? {
        for (entityToLook in entity.world.getEntitiesInBox(Box.Companion.fromExtentsCentered(Vector3d(aggroRadius * 2f)).translate(entity.location))) {
            var visibilityModifier = 1f
            if (entityToLook is EntityPlayer) {

                // Crouched players are 70% less visible
                if (entityToLook.traits[TraitHumanoidStance::class.java]!!.stance == HumanoidStance.CROUCHING)
                    visibilityModifier -= 0.7f

                // If the entity is sprinting, it's 100% more obvious
                if (entityToLook.traits[TraitVelocity::class.java]!!.velocity.length() > 0.7)
                    visibilityModifier += 1.0f
            }

            if (entityToLook != entity
                    && entityToLook.location.distance(entity.location) * visibilityModifier <= aggroRadius
                    && entityToLook is EntityHumanoid
                    && !entityToLook.traits[TraitHealth::class.java]!!.isDead) {
                // Check target is in set
                if (targetsTypes.contains(entityToLook.javaClass)) {
                    aggroBark()

                    return entity
                }
            }
        }

        return null
    }

    abstract fun attack(target: Entity)

    override fun tick() {
        super.tick()

        if (entity.traits[TraitHealth::class.java]!!.isDead)
            return

        if (attackEntityCooldown > 0)
            attackEntityCooldown--

        // Find entities to attack
        if (this.currentTask !is AiTaskAttackEntity && attackEntityCooldown == 0) {
            // Only look for them once in 2s
            attackEntityCooldown = (Math.random() * 60.0 * 2.0).toInt()

            val newTarget = pickTarget()
            if(newTarget != null) {
                attack(newTarget)
            }
        }
    }

}

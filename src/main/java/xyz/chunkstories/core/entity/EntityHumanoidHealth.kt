package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.sound.SoundSource

/**
 * Extends the original entity health component to add in support for damage
 * multipliers
 */
internal open class EntityHumanoidHealth(entity: Entity) : TraitHealth(entity) {

    override fun damage(cause: DamageCause, hitPart: EntityHitbox?, damage: Float): Float {
        var damage = damage
        if (hitPart != null) {
            if (hitPart.name == "boneHead")
                damage *= 2.8f
            else if (hitPart.name.contains("Arm"))
                damage *= 0.75f
            else if (hitPart.name.contains("Leg"))
                damage *= 0.5f
            else if (hitPart.name.contains("Foot"))
                damage *= 0.25f
        }

        damage *= 0.5f

        entity.world.soundManager
                .playSoundEffect("sounds/entities/flesh.ogg", SoundSource.Mode.NORMAL, entity.location, Math.random().toFloat() * 0.4f + 0.4f, 1f)

        return super.damage(cause, null, damage)
    }
}

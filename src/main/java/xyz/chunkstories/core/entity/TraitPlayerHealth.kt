package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.sound.SoundSource

internal class TraitPlayerHealth(entity: Entity) : EntityHumanoidHealth(entity) {

    override fun damage(cause: DamageCause, hitPart: EntityHitbox?, damage: Float): Float {
        if (!isDead) {
            val i = 1 + Math.random().toInt() * 3
            entity.world.soundManager.playSoundEffect("sounds/entities/human/hurt$i.ogg", SoundSource.Mode.NORMAL,
                    entity.location, Math.random().toFloat() * 0.4f + 0.8f, 5.0f)
        }

        return super.damage(cause, hitPart, damage)
    }
}

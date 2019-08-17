package xyz.chunkstories.core.entity.zombie

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.core.entity.EntityPlayer
import xyz.chunkstories.core.entity.ai.AggressiveAI
import xyz.chunkstories.core.entity.ai.AiTaskAttackEntity

class ZombieAI(entity: EntityZombie) : AggressiveAI<EntityZombie>(entity, targets) {
    override fun bark() {
        entity.world.soundManager.playSoundEffect("sounds/entities/zombie/grunt.ogg",
                SoundSource.Mode.NORMAL, entity.location, (0.9 + Math.random() * 0.2).toFloat(), 1.0f)
    }

    override fun aggroBark() {
        entity.world.soundManager.playSoundEffect("sounds/entities/zombie/grunt.ogg",
                SoundSource.Mode.NORMAL, entity.location, (1.5 + Math.random() * 0.2).toFloat(), 1.5f)
    }

    override fun attack(target: Entity) {
        currentTask = AiTaskAttackEntity(this, currentTask, target, 10f, 15f, entity.stage().attackCooldown, entity.stage().attackDamage)
    }

    override val aggroRadius: Double
        get() = entity.stage().aggroRadius

    companion object {
        val targets = setOf(EntityPlayer::class.java)
    }
}
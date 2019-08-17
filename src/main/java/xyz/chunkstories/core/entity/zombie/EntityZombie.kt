//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.zombie

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.*
import xyz.chunkstories.core.entity.ai.AiTaskAttackEntity

class EntityZombie @JvmOverloads constructor(t: EntityDefinition, world: World, stage: ZombieInfectionStage = ZombieInfectionStage.values()[Math.floor(Math.random() * ZombieInfectionStage.values().size).toInt()]) : EntityHumanoid(t, world), DamageCause {
    internal val ai: ZombieAI = ZombieAI(this)

    internal val stageComponent: TraitZombieInfectionStage = TraitZombieInfectionStage(this, stage)

    override val name: String
        get() = "Zombie"

    override val cooldownInMs: Long
        get() = 1500

    init {

        this.traitHealth = object : TraitHealth(this) {

            override fun damage(cause: DamageCause, hitLocation: EntityHitbox?, damage: Float): Float {
                if (!this.isDead)
                    this@EntityZombie.world.soundManager
                            .playSoundEffect("sounds/entities/zombie/hurt.ogg", Mode.NORMAL, location, Math.random().toFloat() * 0.4f + 0.8f,
                                    1.5f + Math.min(0.5f, damage / 15.0f))

                // Aggro anyone who hits the zombie
                if (cause is EntityLiving) {
                    val aggressor = cause as EntityLiving
                    ai.currentTask = AiTaskAttackEntity(ai, ai.currentTask, aggressor, 15f, 20f, stage().attackCooldown, stage().attackDamage)
                }

                return super.damage(cause, hitLocation, damage)
            }
        }
        this.traitHealth.setHealth(stage.hp)

        ZombieRenderer(this)
    }

    override fun tick() {
        // AI works on master
        if (world is WorldMaster)
            ai.tick()

        // Ticks the entity
        super.tick()

        // Anti-glitch
        if (java.lang.Double.isNaN(traitRotation.horizontalRotation.toDouble())) {
            println("nan !$this")
            traitRotation.setRotation(0.0, 0.0)
        }
    }

    fun stage(): ZombieInfectionStage {
        return stageComponent.stage
    }

    fun attack(target: EntityLiving, maxDistance: Float) {
        this.ai.currentTask = AiTaskAttackEntity(ai, ai.currentTask, target, 15f, maxDistance, stage().attackCooldown, stage().attackDamage)
    }
}
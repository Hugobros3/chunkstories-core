//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.ai.ZombieAI

import java.util.HashSet

class EntityZombie @JvmOverloads constructor(t: EntityDefinition, world: World, stage: ZombieInfectionStage = ZombieInfectionStage.values()[Math.floor(Math.random() * ZombieInfectionStage.values().size).toInt()]) : EntityHumanoid(t, world), DamageCause {
    internal var zombieAi: ZombieAI

    internal val stageComponent: TraitZombieInfectionStage

    override val name: String
        get() = "Zombie"

    override val cooldownInMs: Long
        get() = 1500

    init {
        zombieAi = ZombieAI(this, zombieTargets)

        this.stageComponent = TraitZombieInfectionStage(this, stage)

        this.traitHealth = object : TraitHealth(this) {

            override fun damage(cause: DamageCause, osef: EntityHitbox?, damage: Float): Float {
                if (!this.isDead)
                    this@EntityZombie.world.soundManager
                            .playSoundEffect("sounds/entities/zombie/hurt.ogg", Mode.NORMAL, location, Math.random().toFloat() * 0.4f + 0.8f,
                                    1.5f + Math.min(0.5f, damage / 15.0f))

                if (cause is EntityLiving) {
                    val entity = cause as EntityLiving
                    zombieAi.setAiTask(
                            zombieAi.AiTaskAttackEntity(entity, 15f, 20f, zombieAi.currentTask(), stage().attackCooldown.toLong(), stage().attackDamage))
                }

                return super.damage(cause, osef, damage)
            }
        }
        this.traitHealth.setHealth(stage.hp)

        ZombieRenderer(this)
        //new TraitRenderable(this, EntityZombieRenderer::new);
    }

    override fun tick() {
        // AI works on master
        if (world is WorldMaster)
            zombieAi.tick()

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
        this.zombieAi
                .setAiTask(zombieAi.AiTaskAttackEntity(target, 15f, maxDistance, zombieAi.currentTask(), stage().attackCooldown.toLong(),
                        stage().attackDamage))
    }

    companion object {

        internal var zombieTargets: MutableSet<Class<out Entity>> = HashSet()

        init {
            zombieTargets.add(EntityPlayer::class.java)
        }
    }
}

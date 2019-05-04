//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.animation.Animator
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.TraitAnimated
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.TraitHitboxes
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance.HumanoidStance
import xyz.chunkstories.core.entity.traits.TraitBasicMovement
import xyz.chunkstories.core.entity.traits.TraitWalkingSounds

abstract class EntityHumanoid(t: EntityDefinition, world: World) : EntityLiving(t, world) {
    var traitHitboxes: TraitHitboxes
    var traitAnimation: TraitAnimated
    val traitStance: TraitHumanoidStance

    init {
        /** A bunch of boxes that follow the bones of the model during animations */
        val hitboxes = arrayOf(
                EntityHitbox(this, Box(-0.15, 0.0, -0.25, 0.30, 0.675, 0.5), "boneTorso"),
                EntityHitbox(this, Box(-0.25, 0.0, -0.25, 0.5, 0.5, 0.5), "boneHead"),
                EntityHitbox(this, Box(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmRU"),
                EntityHitbox(this, Box(-0.1, -0.375, -0.1, 0.2, 0.375, 0.2), "boneArmLU"),
                EntityHitbox(this, Box(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmRD"),
                EntityHitbox(this, Box(-0.1, -0.3, -0.1, 0.2, 0.3, 0.2), "boneArmLD"),
                EntityHitbox(this, Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRU"),
                EntityHitbox(this, Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLU"),
                EntityHitbox(this, Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegRD"),
                EntityHitbox(this, Box(-0.15, -0.375, -0.125, 0.3, 0.375, 0.25), "boneLegLD"),
                EntityHitbox(this, Box(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootL"),
                EntityHitbox(this, Box(-0.15, -0.075, -0.125, 0.35, 0.075, 0.25), "boneFootR"))

        /** Humanoids can have different stances */
        this.traitStance = TraitHumanoidStance(this)

        this.traitAnimation = object : TraitAnimated(this) {
            override var animatedSkeleton: Animator = HumanoidSkeletonAnimator(this@EntityHumanoid)
                internal set
        }

        this.traitHitboxes = object : TraitHitboxes(this) {
            override val hitBoxes: Array<EntityHitbox>
                get() = hitboxes
        }

        TraitBasicMovement(this)

        // Override the entityliving's health component with a modified version
        this.traitHealth = EntityHumanoidHealth(this)

        object : TraitCollidable(this@EntityHumanoid) {

            override val collisionBoxes: Array<Box>
                get() {
                    var height = if (traitStance.stance === HumanoidStance.CROUCHING) 1.45 else 1.9
                    if (this@EntityHumanoid.traitHealth.isDead)
                        height = 0.2
                    return arrayOf(Box(0.6, height, 0.6).translate(-0.3, 0.0, -0.3))
                }

        }

        TraitWalkingSounds(this)
    }

    override fun tick() {
        // Tick : will move the entity, solve velocity/acceleration and so on
        super.tick()

        this.traits[TraitWalkingSounds::class]?.handleWalkingEtcSounds()
    }

    override fun getBoundingBox(): Box {
        return if (traitHealth.isDead) Box(1.6, 1.0, 1.6).translate(-0.8, 0.0, -0.8) else Box(1.0, if (traitStance.stance === HumanoidStance.CROUCHING) 1.5 else 2.0, 1.0).translate(-0.5, 0.0, -0.5)

    }

}

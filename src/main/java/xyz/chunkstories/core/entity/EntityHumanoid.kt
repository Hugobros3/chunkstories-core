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

/** Helper function to make the bounding box start from 0 to negative Y,
 * to match the skeleton bones where this is the case
 */
private fun Box.rootOnTop(): Box {
    this.translate(0.0, -this.min.y, 0.0)
    return this
}

abstract class EntityHumanoid(t: EntityDefinition, world: World) : EntityLiving(t, world) {
    var traitHitboxes: TraitHitboxes
    var traitAnimation: TraitAnimated
    val traitStance: TraitHumanoidStance

    init {

        /** A bunch of boxes that follow the bones of the model during animations */
        val hitboxes = arrayOf(
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.30, 0.675, 0.5), "boneTorso"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.5, 0.5, 0.5), "boneHead"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.2, 0.375, 0.2).rootOnTop(), "boneArmRU"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.2, 0.375, 0.2).rootOnTop(), "boneArmLU"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.2, 0.3, 0.2).rootOnTop(), "boneArmRD"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.2, 0.3, 0.2).rootOnTop(), "boneArmLD"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.3, 0.375, 0.25).rootOnTop(), "boneLegRU"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.3, 0.375, 0.25).rootOnTop(), "boneLegLU"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.3, 0.375, 0.25).rootOnTop(), "boneLegRD"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.3, 0.375, 0.25).rootOnTop(), "boneLegLD"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.35, 0.075, 0.25).rootOnTop(), "boneFootL"),
                EntityHitbox(this, Box.fromExtentsCenteredHorizontal(0.35, 0.075, 0.25).rootOnTop(), "boneFootR"))

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
                    return arrayOf(Box.fromExtentsCenteredHorizontal(0.6, height, 0.6))
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
        return if (traitHealth.isDead) Box.fromExtentsCenteredHorizontal(1.6, 1.0, 1.6) else Box.fromExtentsCenteredHorizontal(1.0, if (traitStance.stance === HumanoidStance.CROUCHING) 1.5 else 2.0, 1.0)
    }

}

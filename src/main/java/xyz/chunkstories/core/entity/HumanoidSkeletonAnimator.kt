//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.animation.Animation
import xyz.chunkstories.api.animation.CompoundAnimationHelper
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance.HumanoidStance
import xyz.chunkstories.core.entity.traits.TraitMining
import xyz.chunkstories.core.item.ItemMiningTool
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

import kotlin.math.sqrt

internal val bonesInfluencedByItem = listOf("boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand")

class HumanoidSkeletonAnimator(internal val entity: Entity) : CompoundAnimationHelper() {
    private val entityHealth: TraitHealth = entity.traits[TraitHealth::class.java]!!
    private val stance: TraitHumanoidStance = entity.traits[TraitHumanoidStance::class.java]!!
    private val entityRotation: TraitRotation = entity.traits[TraitRotation::class.java]!!
    private val entityVelocity: TraitVelocity = entity.traits[TraitVelocity::class.java]!!

    override fun getAnimationPlayingForBone(boneName: String, animationTime: Float): Animation {
        if (entityHealth.isDead)
            return entity.world.gameContext.content.animationsLibrary.getAnimation("./animations/human/ded.bvh")

        val world = entity.world

        if (bonesInfluencedByItem.contains(boneName)) {
            val r =
                run {
                    val selectedItemPile = entity.traits[TraitSelectedItem::class]?.selectedItem

                if (selectedItemPile != null) {
                    // TODO refactor BVH subsystem to enable SkeletonAnimator to also take care of additional transforms
                    val item = selectedItemPile.item

                    if (item is ItemMiningTool) {
                        val trait = entity.traits[TraitMining::class.java]
                        if (trait != null) {
                            if (trait.progress != null)
                                return@run world.content.animationsLibrary.getAnimation("./animations/human/mining.bvh")
                        }
                    }

                    //TODO rework
                    //if (item is ItemCustomHoldingAnimation)
                    //    return@run world.content.animationsLibrary.getAnimation(item.customAnimationName)
                    //else
                    return@run world.content.animationsLibrary.getAnimation("./animations/human/holding-item.bvh")
                }
                null
            }

            if (r != null)
                return r
        }

        val vel = Vector3d(entityVelocity.velocity)

        // Extract just the horizontal speed from that
        val horizSpd = sqrt(vel.x() * vel.x() + vel.z() * vel.z())

        if (stance.stance === HumanoidStance.STANDING) {
            if (horizSpd > 0.065) {
                // System.out.println("running");
                return world.gameContext.content.animationsLibrary.getAnimation("./animations/human/running.bvh")
            }
            return if (horizSpd > 0.0) world.gameContext.content.animationsLibrary.getAnimation("./animations/human/walking.bvh") else world.gameContext.content.animationsLibrary.getAnimation("./animations/human/standstill.bvh")

        } else return if (stance.stance === HumanoidStance.CROUCHING) {
            if (horizSpd > 0.0) world.gameContext.content.animationsLibrary.getAnimation("./animations/human/crouched-walking.bvh") else world.gameContext.content.animationsLibrary.getAnimation("./animations/human/crouched.bvh")

        } else {
            world.gameContext.content.animationsLibrary.getAnimation("./animations/human/ded.bvh")
        }

    }

    override fun getBoneTransformationMatrix(boneName: String, animationTime: Float): Matrix4f {
        var modifiedAnimationTime = animationTime
        val characterRotationMatrix = Matrix4f()
        // Only the torso is modified, the effect is replicated accross the other bones
        // later
        if (boneName.endsWith("boneTorso"))
            characterRotationMatrix.rotate(entityRotation.yaw / 180f * 3.14159f, Vector3f(0f, 1f, 0f))

        val vel = entityVelocity.velocity

        val horizSpd = sqrt(vel.x() * vel.x() + vel.z() * vel.z())

        modifiedAnimationTime *= 0.75f

        if (boneName.endsWith("boneHead")) {
            val modify = Matrix4f(getAnimationPlayingForBone(boneName, modifiedAnimationTime).getBone(boneName)!!.getTransformationMatrix(modifiedAnimationTime))
            modify.rotate((-(entityRotation.pitch / 180 * Math.PI)).toFloat(), Vector3f(1f, 0f, 0f))
            return modify
        }

        if (horizSpd > 0.030)
            modifiedAnimationTime *= 1.5f

        if (horizSpd > 0.060)
            modifiedAnimationTime *= 1.5f
        else if (listOf("boneArmLU", "boneArmRU", "boneArmLD", "boneArmRD", "boneItemInHand", "boneTorso").contains(boneName)) {

            val trait = entity.traits[TraitMining::class.java]
            if (trait != null && listOf("boneArmLU", "boneArmLD", "boneItemInHand").contains(boneName)) {
                val miningProgress = trait.progress
                if (miningProgress != null) {
                    val lol = entity.world.gameContext.content.animationsLibrary.getAnimation("./animations/human/mining.bvh")

                    return characterRotationMatrix.mul(lol.getBone(boneName)!!.getTransformationMatrix(((System.currentTimeMillis() - miningProgress.started) * 1.5f)))
                }
            }
        }

        val selectedItem =entity.traits[TraitSelectedItem::class]?.selectedItem

        if (listOf("boneArmLU", "boneArmRU").contains(boneName)) {
            val k = if (stance.stance === HumanoidStance.CROUCHING) 0.65f else 0.75f

            if (selectedItem != null) {
                characterRotationMatrix.translate(Vector3f(0f, k, 0f))
                characterRotationMatrix.rotate(-(entityRotation.pitch + if (stance.stance === HumanoidStance.CROUCHING) -50f else 0f) / 180f * 3.14159f,
                        Vector3f(1f, 0f, 0f))
                characterRotationMatrix.translate(Vector3f(0f, -k, 0f))

                if (stance.stance === HumanoidStance.CROUCHING && entity == (entity.world as WorldClient).client.player.controlledEntity)
                    characterRotationMatrix.translate(Vector3f(-0.25f, -0.2f, 0f))

            }
        }

        //TODO rework
        //if (boneName == "boneItemInHand" && selectedItem!!.item is ItemCustomHoldingAnimation) {
        //    modifiedAnimationTime = (selectedItem.item as ItemCustomHoldingAnimation).transformAnimationTime(modifiedAnimationTime)
        //}

        return characterRotationMatrix.mul(getAnimationPlayingForBone(boneName, modifiedAnimationTime).getBone(boneName)!!.getTransformationMatrix(modifiedAnimationTime))
    }
}

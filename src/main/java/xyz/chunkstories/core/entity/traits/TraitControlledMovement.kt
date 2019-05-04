//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.core.voxel.VoxelClimbable

abstract class TraitControlledMovement(entity: Entity) : TraitBasicMovement(entity) {

    protected var running: Boolean = false

    abstract val backwardsSpeed: Double

    abstract val forwardSpeed: Double

    open fun tick(controller: LocalPlayer) {
        val collisions = entity.traits[TraitCollidable::class.java]

        val entityHealth = entity.traits[TraitHealth::class.java]
        val entityVelocity = entity.traits[TraitVelocity::class.java]
        val entityRotation = entity.traits[TraitRotation::class.java]

        if (collisions == null || entityVelocity == null || entityRotation == null || entityHealth == null
                || entityHealth.isDead)
            return

        val focus = controller.hasFocus()

        val inWater = isInWater
        var onLadder = false

        all@ for (vctx in entity.world.getVoxelsWithin(entity.getTranslatedBoundingBox())) {
            if (vctx.voxel is VoxelClimbable) {
                for (box in vctx.translatedCollisionBoxes!!) {
                    // TODO use actual collision model of the entity here
                    if (box.collidesWith(entity.getTranslatedBoundingBox())) {
                        onLadder = true
                        break@all
                    }
                }
            }
        }

        if (focus) {
            if (entityVelocity.velocity.y <= 0.02) {
                if (!inWater && controller.inputsManager.getInputByName("jump")!!.isPressed
                        && collisions.isOnGround) {
                    jump(0.15)
                } else if (inWater && controller.inputsManager.getInputByName("jump")!!.isPressed)
                    jump(0.05)
            }
        }

        if (focus && controller.inputsManager.getInputByName("forward")!!.isPressed) {
            if (controller.inputsManager.getInputByName("run")!!.isPressed)
                running = true
        } else
            running = false

        var horizontalSpeed = 0.0

        var modif = 0.0
        if (focus) {
            if (controller.inputsManager.getInputByName("forward")!!.isPressed
                    || controller.inputsManager.getInputByName("left")!!.isPressed
                    || controller.inputsManager.getInputByName("right")!!.isPressed)

                horizontalSpeed = forwardSpeed
            else if (controller.inputsManager.getInputByName("back")!!.isPressed)
                horizontalSpeed = -backwardsSpeed
        }

        // Water slows you down
        // Strafing
        if (controller.inputsManager.getInputByName("forward")!!.isPressed) {
            if (controller.inputsManager.getInputByName("left")!!.isPressed)
                modif += 45.0
            if (controller.inputsManager.getInputByName("right")!!.isPressed)
                modif -= 45.0
        } else if (controller.inputsManager.getInputByName("back")!!.isPressed) {
            if (controller.inputsManager.getInputByName("left")!!.isPressed)
                modif += (180 - 45).toDouble()
            if (controller.inputsManager.getInputByName("right")!!.isPressed)
                modif -= (180 - 45).toDouble()
        } else {
            if (controller.inputsManager.getInputByName("left")!!.isPressed)
                modif += 90.0
            if (controller.inputsManager.getInputByName("right")!!.isPressed)
                modif -= 90.0
        }

        if (onLadder) {
            entityVelocity.setVelocityY((Math.sin(entityRotation.verticalRotation / 180f * Math.PI) * horizontalSpeed).toFloat().toDouble())
        }

        targetVelocity.x = Math.sin((entityRotation.horizontalRotation + modif) / 180f * Math.PI) * horizontalSpeed
        targetVelocity.z = Math.cos((entityRotation.horizontalRotation + modif) / 180f * Math.PI) * horizontalSpeed

        super.tick()

    }

    override fun tick() {
        val controller = entity.traits[TraitControllable::class]?.controller

        // Consider player inputs...
        if (controller != null && controller is LocalPlayer) {
            tick(controller)
        } else { // no player ? just let it sit
            super.tick()
        }
    }
}

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
import xyz.chunkstories.core.voxel.isInLiquid
import xyz.chunkstories.core.voxel.isOnLadder

abstract class TraitControlledMovement(entity: Entity) : TraitBasicMovement(entity) {

	protected var running: Boolean = false
	abstract val backwardsSpeed: Double
	abstract val forwardSpeed: Double

	open fun tickMovementWithController(controller: LocalPlayer) {
		val collisions = entity.traits[TraitCollidable::class.java] ?: return

		val entityHealth = entity.traits[TraitHealth::class.java] ?: return
		val entityVelocity = entity.traits[TraitVelocity::class.java] ?: return
		val entityRotation = entity.traits[TraitRotation::class.java] ?: return

		if (entityHealth.isDead)
			return

		val focus = controller.hasFocus()

		val climbing = entity.isOnLadder()
		val inLiquid = entity.isInLiquid()

		if (focus) {
			if (entityVelocity.velocity.y <= 0.02) {
				if (controller.inputsManager.getInputByName("jump")!!.isPressed) {
					//println("jumped ${collisions.isOnGround} and ${!inLiquid} ${collisions.isStuckInEntity}")
					if (collisions.isOnGround && !inLiquid)
						jump(0.15)
					else if (inLiquid && controller.inputsManager.getInputByName("jump")!!.isPressed)
						jump(0.05)
				}
			}
		}

		if (focus && controller.inputsManager.getInputByName("forward")!!.isPressed) {
			if (controller.inputsManager.getInputByName("run")!!.isPressed)
				running = true
		} else
			running = false

		var horizontalSpeed = 0.0

		if (focus) {
			if (controller.inputsManager.getInputByName("forward")!!.isPressed
					|| controller.inputsManager.getInputByName("left")!!.isPressed
					|| controller.inputsManager.getInputByName("right")!!.isPressed)

				horizontalSpeed = forwardSpeed
			else if (controller.inputsManager.getInputByName("back")!!.isPressed)
				horizontalSpeed = -backwardsSpeed
		}

		// Strafing
		val strafeAngle = figureOutStrafeAngle(controller)

		if (climbing) {
			entityVelocity.setVelocityY((Math.sin(entityRotation.verticalRotation / 180f * Math.PI) * horizontalSpeed).toFloat().toDouble())
		}

		targetVelocity.x = Math.sin((entityRotation.horizontalRotation + strafeAngle) / 180f * Math.PI) * horizontalSpeed
		targetVelocity.z = Math.cos((entityRotation.horizontalRotation + strafeAngle) / 180f * Math.PI) * horizontalSpeed

		super.tickMovement()
	}

	fun figureOutStrafeAngle(controller: Controller): Double {
		var strafeAngle = 0.0
		if (controller.inputsManager.getInputByName("forward")!!.isPressed) {
			if (controller.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += 45.0
			if (controller.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= 45.0
		} else if (controller.inputsManager.getInputByName("back")!!.isPressed) {
			if (controller.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += (180 - 45).toDouble()
			if (controller.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= (180 - 45).toDouble()
		} else {
			if (controller.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += 90.0
			if (controller.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= 90.0
		}

		return strafeAngle
	}

	override fun tickMovement() {
		val controller = entity.traits[TraitControllable::class]?.controller

		// Consider player inputs...
		if (controller != null && controller is LocalPlayer) {
			tickMovementWithController(controller)
		} else { // no player ? just let it sit
			super.tickMovement()
		}
	}
}

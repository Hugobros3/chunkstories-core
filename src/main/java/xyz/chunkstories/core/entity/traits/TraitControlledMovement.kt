//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Controller
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.isPlayerCharacter
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

	open fun tickMovementWithController() {
		val collisions = entity.traits[TraitCollidable::class.java] ?: return

		val entityHealth = entity.traits[TraitHealth::class.java] ?: return
		val entityVelocity = entity.traits[TraitVelocity::class.java] ?: return
		val entityRotation = entity.traits[TraitRotation::class.java] ?: return

		if (entityHealth.isDead)
			return

		val focus = client.inputsManager.mouse.isGrabbed

		val climbing = entity.isOnLadder()
		val inLiquid = entity.isInLiquid()

		if (focus) {
			if (entityVelocity.velocity.y() <= 0.02) {
				if (client.inputsManager.getInputByName("jump")!!.isPressed) {
					//println("jumped ${collisions.isOnGround} and ${!inLiquid} ${collisions.isStuckInEntity}")
					if (collisions.isOnGround && !inLiquid)
						jump(0.15)
					else if (inLiquid && client.inputsManager.getInputByName("jump")!!.isPressed)
						jump(0.05)
				}
			}
		}

		if (focus && client.inputsManager.getInputByName("forward")!!.isPressed) {
			if (client.inputsManager.getInputByName("run")!!.isPressed)
				running = true
		} else
			running = false

		var horizontalSpeed = 0.0

		if (focus) {
			if (client.inputsManager.getInputByName("forward")!!.isPressed
					|| client.inputsManager.getInputByName("left")!!.isPressed
					|| client.inputsManager.getInputByName("right")!!.isPressed)

				horizontalSpeed = forwardSpeed
			else if (client.inputsManager.getInputByName("back")!!.isPressed)
				horizontalSpeed = -backwardsSpeed
		}

		// Strafing
		val strafeAngle = figureOutStrafeAngle()

		if (climbing) {
			entityVelocity.setVelocityY((Math.sin(entityRotation.pitch / 180f * Math.PI) * horizontalSpeed).toFloat().toDouble())
		}

		targetVelocity.x = Math.sin((entityRotation.yaw + strafeAngle) / 180f * Math.PI) * horizontalSpeed
		targetVelocity.z = Math.cos((entityRotation.yaw + strafeAngle) / 180f * Math.PI) * horizontalSpeed

		super.tickMovement()
	}

	fun figureOutStrafeAngle(): Double {
		var strafeAngle = 0.0
		if (client.inputsManager.getInputByName("forward")!!.isPressed) {
			if (client.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += 45.0
			if (client.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= 45.0
		} else if (client.inputsManager.getInputByName("back")!!.isPressed) {
			if (client.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += (180 - 45).toDouble()
			if (client.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= (180 - 45).toDouble()
		} else {
			if (client.inputsManager.getInputByName("left")!!.isPressed)
				strafeAngle += 90.0
			if (client.inputsManager.getInputByName("right")!!.isPressed)
				strafeAngle -= 90.0
		}

		return strafeAngle
	}

	override fun tickMovement() {
		val controller = entity.controller

		// Consider player inputs...
		if (entity.isPlayerCharacter) {
			tickMovementWithController()
		} else { // no player ? just let it sit
			super.tickMovement()
		}
	}
}

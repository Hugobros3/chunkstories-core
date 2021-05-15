//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Vector3d
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitFlyingMode
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.core.entity.traits.TraitControlledMovement
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance

/** A trait that extend normal controlled movement to enable flying too ! */
internal class TraitMaybeFlyControlledMovement(val entityPlayer: EntityPlayer) : TraitControlledMovement(entityPlayer) {

	override val forwardSpeed: Double
		get() = if (!running || entityPlayer.traitStance.stance === TraitHumanoidStance.HumanoidStance.CROUCHING) 0.06 else 0.09

	override val backwardsSpeed: Double
		get() = 0.05

	var oldStyleFlyControls = false
	var toggleFlyControlsPressed = false

	var lastJumpTap = 0L
	var holdingJump = false
	var isCurrentlyFlying: Boolean
		get() = entity.traits[TraitFlyingMode::class]!!.isFlying
		set(value) {
			entity.traits[TraitFlyingMode::class]!!.isFlying = value
		}

	override fun tickMovementWithController() {
		val toggleFlyControlsPressed = client.engine.inputsManager.getInputByName("toggleFlyControls")!!.isPressed
		if(toggleFlyControlsPressed && !this.toggleFlyControlsPressed)
			oldStyleFlyControls = !oldStyleFlyControls
		this.toggleFlyControlsPressed = toggleFlyControlsPressed

		if (entityPlayer.traitFlyingMode.isAllowed) {
			if(oldStyleFlyControls) {
				// Delegate movement handling to the fly mode component
				flyOldStyle()
			} else {
				fly()
			}
		} else {
			move()
		}

		// TODO check if this is needed
		// Instead of creating a packet and dealing with it ourselves, we instead push
		// the relevant components
		//entityPlayer.traitLocation.pushComponentEveryoneButController()
		// In that case that means pushing to the server.
	}

	fun move() {
		if (client.engine.inputsManager.mouse.isGrabbed && entityPlayer.traits[TraitCollidable::class.java]!!.isOnGround) {
			if (client.engine.inputsManager.getInputByName("crouch")!!.isPressed)
				entityPlayer.traitStance.stance = TraitHumanoidStance.HumanoidStance.CROUCHING
			else
				entityPlayer.traitStance.stance = TraitHumanoidStance.HumanoidStance.STANDING
		}

		super.tickMovementWithController()
	}

	fun fly() {
		val collisions = entity.traits[TraitCollidable::class.java] ?: return

		val entityHealth = entity.traits[TraitHealth::class.java] ?: return
		val entityVelocity = entity.traits[TraitVelocity::class.java] ?: return
		val entityRotation = entity.traits[TraitRotation::class.java] ?: return

		if(!holdingJump && client.engine.inputsManager.getInputByName("jump")!!.isPressed) {
			val now = System.currentTimeMillis()

			if(now - lastJumpTap < 300) {
				//println("successfull double jump")
				if(isCurrentlyFlying) {
					isCurrentlyFlying = false
				} else {
					isCurrentlyFlying = true
					collisions.moveWithCollisionRestrain(0.0, 0.1, 0.0)
				}
			}

			lastJumpTap = now
		}
		holdingJump = client.engine.inputsManager.getInputByName("jump")!!.isPressed

		//TODO reorganise this nonsense
		if(!isCurrentlyFlying) {
			move()
			return
		}

		var horizontalSpeed = 0.0

		if (client.engine.inputsManager.mouse.isGrabbed) {
			if (client.engine.inputsManager.getInputByName("forward")!!.isPressed
					|| client.engine.inputsManager.getInputByName("left")!!.isPressed
					|| client.engine.inputsManager.getInputByName("right")!!.isPressed)

				horizontalSpeed = if (client.engine.inputsManager.getInputByName("run")!!.isPressed)
					flySpeed * 2.0f
				else
					flySpeed * 1.0f
			else if (client.engine.inputsManager.getInputByName("back")!!.isPressed)
				horizontalSpeed = -flySpeed
		}

		// Strafing
		val strafeAngle = figureOutStrafeAngle()

		flySpeed = 0.1
		targetVelocity.x = Math.sin((entityRotation.yaw + strafeAngle) / 180f * Math.PI) * horizontalSpeed
		targetVelocity.z = Math.cos((entityRotation.yaw + strafeAngle) / 180f * Math.PI) * horizontalSpeed
		targetVelocity.y = when {
			client.engine.inputsManager.getInputByName("jump")!!.isPressed -> flySpeed
			client.engine.inputsManager.getInputByName("crouch")!!.isPressed -> -flySpeed
			else -> 0.0
		}

		val velocity = Vector3d(entityVelocity.velocity)
		acceleration = Vector3d(targetVelocity.x() - velocity.x(), targetVelocity.y() - velocity.y(), targetVelocity.z() - velocity.z())

		val maxAcceleration = 0.005
		if (acceleration.length() > maxAcceleration) {
			acceleration.normalize()
			acceleration.mul(maxAcceleration)
		}

		velocity.add(acceleration)
		entityVelocity.setVelocity(velocity)
		collisions.moveWithCollisionRestrain(velocity)

		// Flying also means we're standing
		entityPlayer.traitStance.stance = TraitHumanoidStance.HumanoidStance.STANDING

		if(collisions.isOnGround)
			isCurrentlyFlying = false
	}

	var flySpeed = 0.0625
	var isNoclip = true

	fun flyOldStyle() {
		if (!client.engine.inputsManager.mouse.isGrabbed)
			return

		// Flying resets the entity velocity, if it has one
		entity.traits[TraitVelocity::class]?.setVelocity(0.0, 0.0, 0.0)

		val entityRotation = entity.traits[TraitRotation::class.java] ?: return
		// you must be able to rotate to fly

		val entityCollisions = entity.traits[TraitCollidable::class.java]
		val ignoreCollisions = (entityCollisions == null) or this.isNoclip

		var cameraSpeed = flySpeed
		if (client.engine.inputsManager.getInputByName("flyReallyFast")!!.isPressed)
			cameraSpeed *= 8 * 5f
		else if (client.engine.inputsManager.getInputByName("flyFast")!!.isPressed)
			cameraSpeed *= 8f

		if (client.engine.inputsManager.getInputByName("back")!!.isPressed) {
			val horizRotRad = ((entityRotation.yaw + 180f) / 180f * Math.PI).toFloat()
			val vertRotRad = (-entityRotation.pitch / 180f * Math.PI).toFloat()
			if (ignoreCollisions)
				entity.traitLocation.move(Math.sin(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()), Math.sin(vertRotRad.toDouble()) * cameraSpeed,
						Math.cos(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()))
			else
				entityCollisions!!.moveWithCollisionRestrain(Math.sin(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()), Math.sin(vertRotRad.toDouble()) * cameraSpeed,
						Math.cos(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()))
		}
		if (client.engine.inputsManager.getInputByName("forward")!!.isPressed) {
			val horizRotRad = (entityRotation.yaw / 180f * Math.PI).toFloat()
			val vertRotRad = (entityRotation.pitch / 180f * Math.PI).toFloat()
			if (ignoreCollisions)
				entity.traitLocation.move(Math.sin(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()), Math.sin(vertRotRad.toDouble()) * cameraSpeed,
						Math.cos(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()))
			else
				entityCollisions!!.moveWithCollisionRestrain(Math.sin(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()), Math.sin(vertRotRad.toDouble()) * cameraSpeed,
						Math.cos(horizRotRad.toDouble()) * cameraSpeed * Math.cos(vertRotRad.toDouble()))
		}
		if (client.engine.inputsManager.getInputByName("right")!!.isPressed) {
			val horizRot = ((entityRotation.yaw + 90) / 180f * Math.PI).toFloat()
			if (ignoreCollisions)
				entity.traitLocation.move(-Math.sin(horizRot.toDouble()) * cameraSpeed, 0.0, -Math.cos(horizRot.toDouble()) * cameraSpeed)
			else
				entityCollisions!!.moveWithCollisionRestrain(-Math.sin(horizRot.toDouble()) * cameraSpeed, 0.0, -Math.cos(horizRot.toDouble()) * cameraSpeed)
		}
		if (client.engine.inputsManager.getInputByName("left")!!.isPressed) {
			val horizRot = ((entityRotation.yaw - 90) / 180f * Math.PI).toFloat()
			if (ignoreCollisions)
				entity.traitLocation.move(-Math.sin(horizRot.toDouble()) * cameraSpeed, 0.0, -Math.cos(horizRot.toDouble()) * cameraSpeed)
			else
				entityCollisions!!.moveWithCollisionRestrain(-Math.sin(horizRot.toDouble()) * cameraSpeed, 0.0, -Math.cos(horizRot.toDouble()) * cameraSpeed)
		}

		// Flying also means we're standing
		entityPlayer.traitStance.stance = TraitHumanoidStance.HumanoidStance.STANDING
	}
}

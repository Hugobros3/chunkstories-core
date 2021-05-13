//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Math
import org.joml.Vector3d
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.entity.traits.TraitInteractible
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.makeCamera
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.interfaces.ItemZoom
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.MutableChunkCell
import xyz.chunkstories.core.CoreOptions
import xyz.chunkstories.core.gui.CreativeBlockSelector

internal class PlayerController(private val entityPlayer: EntityPlayer) : TraitControllable(entityPlayer) {
	val isClient = entity.world.gameInstance is IngameClient
	val client = entity.world.gameInstance as IngameClient

	var lastPX = -1.0
	var lastPY = -1.0

	override fun onEachFrame(): Boolean {
		val controller = entity.controller
		if (client.gui.hasFocus()) {
			rotateCameraAccordingToMouse()
			return true
		}
		return false
	}

	fun rotateCameraAccordingToMouse() {
		if (entityPlayer.traitHealth.isDead)
			return
		if (!client.inputsManager.mouse.isGrabbed)
			return

		val cPX = client.inputsManager.mouse.cursorX
		val cPY = client.inputsManager.mouse.cursorY

		var dx = 0.0
		var dy = 0.0
		if (lastPX != -1.0) {
			dx = cPX - client.gameWindow.width / 2.0
			dy = cPY - client.gameWindow.height / 2.0
		}
		lastPX = cPX
		lastPY = cPY

		var rotH = entityPlayer.traitRotation.yaw.toDouble()
		var rotV = entityPlayer.traitRotation.pitch.toDouble()

		var modifier = 1.0
		val selectedItem = entityPlayer.traits[TraitSelectedItem::class]?.selectedItem

		if (selectedItem != null && selectedItem.item is ItemZoom) {
			val item = selectedItem.item as ItemZoom
			modifier = 1.0 / item.zoomFactor
		}

		rotH -= dx * modifier / 3f * client.configuration.getDoubleValue(CoreOptions.mouseSensitivity)
		rotV += dy * modifier / 3f * client.configuration.getDoubleValue(CoreOptions.mouseSensitivity)
		entityPlayer.traitRotation.setRotation(rotH, rotV)

		client.inputsManager.mouse.setMouseCursorLocation(client.gameWindow.width / 2.0, client.gameWindow.height / 2.0)
	}

	// TODO: use this again
	val camera: Camera
		get() {
			val client = entity.world.gameInstance as? IngameClient ?: throw Exception("calling getCamera() on a non-client context is undefined behavior")

			val location = entity.location
			val cameraPosition = Vector3d(location)
			cameraPosition.y += entityPlayer.traitStance.stance.eyeLevel

			val direction = (entity.traits[TraitRotation::class]?.directionLookingAt ?: Vector3d(0.0, 0.0, 1.0)).toVec3f()
			val up = (entity.traits[TraitRotation::class]?.upDirection ?: Vector3d(0.0, 0.0, 1.0)).toVec3f()

			val fovModifier = entityPlayer.traitSelectedItem.selectedItem?.let { (it.item as? ItemZoom)?.zoomFactor } ?: 1f
			var speedEffect = (entityPlayer.traitVelocity.velocity.x() * entityPlayer.traitVelocity.velocity.x() + entityPlayer.traitVelocity.velocity.z() * entityPlayer.traitVelocity.velocity.z()).toFloat()
			speedEffect -= 0.07f * 0.07f
			speedEffect = Math.max(0.0f, speedEffect)
			speedEffect *= 50.0f

			return client.makeCamera(cameraPosition, direction, up, fovModifier * (90f + speedEffect))
		}

	override fun onControllerInput(input: Input): Boolean {
		// Traits can handle inputs
		for (i in (0 until entity.traits.byId.size).reversed()) {
			val trait = entity.traits.byId[i]
			if(trait.handleInput(input))
				return true
		}

		val controller = entity.controller

		// We are moving inventory bringup here !
		if (input.name == "inventory" && isClient) {
			TODO("More inventory")
			/*if (entityPlayer.traitCreativeMode.enabled) {
				entityPlayer.world.client.gui.let {
					it.topLayer = CreativeBlockSelector(it, it.topLayer)
				}
				//entityPlayer.world.client.gui.openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!, entity.world.content.voxels().createCreativeInventory())
			} else {
				entityPlayer.world.client.gui.openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!)
			}

			return true*/
		}

		// Then we check if the world minds being interacted with
		// Creative mode features building and picking.

		val lookingAt = entityPlayer.traitSight.getLookingAt(5.0)
		when (lookingAt) {
			is RayResult.Hit.EntityHit -> {
				val observedEntity = lookingAt.entity
				if (observedEntity != entity && observedEntity.traits[TraitInteractible::class]?.handleInteraction(entity, input) == true) {
					return true
				}
			}
			is RayResult.Hit.VoxelHit -> {
				val cell = lookingAt.cell as MutableChunkCell
				if (cell.data.blockType.onInteraction(entity, cell, input)) {
					return true
				}
			}
		}

		val itemInHand = entityPlayer.traitSelectedItem.selectedItem
		if (itemInHand != null) {
			// See if the item handles the interaction
			if (itemInHand.item.onControllerInput(entityPlayer, itemInHand, input, controller!!))
				return true
		}

		/*if (input.name == "mouse.left") {
			when (lookingAt) {
				is RayResult.Hit.EntityHit -> {
					val targetEntity = lookingAt.entity
					entity.traits[TraitMeleeCombat::class]?.tryAttack(targetEntity, lookingAt.part)
				}
			}
		}*/

		if (entityPlayer.world is WorldMaster && lookingAt is RayResult.Hit.VoxelHit) {
			// Creative mode features building and picking.
			if (entityPlayer.traitCreativeMode.enabled) {
				if (input.name == "mouse.left") {
					val cell = lookingAt.cell
					cell.data.blockType.breakBlock(cell as MutableChunkCell, controller as? Player, TraitCreativeMode.CREATIVE_MODE_MINING_TOOL)
				} else if (input.name == "mouse.middle") {
					val peekedCell = lookingAt.cell
					val voxel = peekedCell.data.blockType

					if (!voxel.isAir) {
						// Spawn new itemPile in his inventory
						val item = voxel.getVariant(peekedCell.data).newItem<ItemBlock>()
						entityPlayer.traits[TraitInventory::class]?.inventory!!.setItemAt(entityPlayer.traitSelectedItem.selectedSlot, 0, item, 1, force = true)
						return true
					}
				}
			}
		}

		return false
	}
}

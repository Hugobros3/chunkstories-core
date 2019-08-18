package xyz.chunkstories.core.entity

import org.joml.Math
import org.joml.Vector3d
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.traits.TraitInteractible
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.graphics.structs.makeCamera
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.interfaces.ItemZoom
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.core.CoreOptions
import xyz.chunkstories.core.gui.CreativeBlockSelector
import java.lang.Exception

internal class EntityPlayerController(private val entityPlayer: EntityPlayer) : TraitControllable(entityPlayer) {

    var lastPX = -1.0
    var lastPY = -1.0

    override fun onEachFrame(): Boolean {
        val controller = controller
        if (controller is LocalPlayer && controller.hasFocus()) {
            rotateCameraAccordingToMouse(controller)
            return true
        }
        return false
    }

    fun rotateCameraAccordingToMouse(controller: LocalPlayer) {
        if (entityPlayer.traitHealth.isDead)
            return
        if (!controller.inputsManager.mouse.isGrabbed)
            return

        val cPX = controller.inputsManager.mouse.cursorX
        val cPY = controller.inputsManager.mouse.cursorY

        var dx = 0.0
        var dy = 0.0
        if (lastPX != -1.0) {
            dx = cPX - controller.window.width / 2.0
            dy = cPY - controller.window.height / 2.0
        }
        lastPX = cPX
        lastPY = cPY

        var rotH = entityPlayer.traitRotation.horizontalRotation.toDouble()
        var rotV = entityPlayer.traitRotation.verticalRotation.toDouble()

        var modifier = 1.0
        val selectedItem = entityPlayer.traits[TraitSelectedItem::class]?.selectedItem

        if (selectedItem != null && selectedItem.item is ItemZoom) {
            val item = selectedItem.item as ItemZoom
            modifier = 1.0 / item.zoomFactor
        }

        rotH -= dx * modifier / 3f * controller.client.configuration.getDoubleValue(CoreOptions.mouseSensitivity)
        rotV += dy * modifier / 3f * controller.client.configuration.getDoubleValue(CoreOptions.mouseSensitivity)
        entityPlayer.traitRotation.setRotation(rotH, rotV)

        controller.inputsManager.mouse.setMouseCursorLocation(controller.window.width / 2.0, controller.window.height / 2.0)
    }

    override val camera: Camera
        get() {
            val client = entity.world.gameContext as? IngameClient ?: throw Exception("calling getCamera() on a non-client context is undefined behavior")

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
        val controller = controller

        // We are moving inventory bringup here !
        if (input.name == "inventory" && entityPlayer.world is WorldClient) {

            if (entityPlayer.traitCreativeMode.get()) {
                entityPlayer.world.client.gui.let {
                    it.topLayer = CreativeBlockSelector(it, it.topLayer)
                }
                //entityPlayer.world.client.gui.openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!, entity.world.content.voxels().createCreativeInventory())
            } else {
                entityPlayer.world.client.gui.openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!)
            }

            return true
        }


        //var maxLen = 1024.0
        //val diff = Vector3d(lookingAt).sub(entityPlayer.location)
        //maxLen = diff.length()

        /*val initialPosition = Vector3d(entityPlayer.location)
        initialPosition.add(Vector3d(0.0, entityPlayer.traitStance.stance.eyeLevel, 0.0))

        val direction = entityPlayer.traitRotation.directionLookingAt*/

        /*val ray = RayQuery(Location(entityPlayer.world, initialPosition), direction, 0.0, 256.0)
        val pointingAt = ray.trace()
        println(pointingAt)*/

        /*val i = entityPlayer.world.collisionsManager.rayTraceEntities(initialPosition, direction, maxLen)
        while (i.hasNext()) {
            val e = i.next()
            if (e !== entityPlayer && e.traits[TraitInteractible::class]?.handleInteraction(entityPlayer, input) == true)
                return true
        }*/// Spawn new itemPile in his inventory
        //val item = entityPlayer.world.gameContext.content.items().getItemDefinition("item_voxel")!!.newItem<ItemVoxel>()
        //item.voxel = voxel
        //item.voxelMeta = peekedCell.metaData
        // Here goes generic entity response to interaction

        // n/a

        // Then we check if the world minds being interacted with
        // Creative mode features building and picking.

        val lookingAt = entityPlayer.traitSight.getLookingAt(5.0)
        when (lookingAt) {
            is RayResult.Hit.EntityHit -> {
                val observedEntity = lookingAt.entity
                if(observedEntity != entity && observedEntity.traits[TraitInteractible::class]?.handleInteraction(entity, input) == true) {
                    return true
                }
            }
            is RayResult.Hit.VoxelHit -> {
                val cell = lookingAt.cell
                // Should always be true !
                if(cell is ChunkCell) {
                    if(cell.voxel.handleInteraction(entity, cell, input)) {
                        return true
                    }
                }
            }
        }

        val itemInHand = entityPlayer.traitSelectedItem.selectedItem
        if (itemInHand != null) {
            // See if the item handles the interaction
            if (itemInHand.item.onControllerInput(entityPlayer, itemInHand, input, controller!!))
                return true
        } else {

        }

        if (entityPlayer.world is WorldMaster && lookingAt is RayResult.Hit.VoxelHit) {
            // Creative mode features building and picking.
            if (entityPlayer.traitCreativeMode.get()) {
                if (input.name == "mouse.left") {
                    val cell = lookingAt.cell
                    cell.voxel.breakBlock(cell, TraitCreativeMode.CREATIVE_MODE_MINING_TOOL, entity)
                } else if (input.name == "mouse.middle") {
                    val peekedCell = lookingAt.cell
                    val voxel = peekedCell.voxel

                    if (!voxel.isAir()) {
                        // Spawn new itemPile in his inventory
                        val item = voxel.getVariant(peekedCell).newItem<ItemVoxel>()
                        entityPlayer.traits[TraitInventory::class]?.inventory!!.setItemAt(entityPlayer.traitSelectedItem.getSelectedSlot(), 0, item, 1, force = true)
                        return true
                    }
                }
            }
        }

        return false
    }
}

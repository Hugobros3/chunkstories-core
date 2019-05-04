package xyz.chunkstories.core.entity

import org.joml.Math
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.client.LocalPlayer
import xyz.chunkstories.api.entity.traits.TraitInteractible
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.events.player.voxel.PlayerVoxelModificationEvent
import xyz.chunkstories.api.exceptions.world.WorldException
import xyz.chunkstories.api.graphics.structs.Camera
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.interfaces.ItemZoom
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.core.CoreOptions
import xyz.chunkstories.core.item.BlockMiningOperation
import xyz.chunkstories.core.item.inventory.*
import java.lang.Exception


internal class EntityPlayerController(private val entityPlayer: EntityPlayer) : TraitControllable(entityPlayer) {

    var lastPX = -1.0
    var lastPY = -1.0

    override fun onEachFrame(): Boolean {
        val controller = controller
        if (controller is LocalPlayer && controller.hasFocus()) {
            moveCamera(controller)
            return true
        }
        return false
    }

    fun moveCamera(controller: LocalPlayer) {
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

            val fov = (90.0 / 360.0 * (Math.PI * 2)).toFloat()
            val aspect = client.gameWindow.width.toFloat() / client.gameWindow.height.toFloat()
            val projectionMatrix = Matrix4f().perspective(fov, aspect, 0.1f, 2000f, true)

            val location = entity.location
            val cameraPosition = location.toVec3f()

            cameraPosition.y += entityPlayer.traitStance.stance.eyeLevel.toFloat()

            val entityDirection = (entity.traits[TraitRotation::class]?.directionLookingAt ?: Vector3d(0.0, 0.0, 1.0)).toVec3f()
            val entityLookAt = Vector3f(cameraPosition).add(entityDirection)

            val up = (entity.traits[TraitRotation::class]?.upDirection ?: Vector3d(0.0, 0.0, 1.0)).toVec3f()

            val viewMatrix = Matrix4f()
            viewMatrix.lookAt(cameraPosition, entityLookAt, up)

            val fovModifier = entityPlayer.traitSelectedItem.selectedItem?.let { (it.item as? ItemZoom)?.let { it.zoomFactor } } ?: 1f

            var speedEffect = (entityPlayer.traitVelocity.velocity.x() * entityPlayer.traitVelocity.velocity.x() + entityPlayer.traitVelocity.velocity.z() * entityPlayer.traitVelocity.velocity.z()).toFloat()

            speedEffect -= 0.07f * 0.07f
            speedEffect = Math.max(0.0f, speedEffect)
            speedEffect *= 500.0f

            return Camera(cameraPosition, entityDirection, up, fovModifier * (fov + speedEffect), viewMatrix, projectionMatrix)
        }

    override fun onControllerInput(input: Input): Boolean {
        val controller = controller

        // We are moving inventory bringup here !
        if (input.name == "inventory" && entityPlayer.world is WorldClient) {

            if (entityPlayer.traitCreativeMode.get()) {
                entityPlayer.world.client.gui.openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!,
                        entity.world.content.voxels().createCreativeInventory())
            } else {
                entityPlayer.world.client.gui
                        .openInventories(entityPlayer.traits[TraitInventory::class]?.inventory!!, entityPlayer.traitArmor.inventory)
            }

            return true
        }

        val blockLocation = entityPlayer.traitVoxelSelection.getBlockLookingAt(true, false)

        var maxLen = 1024.0

        if (blockLocation != null) {
            val diff = Vector3d(blockLocation).sub(entityPlayer.location)
            // Vector3d dir = diff.clone().normalize();
            maxLen = diff.length()
        }

        val initialPosition = Vector3d(entityPlayer.location)
        initialPosition.add(Vector3d(0.0, entityPlayer.traitStance.stance.eyeLevel, 0.0))

        val direction = entityPlayer.traitRotation.directionLookingAt

        val i = entityPlayer.world.collisionsManager.rayTraceEntities(initialPosition, direction, maxLen)
        while (i.hasNext()) {
            val e = i.next()
            if (e !== entityPlayer && e.traits[TraitInteractible::class]?.handleInteraction(entityPlayer, input) == true)
                return true
        }

        val itemSelected = entityPlayer.traitSelectedItem.selectedItem
        if (itemSelected != null) {
            // See if the item handles the interaction
            if (itemSelected.item.onControllerInput(entityPlayer, itemSelected, input, controller!!))
                return true
        }
        if (entityPlayer.world is WorldMaster) {
            // Creative mode features building and picking.
            if (entityPlayer.traitCreativeMode.get()) {
                if (input.name == "mouse.left") {
                    if (blockLocation != null) {
                        // Player events mod
                        if (controller is Player) {
                            val player = controller as Player?
                            val cell = entityPlayer.world.peekSafely(blockLocation)
                            val future = FutureCell(cell)
                            future.voxel = entityPlayer.definition.store().parent().voxels().air()
                            future.blocklight = 0
                            future.sunlight = 0
                            future.metaData = 0

                            val event = PlayerVoxelModificationEvent(cell, future, TraitCreativeMode.CREATIVE_MODE,
                                    player!!)

                            // Anyone has objections ?
                            entityPlayer.world.gameContext.pluginManager.fireEvent(event)

                            if (event.isCancelled)
                                return true

                            BlockMiningOperation.spawnBlockDestructionParticles(blockLocation, entityPlayer.world)
                            entityPlayer.world.soundManager
                                    .playSoundEffect("sounds/gameplay/voxel_remove.ogg", SoundSource.Mode.NORMAL, blockLocation, 1.0f, 1.0f)

                            try {
                                entityPlayer.world.poke(future, entityPlayer)
                                // world.poke((int)blockLocation.x, (int)blockLocation.y, (int)blockLocation.z,
                                // null, 0, 0, 0, this);
                            } catch (e: WorldException) {
                                // Discard but maybe play some effect ?
                            }

                            return true
                        }
                    }
                } else if (input.name == "mouse.middle") {
                    if (blockLocation != null) {
                        val peekedCell = entityPlayer.world.peekSafely(blockLocation)
                        val voxel = peekedCell.voxel

                        if (!voxel.isAir()) {
                            // Spawn new itemPile in his inventory
                            val item = entityPlayer.world.gameContext.content.items().getItemDefinition("item_voxel")!!.newItem<ItemVoxel>()
                            item.voxel = voxel
                            item.voxelMeta = peekedCell.metaData

                            entityPlayer.traits[TraitInventory::class]?.inventory!!.setItemAt(entityPlayer.traitSelectedItem.getSelectedSlot(), 0, item)
                            return true
                        }
                    }
                }
            }
        }
        // Here goes generic entity response to interaction

        // n/a

        // Then we check if the world minds being interacted with
        return entityPlayer.world.handleInteraction(entityPlayer, blockLocation, input)
    }
}

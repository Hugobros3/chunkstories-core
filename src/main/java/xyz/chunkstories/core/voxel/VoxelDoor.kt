//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.reverseWindingOrder
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.MutableCellData
import xyz.chunkstories.api.world.cell.PodCellData
import xyz.chunkstories.api.world.chunk.MutableChunkCell
import xyz.chunkstories.api.world.getCellMut

/**
 * 2-blocks tall door Requires two consecutive voxel ids, x being lower, x+1
 * top, the top part should be suffixed of _top
 */
class VoxelDoor(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
    private val top: Boolean = name.endsWith("_top")

    val model: Model
    val modelFlipped: Model
    val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.textures[BlockSide.FRONT.ordinal].name}.png")))

    private val upperPart: BlockType
        get() = if (top)
            this
        else
            content.blockTypes[name + "_top"]!!

    val lowerPart: BlockType
        get() = if (top)
            content.blockTypes[name.substring(0, name.length - 4)]!!
        else
            this

    private fun flipModel(model: Model): Model {
        return Model(model.meshes.map { it.reverseWindingOrder() })
    }

    init {
        model = content.models[definition["model"].asString ?: "voxels/blockmodels/wood_door/wood_door.dae"]
        modelFlipped = flipModel(model)
    }

    override fun loadRepresentation(): BlockRepresentation {
        return if (top) {
            // don't render the top :)
            BlockRepresentation.Custom { }
        } else {
            BlockRepresentation.Custom { cell ->
                renderDoor(cell, this)
            }
        }
    }

    fun renderDoor(cell: Cell, mesher: BlockRepresentation.Custom.RenderInterface) {
        var facingPassed = (cell.data.extraData shr 2) and 0x3
        val isOpen = (cell.data.extraData shr 0) and 0x1 == 1
        val hingeSide = (cell.data.extraData shr 1) and 0x1 == 1

        val matrix = Matrix4f()

        matrix.translate(0.5f, 0f, 0.5f)
        matrix.rotate(Math.PI.toFloat() * 0.5f * (facingPassed - 3), 0f, 1f, 0f)
        matrix.translate(-0.5f, 0f, -0.5f)

        matrix.translate(0.5f, 0f, 0.5f)
        if (!hingeSide)
            matrix.scale(-1f, 1f, 1f)

        matrix.translate(-0.5f, 0f, -0.5f)

        val ith = 1 / 16f
        matrix.translate(ith, 0f, ith)
        matrix.rotate(Math.PI.toFloat() * 0.5f * if (isOpen) -1 else 0, 0f, 1f, 0f)
        matrix.translate(-ith, 0f, -ith)

        //println("rendering ${cell.data.extraData} rslt="+facingPassed)

        mesher.addModel(if (hingeSide) model else modelFlipped, matrix, mappedOverrides)
    }

    // Meta
    // 0x0 -> open/close
    // 0x1 -> left/right hinge || left = 0 right = 1 (left is default)
    // 0x2-0x4 -> side ( BlockSide << 2 )

    override fun onInteraction(entity: Entity, cell: MutableChunkCell, input: Input): Boolean {
        if (input.name != "mouse.right")
            return false
        if (entity.world !is WorldMaster)
            return true

        val isOpen = (cell.data.extraData shr 0) and 0x1 == 1
        val hingeSide = (cell.data.extraData shr 1) and 0x1 == 1
        val facingPassed = (cell.data.extraData shr 2) and 0x3

        val newState = !isOpen

        val newData = computeMeta(newState, hingeSide, facingPassed)

        val otherPartLocation = cell.location
        if (top)
            otherPartLocation.add(0.0, -1.0, 0.0)
        else
            otherPartLocation.add(0.0, 1.0, 0.0)

        val otherLocationPeek = cell.world.getCellMut(otherPartLocation) ?: return true
        if (otherLocationPeek.data.blockType is VoxelDoor) {
            //println("new door status : $newState")
            cell.world.soundManager.playSoundEffect("sounds/voxels/door.ogg", Mode.NORMAL, cell.location, 1.0f, 1.0f)

            cell.data.extraData = newData
            otherLocationPeek.data.extraData = newData
        } else {
            cell.world.logger.error("Incomplete door @ $otherPartLocation")
        }

        return true
    }

    override fun getCollisionBoxes(info: Cell): Array<Box> {
        val facingPassed = (info.data.extraData shr 2) and 0x3
        val isOpen = (info.data.extraData shr 0) and 0x1 == 1
        val hingeSide = (info.data.extraData shr 1) and 0x1 == 1

        val boxes = arrayOf(Box.fromExtents(0.125, 1.0, 1.0).translate(0.125 / 2, 0.0, 0.5))

        if (isOpen) {
            when (facingPassed + (if (hingeSide) 4 else 0)) {
                0 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 0.125 / 2)
                1 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(0.125 / 2, 0.0, 0.5)
                2 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 1.0 - 0.125 / 2)
                3 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0.0, 0.5)
                4 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 1.0 - 0.125 / 2)
                5 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0.0, 0.5)
                6 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 0.125 / 2)
                7 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(0.125 / 2, 0.0, 0.5)
            }
        } else {
            when (facingPassed) {
                0 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(0.125 / 2, 0.0, 0.5)
                1 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 1.0 - 0.125 / 2)
                2 -> boxes[0] = Box.fromExtents(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0.0, 0.5)
                3 -> boxes[0] = Box.fromExtents(1.0, 1.0, 0.125).translate(0.5, 0.0, 0.125 / 2)
            }
        }

        boxes[0].translate(-boxes[0].extents.x() / 2, 0.0, -boxes[0].extents.z() / 2)

        return boxes
    }

    override fun whenPlaced(cell: MutableChunkCell) {
        super.whenPlaced(cell)

        if (!top)
            cell.world.setCellData(cell.x, cell.y + 1, cell.z, PodCellData(upperPart, 0, 0, cell.data.extraData))
    }

    override fun onRemove(cell: MutableChunkCell): Boolean {
        val world = cell.world
        val x = cell.x
        val y = cell.y
        val z = cell.z

        var otherPartOfTheDoorY = y

        if (top)
            otherPartOfTheDoorY--
        else
            otherPartOfTheDoorY++

        val restOfTheDoorVoxel = world.getCell(x, otherPartOfTheDoorY, z) ?: return true
        // Remove the other part as well, if it still exists
        if (restOfTheDoorVoxel.data.blockType is VoxelDoor)
            world.setCellData(x, otherPartOfTheDoorY, z, PodCellData(content.blockTypes.air))
        return true
    }

    companion object {
        fun computeMeta(isOpen: Boolean, hingeSide: Boolean, doorFacingSide: BlockSide): Int {
            return computeMeta(isOpen, hingeSide, doorFacingSide.ordinal)
        }

        private fun computeMeta(isOpen: Boolean, hingeSide: Boolean, doorFacingsSide: Int): Int {
            // System.out.println(doorFacingsSide + " open: " + isOpen + " hinge:" +
            // hingeSide);
            return (doorFacingsSide shl 2) or (((if (hingeSide) 1 else 0) and 0x1) shl 1) or ((if (isOpen) 1 else 0) and 0x1)
        }
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
        if (top)
            return emptyList()

        val map = mutableMapOf<String, Json>(
                "voxel" to Json.Value.Text(name),
                "class" to Json.Value.Text(ItemDoor::class.java.canonicalName!!),
                "slotsHeight" to Json.Value.Number(2.0)
        )

        val additionalItems = definition["itemProperties"].asDict?.elements
        if (additionalItems != null)
            map.putAll(additionalItems)

        val definition = ItemDefinition(itemStore, name, Json.Dict(map))
        return listOf(definition)
    }
}

class ItemDoor(definition: ItemDefinition) : ItemBlock(definition) {
    val door = blockType as VoxelDoor

    override fun buildRepresentation(worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
        val representation = ModelInstance(door.model, ModelPosition(worldPosition).apply {
            matrix.scale(0.5f)
            matrix.translate(-0.5f, -0.3f, -0.0f)
        }, door.mappedOverrides)
        representationsGobbler.acceptRepresentation(representation, -1)
    }

    override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): MutableCellData {
        /*super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)

        val world = cell.world
        val x = cell.x
        val y = cell.y
        val z = cell.z

        // Check top is free
        val topData = world.peekRaw(x, y + 1, z)
        if (VoxelFormat.id(topData) != 0)
            throw IllegalBlockModificationException(cell, "Top part isn't free")

        val isOpen = false
        val hingeSide: Boolean
        val doorSideFacing: BlockSide

        val loc = placingEntity.location
        val dx = loc.x() - (x + 0.5)
        val dz = loc.z() - (z + 0.5)
        if (Math.abs(dx) > Math.abs(dz)) {
            if (dx > 0)
                doorSideFacing = BlockSide.RIGHT
            else
                doorSideFacing = BlockSide.LEFT
        } else {
            if (dz > 0)
                doorSideFacing = BlockSide.FRONT
            else
                doorSideFacing = BlockSide.BACK
        }

        // If there is an adjacent one, set the hinge to right
        var adjacent: Voxel? = null
        when (doorSideFacing) {
            BlockSide.LEFT -> adjacent = world.peekSimple(x, y, z - 1)
            BlockSide.RIGHT -> adjacent = world.peekSimple(x, y, z + 1)
            BlockSide.FRONT -> adjacent = world.peekSimple(x - 1, y, z)
            BlockSide.BACK -> adjacent = world.peekSimple(x + 1, y, z)
            else -> {
            }
        }

        hingeSide = adjacent is VoxelDoor

        cell.data.extraData = VoxelDoor.computeMeta(isOpen, hingeSide, doorSideFacing)

        // println("placing door: dx=$dx dz=$dz rslt=$doorSideFacing meta=${cell.data.extraData}")

        return true*/
        TODO("Finish rewrite")
    }
}

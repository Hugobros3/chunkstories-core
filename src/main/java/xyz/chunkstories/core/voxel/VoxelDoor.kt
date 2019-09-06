//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.reverseWindingOrder
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.voxel.*
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell

/**
 * 2-blocks tall door Requires two consecutive voxel ids, x being lower, x+1
 * top, the top part should be suffixed of _top
 */
class VoxelDoor(definition: VoxelDefinition) : Voxel(definition) {
    private val top: Boolean = name.endsWith("_top")

	val model: Model
	val modelFlipped: Model
    val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

    private val upperPart: Voxel
        get() = if (top)
            this
        else
            store.getVoxel(name + "_top")!!

    val lowerPart: Voxel
        get() = if (top)
            store.getVoxel(name.substring(0, name.length - 4))!!
        else
            this

    private fun flipModel(model: Model): Model {
        return Model(model.meshes.map { it.reverseWindingOrder() })
    }

    init {
        model = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/wood_door/wood_door.dae"]

        modelFlipped = flipModel(model)

        if (top) {
            // don't render the top :)
            customRenderingRoutine = { _ ->

            }
        } else {
            customRenderingRoutine = { cell ->
                renderDoor(cell, this)
            }
        }
        //for (int i = 0; i < 8; i++)
        //	models[i] = store().models().getVoxelModel("door.m" + i);
    }

    fun renderDoor(cell: Cell, mesher: ChunkMeshRenderingInterface) {
        var facingPassed = (cell.metaData shr 2) and 0x3
        val isOpen = (cell.metaData shr 0) and 0x1 == 1
        val hingeSide = (cell.metaData shr 1) and 0x1 == 1

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

        //println("rendering ${cell.metaData} rslt="+facingPassed)

        mesher.addModel(if (hingeSide) model else modelFlipped, matrix, mappedOverrides)
    }

    // Meta
    // 0x0 -> open/close
    // 0x1 -> left/right hinge || left = 0 right = 1 (left is default)
    // 0x2-0x4 -> side ( VoxelSide << 2 )

    override fun handleInteraction(entity: Entity, voxelContext: ChunkCell, input: Input): Boolean {
        if (input.name != "mouse.right")
            return false
        if (entity.world !is WorldMaster)
            return true

        val isOpen = (voxelContext.metaData shr 0) and 0x1 == 1
        val hingeSide = (voxelContext.metaData shr 1) and 0x1 == 1
        val facingPassed = (voxelContext.metaData shr 2) and 0x3

        val newState = !isOpen

        val newData = computeMeta(newState, hingeSide, facingPassed)

        val otherPartLocation = voxelContext.location
        if (top)
            otherPartLocation.add(0.0, -1.0, 0.0)
        else
            otherPartLocation.add(0.0, 1.0, 0.0)

        val otherLocationPeek = voxelContext.world.peek(otherPartLocation)
        if (otherLocationPeek.voxel is VoxelDoor) {
            //println("new door status : $newState")
            voxelContext.world.soundManager.playSoundEffect("sounds/voxels/door.ogg", Mode.NORMAL, voxelContext.location, 1.0f, 1.0f)

            voxelContext.metaData = newData
            otherLocationPeek.metaData = newData
        } else {
            store.parent.logger.error("Incomplete door @ $otherPartLocation")
        }

        return true
    }

    override fun getCollisionBoxes(info: Cell): Array<Box>? {
        val facingPassed = (info.metaData shr 2) and 0x3
        val isOpen = (info.metaData shr 0) and 0x1 == 1
        val hingeSide = (info.metaData shr 1) and 0x1 == 1

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

	override fun whenPlaced(cell: FreshChunkCell) {
		super.whenPlaced(cell)

		if(!top)
			cell.world.pokeSimple(cell.x, cell.y + 1, cell.z, upperPart, -1, -1, cell.metaData)
	}

    override fun onRemove(cell: ChunkCell, cause: WorldModificationCause?) {
        // Don't interfere with system pokes, else we get stuck in a loop
        if (cause !is Entity)
            return

        val world = cell.world
        val x = cell.x
        val y = cell.y
        val z = cell.z

        // Ignore all that crap on a slave world
        if (world !is WorldMaster)
            return

        var otherPartOfTheDoorY = y

        if (top)
            otherPartOfTheDoorY--
        else
            otherPartOfTheDoorY++

        val restOfTheDoorVoxel = world.peekSimple(x, otherPartOfTheDoorY, z)
        // Remove the other part as well, if it still exists
        if (restOfTheDoorVoxel is VoxelDoor)
            world.pokeSimple(x, otherPartOfTheDoorY, z, store.air, -1, -1, 0)

    }

    companion object {
        fun computeMeta(isOpen: Boolean, hingeSide: Boolean, doorFacingSide: VoxelSide): Int {
            return computeMeta(isOpen, hingeSide, doorFacingSide.ordinal)
        }

        private fun computeMeta(isOpen: Boolean, hingeSide: Boolean, doorFacingsSide: Int): Int {
            // System.out.println(doorFacingsSide + " open: " + isOpen + " hinge:" +
            // hingeSide);
            return (doorFacingsSide shl 2) or (((if (hingeSide) 1 else 0) and 0x1) shl 1) or ((if (isOpen) 1 else 0) and 0x1)
        }
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		if(top)
			return emptyList()

		return listOf(ItemDefinition(itemStore, name, Json.Dict(mapOf(
				"voxel" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemDoor::class.java.canonicalName!!),
				"slotsHeight" to Json.Value.Number(2.0)
		))))
    }
}

class ItemDoor(definition: ItemDefinition) : ItemVoxel(definition) {
	val door = super.voxel as VoxelDoor

	override fun buildRepresentation(worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
		val representation = ModelInstance(door.model, ModelPosition(worldPosition).apply {
			matrix.translate(-0.4f, -0.4f, -0.0f)
		}, door.mappedOverrides)
		representationsGobbler.acceptRepresentation(representation, -1)
	}

	override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
        super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)

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
        val doorSideFacing: VoxelSide

        val loc = placingEntity.location
        val dx = loc.x() - (x + 0.5)
        val dz = loc.z() - (z + 0.5)
        if (Math.abs(dx) > Math.abs(dz)) {
            if (dx > 0)
                doorSideFacing = VoxelSide.RIGHT
            else
                doorSideFacing = VoxelSide.LEFT
        } else {
            if (dz > 0)
                doorSideFacing = VoxelSide.FRONT
            else
                doorSideFacing = VoxelSide.BACK
        }

        // If there is an adjacent one, set the hinge to right
        var adjacent: Voxel? = null
        when (doorSideFacing) {
            VoxelSide.LEFT -> adjacent = world.peekSimple(x, y, z - 1)
            VoxelSide.RIGHT -> adjacent = world.peekSimple(x, y, z + 1)
            VoxelSide.FRONT -> adjacent = world.peekSimple(x - 1, y, z)
            VoxelSide.BACK -> adjacent = world.peekSimple(x + 1, y, z)
            else -> {
            }
        }

        hingeSide = adjacent is VoxelDoor

        cell.metaData = VoxelDoor.computeMeta(isOpen, hingeSide, doorSideFacing)

        println("placing door: dx=$dx dz=$dz rslt=$doorSideFacing meta=${cell.metaData}")

        return true
    }
}

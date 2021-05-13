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
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.reverseWindingOrder
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.MutableCellData
import kotlin.math.abs

class VoxelStairs(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	private val model: Model = content.models[definition["model"].asString ?: "voxels/blockmodels/stairs/stairs.dae"]
	private val modelFlipped: Model
	private val mappedOverrides: Map<Int, MeshMaterial>

	init {
		modelFlipped = flipModel(model)

		val overrides = model.meshes.mapIndexedNotNull { i, mesh ->
			val texName = when (mesh.material.name) {
				"FrontMaterial" -> textures[BlockSide.FRONT.ordinal].name
				"BackMaterial" -> textures[BlockSide.BACK.ordinal].name
				"LeftMaterial" -> textures[BlockSide.LEFT.ordinal].name
				"RightMaterial" -> textures[BlockSide.RIGHT.ordinal].name
				"TopMaterial" -> textures[BlockSide.TOP.ordinal].name
				"BottomMaterial" -> textures[BlockSide.BOTTOM.ordinal].name
				else -> return@mapIndexedNotNull null
			}

			Pair(i, MeshMaterial(mesh.material.name, mapOf("albedoTexture" to "voxels/textures/$texName.png")))
		}

		mappedOverrides = overrides.toMap()
	}

	override fun loadRepresentation(): BlockRepresentation {
		return BlockRepresentation.Custom { cell ->
			render(cell, this)
		}
	}

	fun flipModel(model: Model): Model {
		return Model(model.meshes.map { it.reverseWindingOrder() })
	}

	fun render(cell: Cell, mesher: BlockRepresentation.Custom.RenderInterface) {
		val meta = cell.data.extraData
		val rotation = meta % 4 - 1/*when (meta % 4) {
			0 -> 3
			1 -> 1
			2 -> 2
			3 -> 0
			else -> -1
		}*/
		val flipped = (meta and 0x4) != 0

		val matrix = Matrix4f()

		matrix.translate(0.5f, 0.5f, 0.5f)
		matrix.rotate(Math.PI.toFloat() * 0.5f * rotation, 0f, 1f, 0f)
		if (flipped)
			matrix.scale(1f, -1f, 1f)
		matrix.translate(-0.5f, -0.5f, -0.5f)

		if (flipped)
			mesher.addModel(modelFlipped, matrix, mappedOverrides)
		else
			mesher.addModel(model, matrix, mappedOverrides)
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box> {
		val meta = cell.data.extraData
		val boxes = arrayOf(
				Box.fromExtents(1.0, 0.5, 1.0),
				when (meta % 4) {
					0 -> Box.fromExtents(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.0)
					1 -> Box.fromExtents(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.0)
					2 -> Box.fromExtents(0.5, 0.5, 1.0).translate(0.0, -0.0, 0.0)
					3 -> Box.fromExtents(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.5)
					else -> Box.fromExtents(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.25)
				}
		)

		if (meta / 4 == 0) {
			boxes[0].translate(0.0, 0.0, 0.0)
			boxes[1].translate(0.0, 0.5, 0.0)
		} else {
			boxes[0].translate(0.0, 0.5, 0.0)
			boxes[1].translate(0.0, 0.0, 0.0)
		}

		return boxes
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
				"voxel" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemStairs::class.java.canonicalName!!)
		)))

		return listOf(definition)
	}
}

class ItemStairs(definition: ItemDefinition) : ItemBlock(definition) {
	override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): MutableCellData {
		val data = super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)!!

		val loc = placingEntity.location
		val dx = hit.hitPosition.x() - loc.x()
		val dz = hit.hitPosition.z() - loc.z()

		val facing = if (abs(dx) > abs(dz)) {
			if (dx > 0)
				BlockSide.LEFT
			else
				BlockSide.RIGHT
		} else {
			if (dz > 0)
				BlockSide.BACK
			else
				BlockSide.FRONT
		}

		val flipped = if(adjacentCellSide == BlockSide.BOTTOM)
			true
		else if(adjacentCellSide == BlockSide.TOP)
			false
		else
			(hit.hitPosition.y() % 1) > 0.5

		data.extraData = facing.ordinal or ((if(flipped) 1 else 0) shl 2)

		return data
	}
}
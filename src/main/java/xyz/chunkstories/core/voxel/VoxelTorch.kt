//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import org.joml.Vector4f
import xyz.chunkstories.api.block.BlockRepresentation
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.MiningTool
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.representation.PointLight
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.util.kotlin.toVec3d
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.MutableCellData
import xyz.chunkstories.api.world.chunk.MutableChunkCell

class VoxelTorch(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	internal var mappedOverrides: Map<Int, MeshMaterial>
	internal var model: Model = content.models[definition["model"].asString ?: "voxels/blockmodels/torch/torch.dae"]

	init {

		val overrides = model.meshes.mapIndexedNotNull { i, mesh ->
			val texName = when (mesh.material.name) {
				"TorchMaterial" -> textures[BlockSide.FRONT.ordinal].name
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

	fun render(cell: Cell, mesher: BlockRepresentation.Custom.RenderInterface) {
		val placedOnSide = BlockSide.values()[cell.data.extraData]
		val matrix = Matrix4f()
		matrix.translate(0.5f, 0.0f, 0.5f)

		when (placedOnSide) {
			BlockSide.LEFT -> {
				matrix.rotate(2 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			BlockSide.FRONT -> {
				matrix.rotate(3 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			BlockSide.RIGHT -> {
				matrix.rotate(0 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			BlockSide.BACK -> {
				matrix.rotate(1 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			BlockSide.TOP -> {
			}
			BlockSide.BOTTOM -> {
			}
		}

		mesher.addModel(model, matrix, mappedOverrides)
	}

	override fun getCollisionBoxes(cell: Cell): Array<Box> {
		when (val side = cell.data.extraData) {
			BlockSide.TOP.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.125).translate(0.5, 0.0, 0.5))
			BlockSide.LEFT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.2, 0.6, 0.125).translate(1.0 - 0.2 * 0.5, 0.3, 0.5))
			BlockSide.RIGHT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.2, 0.6, 0.125).translate(0.2 * 0.5, 0.3, 0.5))
			BlockSide.FRONT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.2).translate(0.5, 0.3, 0.2 * 0.5))
			BlockSide.BACK.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.2).translate(0.5, 0.3, 1.0 - 0.2 * 0.5))
		}

		return super.getCollisionBoxes(cell)
	}

	override fun tick(cell: MutableChunkCell) {
		val side = BlockSide.values()[cell.data.extraData]
		val adjacent = cell.world.getCell(cell.x - side.dx, cell.y - side.dy, cell.z - side.dz) ?: return
		if(!adjacent.data.blockType.solid || !adjacent.data.blockType.opaque) {
			cell.data.blockType.breakBlock(cell, null, TorchVoidPop)
		}
	}

	object TorchVoidPop : MiningTool {
		override val miningEfficiency = Float.POSITIVE_INFINITY
		override val toolTypeName = "world"
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val map = mutableMapOf<String, Json>(
				"voxel" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemTorch::class.java.canonicalName!!))

		val additionalItems = definition["itemProperties"].asDict?.elements
		if(additionalItems != null)
			map.putAll(additionalItems)

		val definition = ItemDefinition(itemStore, name, Json.Dict(map))

		return listOf(definition)
	}
}

class ItemTorch(definition: ItemDefinition) : ItemBlock(definition) {
	override fun buildRepresentation(worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
		val torchVoxel = (blockType as VoxelTorch)

		val representation = ModelInstance(torchVoxel.model, ModelPosition(worldPosition).apply {
			matrix.translate(-0.4f, -0.4f, -0.0f)
		}, torchVoxel.mappedOverrides)
		representationsGobbler.acceptRepresentation(representation, -1)

		val position = Vector4f(0.3f, 1f, -0.5f, 1f)
		worldPosition.transform(position)
		if (blockType.emittedLightLevel > 0) {
			val light = PointLight(position.toVec3f().toVec3d().add(0.0, 0.0, 0.0), blockType.textures[0].averagedColor.toVec3f().toVec3d().mul(blockType.emittedLightLevel.toDouble()))
			representationsGobbler.acceptRepresentation(light)
		}
	}

	override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): MutableCellData? {
		if (!adjacentCell.data.blockType.solid || !adjacentCell.data.blockType.opaque || adjacentCellSide == BlockSide.BOTTOM)
			return null

		val data = super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)!!
		data.extraData = adjacentCellSide.ordinal
		return data
	}
}
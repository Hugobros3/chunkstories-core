//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import org.joml.Vector4f
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.representation.PointLight
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.util.kotlin.toVec3d
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.voxel.*
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.DummyCell
import xyz.chunkstories.api.world.cell.EditableCell
import xyz.chunkstories.api.world.cell.FutureCell

class VoxelTorch(definition: VoxelDefinition) : Voxel(definition) {
	internal var mappedOverrides: Map<Int, MeshMaterial>
	internal var model: Model

	init {
		model = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/torch/torch.dae"]

		val overrides = model.meshes.mapIndexedNotNull { i, mesh ->
			val texName = when (mesh.material.name) {
				"TorchMaterial" -> voxelTextures[VoxelSide.FRONT.ordinal].name
				else -> return@mapIndexedNotNull null
			}

			Pair(i, MeshMaterial(mesh.material.name, mapOf("albedoTexture" to "voxels/textures/$texName.png")))
		}

		mappedOverrides = overrides.toMap()

		customRenderingRoutine = { cell ->
			render(cell, this)
		}
	}

	fun render(cell: Cell, mesher: ChunkMeshRenderingInterface) {
		val placedOnSide = VoxelSide.values()[cell.metaData]
		val matrix = Matrix4f()
		matrix.translate(0.5f, 0.0f, 0.5f)

		when (placedOnSide) {
			VoxelSide.LEFT -> {
				matrix.rotate(2 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			VoxelSide.FRONT -> {
				matrix.rotate(3 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			VoxelSide.RIGHT -> {
				matrix.rotate(0 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			VoxelSide.BACK -> {
				matrix.rotate(1 * Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.55f, 0.3f, 0.0f)
				matrix.rotate(-0.4f, 0f, 0f, 1f)
			}
			VoxelSide.TOP -> {
			}
			VoxelSide.BOTTOM -> {
			}
		}

		mesher.addModel(model, matrix, mappedOverrides)
	}

	override fun getCollisionBoxes(info: Cell): Array<Box>? {
		when (val side = info.metaData) {
			VoxelSide.TOP.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.125).translate(0.5, 0.0, 0.5))
			VoxelSide.LEFT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.2, 0.6, 0.125).translate(1.0 - 0.2 * 0.5, 0.3, 0.5))
			VoxelSide.RIGHT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.2, 0.6, 0.125).translate(0.2 * 0.5, 0.3, 0.5))
			VoxelSide.FRONT.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.2).translate(0.5, 0.3, 0.2 * 0.5))
			VoxelSide.BACK.ordinal -> return arrayOf(Box.fromExtentsCenteredHorizontal(0.125, 0.6, 0.2).translate(0.5, 0.3, 1.0 - 0.2 * 0.5))
		}

		return super.getCollisionBoxes(info)
	}

	override fun tick(cell: EditableCell) {
		val side = VoxelSide.values()[cell.metaData]
		val adjacent = cell.world.peek(cell.x - side.dx, cell.y - side.dy, cell.z - side.dz)
		if(!adjacent.voxel.solid || !adjacent.voxel.opaque) {
			cell.voxel.breakBlock(cell, TorchVoidPop, null)
		}
	}

	object TorchVoidPop : MiningTool {
		override val miningEfficiency = Float.POSITIVE_INFINITY
		override val toolTypeName = "world"
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
				"voxel" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemTorch::class.java.canonicalName!!)
		)))

		return listOf(definition)
	}
}

class ItemTorch(definition: ItemDefinition) : ItemVoxel(definition) {
	override fun buildRepresentation(worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
		val torchVoxel = (voxel as VoxelTorch)

		val representation = ModelInstance(torchVoxel.model, ModelPosition(worldPosition).apply {
			matrix.translate(-0.4f, -0.4f, -0.0f)
		}, torchVoxel.mappedOverrides)
		representationsGobbler.acceptRepresentation(representation, -1)

		val position = Vector4f(0.3f, 1f, -0.5f, 1f)
		worldPosition.transform(position)
		if (voxel.emittedLightLevel > 0) {
			val light = PointLight(position.toVec3f().toVec3d().add(0.0, 0.0, 0.0), voxel.voxelTextures[0].color.toVec3f().toVec3d().mul(voxel.emittedLightLevel.toDouble()))
			representationsGobbler.acceptRepresentation(light)
		}
	}

	override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
		if (!adjacentCell.voxel.solid || !adjacentCell.voxel.opaque || adjacentCellSide == VoxelSide.BOTTOM)
			return false

		super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)
		cell.metaData = adjacentCellSide.ordinal
		return true
	}
}
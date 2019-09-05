//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.graphics.reverseWindingOrder
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.ChunkMeshRenderingInterface
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell

class VoxelStairs(definition: VoxelDefinition) : Voxel(definition) {

	private val model: Model
	private val modelFlipped: Model
	private val mappedOverrides: Map<Int, MeshMaterial>

	init {
		model = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/stairs/stairs.dae"]
		modelFlipped = flipModel(model)

		val overrides = model.meshes.mapIndexedNotNull { i, mesh ->
			val texName = when (mesh.material.name) {
				"FrontMaterial" -> voxelTextures[VoxelSide.FRONT.ordinal].name
				"BackMaterial" -> voxelTextures[VoxelSide.BACK.ordinal].name
				"LeftMaterial" -> voxelTextures[VoxelSide.LEFT.ordinal].name
				"RightMaterial" -> voxelTextures[VoxelSide.RIGHT.ordinal].name
				"TopMaterial" -> voxelTextures[VoxelSide.TOP.ordinal].name
				"BottomMaterial" -> voxelTextures[VoxelSide.BOTTOM.ordinal].name
				else -> return@mapIndexedNotNull null
			}

			Pair(i, MeshMaterial(mesh.material.name, mapOf("albedoTexture" to "voxels/textures/$texName.png")))
		}

		mappedOverrides = overrides.toMap()

		customRenderingRoutine = { cell ->
			render(cell, this)
		}
	}

	fun flipModel(model: Model): Model {
		return Model(model.meshes.map { it.reverseWindingOrder() })
	}

	fun render(cell: Cell, mesher: ChunkMeshRenderingInterface) {
		val meta = cell.metaData
		val rotation = when (meta % 4) {
			0 -> 3
			1 -> 1
			2 -> 2
			3 -> 0
			else -> -1
		}
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

	override fun getCollisionBoxes(info: Cell): Array<Box>? {
		val meta = info.metaData
		val boxes = arrayOf(
				Box.fromExtents(1.0, 0.5, 1.0),
				when (meta % 4) {
					0 -> Box.fromExtents(0.5, 0.5, 1.0).translate(0.5, -0.0, 0.0)
					1 -> Box.fromExtents(0.5, 0.5, 1.0).translate(0.0, -0.0, 0.0)
					2 -> Box.fromExtents(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.5)
					3 -> Box.fromExtents(1.0, 0.5, 0.5).translate(0.0, -0.0, 0.0)
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

	fun onPlace(cell: FutureCell, cause: WorldModificationCause?) {
		// id+dir of slope
		// 0LEFT x-
		// 1RIGHT x+
		// 2BACK z-
		// 3FRONT z+

		if (cause is Entity) {
			var stairsSide: Int

			val entity = cause as Entity?
			val loc = entity!!.location
			val dx = loc.x() - (cell.x + 0.5)
			val dz = loc.z() - (cell.z + 0.5)

			// System.out.println("dx: "+dx+" dz:" + dz);

			stairsSide = if (Math.abs(dx) > Math.abs(dz)) {
				if (dx > 0)
					1
				else
					0
			} else {
				if (dz > 0)
					3
				else
					2
			}

			if (entity.traits[TraitRotation::class]?.verticalRotation ?: 0f > 0.0f)
			//if (entity.traits.tryWithBoolean<TraitRotation>(TraitRotation::class.java) { er -> er.verticalRotation < 0 })
				stairsSide += 4

			cell.metaData = stairsSide
		}
	}
}

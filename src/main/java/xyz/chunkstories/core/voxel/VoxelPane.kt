//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Matrix4f
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.ChunkMeshRenderingInterface
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

class VoxelPane(definition: VoxelDefinition) : Voxel(definition) {

	private val baseModel: Model
	private val backPartMode: Model
	private val frontPartModel: Model
	private val mappedOverrides = mapOf(0 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")),
			1 to MeshMaterial("door_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

	init {
		baseModel = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/glass_pane/glass_pane.dae"]
		backPartMode = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/glass_pane/glass_pane_back_half.dae"]
		frontPartModel = definition.store.parent.models[definition["model"].asString ?: "voxels/blockmodels/glass_pane/glass_pane_front_half.dae"]

		customRenderingRoutine = { cell ->
			render(cell, this)
		}
	}

	fun render(cell: Cell, mesher: ChunkMeshRenderingInterface) {
		var vox: Voxel?
		vox = cell.getNeightborVoxel(0)
		val connectLeft = vox!!.solid && vox.opaque || vox == this
		vox = cell.getNeightborVoxel(1)
		val connectFront = vox!!.solid && vox.opaque || vox == this
		vox = cell.getNeightborVoxel(2)
		val connectRight = vox!!.solid && vox.opaque || vox == this
		vox = cell.getNeightborVoxel(3)
		val connectBack = vox!!.solid && vox.opaque || vox == this

		fun arity(b: Boolean): Int = if (b) 1 else 0

		val arity = arity(connectLeft) + arity(connectRight) + arity(connectFront) + arity((connectBack))

		when {
			arity == 0 -> {
				mesher.addModel(baseModel, materialsOverrides = mappedOverrides)
				val matrix = Matrix4f()
				matrix.translate(0.5f, 0f, 0.5f)
				matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
				matrix.translate(-0.5f, 0f, -0.5f)
				mesher.addModel(baseModel, matrix, mappedOverrides)
				return
			}
			arity == 2 -> {
				if (connectBack && connectFront) {
					mesher.addModel(baseModel, materialsOverrides = mappedOverrides)
					return
				} else if (connectLeft && connectRight) {
					val matrix = Matrix4f()
					matrix.translate(0.5f, 0f, 0.5f)
					matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
					matrix.translate(-0.5f, 0f, -0.5f)
					mesher.addModel(baseModel, matrix, mappedOverrides)
					return
				}
			}
		}

		if (connectBack) {
			val matrix = Matrix4f()
			matrix.translate(0.5f, 0f, 0.5f)
			//matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
			matrix.translate(-0.5f, 0f, -0.5f)
			mesher.addModel(backPartMode, materialsOverrides = mappedOverrides)
		}
		if (connectFront) {
			val matrix = Matrix4f()
			matrix.translate(0.5f, 0f, 0.5f)
			//matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
			matrix.translate(-0.5f, 0f, -0.5f)
			mesher.addModel(frontPartModel, materialsOverrides = mappedOverrides)
		}
		if (connectLeft) {
			val matrix = Matrix4f()
			matrix.translate(0.5f, 0f, 0.5f)
			matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
			matrix.translate(-0.5f, 0f, -0.5f)
			mesher.addModel(backPartMode, matrix, mappedOverrides)
		}
		if (connectRight) {
			val matrix = Matrix4f()
			matrix.translate(0.5f, 0f, 0.5f)
			matrix.rotate(Math.PI.toFloat() * 0.5f, 0f, 1f, 0f)
			matrix.translate(-0.5f, 0f, -0.5f)
			mesher.addModel(frontPartModel, matrix, mappedOverrides)
		}
	}

	override fun getCollisionBoxes(info: Cell): Array<Box>? {

		var vox: Voxel?
		vox = info.getNeightborVoxel(0)
		val connectLeft = vox!!.solid && vox.opaque || vox == this
		vox = info.getNeightborVoxel(1)
		val connectFront = vox!!.solid && vox.opaque || vox == this
		vox = info.getNeightborVoxel(2)
		val connectRight = vox!!.solid && vox.opaque || vox == this
		vox = info.getNeightborVoxel(3)
		val connectBack = vox!!.solid && vox.opaque || vox == this

		val width = 0.1
		val delta1 = 0.45
		val delta2 = 0.55

		val boxes =
				// Cross case
				if (connectLeft && connectFront && connectRight && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1))
				// T cases
				else if (connectLeft && connectFront && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, 0.5).translate(delta1, 0.0, 0.5))
				else if (connectLeft && connectFront && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(0.5, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectLeft && connectBack && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, 0.5).translate(delta1, 0.0, 0.0))
				else if (connectBack && connectFront && connectRight)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0), Box.fromExtents(0.5, 1.0, width).translate(0.5, 0.0, delta1))
				// Line cases
				else if (connectLeft && connectRight)
					arrayOf(Box.fromExtents(1.0, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectFront && connectBack)
					arrayOf(Box.fromExtents(width, 1.0, 1.0).translate(delta1, 0.0, 0.0))
				// Corner cases
				else if (connectLeft && connectBack)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else if (connectRight && connectBack)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else if (connectLeft && connectFront)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				else if (connectRight && connectFront)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1), Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				// Lone cases
				else if (connectLeft)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(0.0, 0.0, delta1))
				else if (connectRight)
					arrayOf(Box.fromExtents(delta2, 1.0, width).translate(delta1, 0.0, delta1))
				else if (connectFront)
					arrayOf(Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, delta1))
				else if (connectBack)
					arrayOf(Box.fromExtents(width, 1.0, delta2).translate(delta1, 0.0, 0.0))
				else
					arrayOf(Box.fromExtents(width, 1.0, width).translate(delta1, 0.0, delta1))

		return boxes
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

class VoxelFence(type: VoxelDefinition) : Voxel(type) {

	val post: Model
	val beams: Array<Model>

	init {
		post = definition.store.parent.models["voxels/blockmodels/fence/fence_post.dae"]
		beams = VoxelSide.values().copyOfRange(0, 4).map { definition.store.parent.models["voxels/blockmodels/fence/fence_${it.name.toLowerCase()}.dae"] }.toTypedArray()

		val mappedOverrides = mapOf(0 to MeshMaterial("fence_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

		customRenderingRoutine = { cell ->
			addModel(post, materialsOverrides = mappedOverrides)

			var vox: Voxel?
			vox = cell.getNeightborVoxel(0)
			val connectLeft = vox != null && ((vox.solid && vox.opaque) || vox == this@VoxelFence)
			vox = cell.getNeightborVoxel(1)
			val connectFront = vox != null && ((vox.solid && vox.opaque) || vox == this@VoxelFence)
			vox = cell.getNeightborVoxel(2)
			val connectRight = vox != null && ((vox.solid && vox.opaque) || vox == this@VoxelFence)
			vox = cell.getNeightborVoxel(3)
			val connectBack = vox != null && ((vox.solid && vox.opaque) || vox == this@VoxelFence)

			if (connectLeft)
				addModel(beams[VoxelSide.LEFT.ordinal], materialsOverrides = mappedOverrides)
			if (connectRight)
				addModel(beams[VoxelSide.RIGHT.ordinal], materialsOverrides = mappedOverrides)
			if (connectFront)
				addModel(beams[VoxelSide.FRONT.ordinal], materialsOverrides = mappedOverrides)
			if (connectBack)
				addModel(beams[VoxelSide.BACK.ordinal], materialsOverrides = mappedOverrides)
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

		val width = 0.4
		val delta1 = 0.3
		val delta2 = 0.7

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

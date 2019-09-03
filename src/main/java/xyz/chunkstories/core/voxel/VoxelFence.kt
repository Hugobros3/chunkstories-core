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

	/*@Override
	public VoxelModel getVoxelRenderer(CellData info) {
		Voxel vox;
		vox = info.getNeightborVoxel(0);
		boolean connectLeft = (vox.isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(1);
		boolean connectFront = (vox.isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(2);
		boolean connectRight = (vox.isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(3);
		boolean connectBack = (vox.isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);

		String type = "default";
		if (connectLeft && connectFront && connectRight && connectBack)
			type = "allDir";
		else if (connectLeft && connectFront && connectRight)
			type = "allButBack";
		else if (connectLeft && connectFront && connectBack)
			type = "allButRight";
		else if (connectLeft && connectBack && connectRight)
			type = "allButFront";
		else if (connectBack && connectFront && connectRight)
			type = "allButLeft";
		else if (connectLeft && connectRight)
			type = "allX";
		else if (connectFront && connectBack)
			type = "allZ";
		else if (connectLeft && connectBack)
			type = "leftBack";
		else if (connectRight && connectBack)
			type = "rightBack";
		else if (connectLeft && connectFront)
			type = "leftFront";
		else if (connectRight && connectFront)
			type = "rightFront";
		else if (connectLeft)
			type = "left";
		else if (connectRight)
			type = "right";
		else if (connectFront)
			type = "front";
		else if (connectBack)
			type = "back";

		return store.models().getVoxelModel("wood_fence" + "." + type);
	}*/

	val post: Model
	val beams: Array<Model>

	init {
		post = definition.store.parent.models["voxels/blockmodels/fence/fence_post.dae"]
		beams = VoxelSide.values().copyOfRange(0, 4).map { definition.store.parent.models["voxels/blockmodels/fence/fence_${it.name.toLowerCase()}.dae"] }.toTypedArray()

		/*val overrides = top.meshes.mapIndexedNotNull { i, mesh ->
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

		val mappedOverrides = overrides.toMap()*/
		val mappedOverrides = mapOf(0 to MeshMaterial("fence_material", mapOf("albedoTexture" to "voxels/textures/${this.voxelTextures[VoxelSide.FRONT.ordinal].name}.png")))

		customRenderingRoutine = { cell ->
			/*if (bottomOrTop(cell.metaData))
				addModel(bottom, materialsOverrides = mappedOverrides)
			else
				addModel(top, materialsOverrides = mappedOverrides)*/
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

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
import xyz.chunkstories.api.world.cell.CellData

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
        post = definition.store.parent().models["voxels/blockmodels/fence/fence_post.dae"]
        beams = VoxelSide.values().copyOfRange(0, 4).map { definition.store.parent().models["voxels/blockmodels/fence/fence_${it.name.toLowerCase()}.dae"] }.toTypedArray()

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

            if(connectLeft)
                addModel(beams[VoxelSide.LEFT.ordinal], materialsOverrides = mappedOverrides)
            if(connectRight)
                addModel(beams[VoxelSide.RIGHT.ordinal], materialsOverrides = mappedOverrides)
            if(connectFront)
                addModel(beams[VoxelSide.FRONT.ordinal], materialsOverrides = mappedOverrides)
            if(connectBack)
                addModel(beams[VoxelSide.BACK.ordinal], materialsOverrides = mappedOverrides)
        }
    }

    override fun getCollisionBoxes(info: CellData): Array<Box>? {
        // System.out.println("kek");
        var boxes = arrayOf(Box(0.3, 0.0, 0.3, 0.4, 1.0, 0.4))

        var vox: Voxel?
        vox = info.getNeightborVoxel(0)
        val connectLeft = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(1)
        val connectFront = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(2)
        val connectRight = vox!!.solid && vox.opaque || vox == this
        vox = info.getNeightborVoxel(3)
        val connectBack = vox!!.solid && vox.opaque || vox == this

        if (connectLeft && connectFront && connectRight && connectBack) {
            boxes = arrayOf(Box(0.3, 0.0, 0.0, 0.4, 1.0, 1.0), Box(0.0, 0.0, 0.3, 1.0, 1.0, 0.4))
        } else if (connectLeft && connectFront && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.3, 1.0, 1.0, 0.4), Box(0.3, 0.0, 0.25, 0.4, 1.0, 0.5).translate(0.0, 0.0, 0.25))
        else if (connectLeft && connectFront && connectBack)
            boxes = arrayOf(Box(0.3, 0.0, 0.0, 0.4, 1.0, 1.0), Box(0.25, 0.0, 0.3, 0.5, 1.0, 0.4).translate(-0.25, 0.0, 0.0))
        else if (connectLeft && connectBack && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.3, 1.0, 1.0, 0.4), Box(0.3, 0.0, 0.25, 0.4, 1.0, 0.5).translate(0.0, 0.0, -0.25))
        else if (connectBack && connectFront && connectRight)
            boxes = arrayOf(Box(0.3, 0.0, 0.0, 0.4, 1.0, 1.0), Box(0.25, 0.0, 0.3, 0.5, 1.0, 0.4).translate(0.25, 0.0, 0.0))
        else if (connectLeft && connectRight)
            boxes = arrayOf(Box(0.0, 0.0, 0.3, 1.0, 1.0, 0.4))
        else if (connectFront && connectBack)
            boxes = arrayOf(Box(0.3, 0.0, 0.0, 0.4, 1.0, 1.0))
        else if (connectLeft && connectBack)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(-0.15, 0.0, 0.0), Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.7).translate(0.0, 0.0, -0.15))
        else if (connectRight && connectBack)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(0.15, 0.0, 0.0), Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.7).translate(0.0, 0.0, -0.15))
        else if (connectLeft && connectFront)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(-0.15, 0.0, 0.0), Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.7).translate(0.0, 0.0, 0.15))
        else if (connectRight && connectFront)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(0.15, 0.0, 0.0), Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.70).translate(0.0, 0.0, 0.15))
        else if (connectLeft)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(-0.15, 0.0, 0.0))
        else if (connectRight)
            boxes = arrayOf(Box(0.15, 0.0, 0.3, 0.7, 1.0, 0.4).translate(0.15, 0.0, 0.0))
        else if (connectFront)
            boxes = arrayOf(Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.7).translate(0.0, 0.0, 0.15))
        else if (connectBack)
            boxes = arrayOf(Box(0.3, 0.0, 0.15, 0.4, 1.0, 0.7).translate(0.0, 0.0, -0.15))

        // for (Box box : boxes)
        // box.translate(+0.25, -0, +0.25);

        return boxes
    }
}

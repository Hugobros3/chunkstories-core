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

class VoxelHalfTile(definition: VoxelDefinition) : Voxel(definition) {

    val top: Model
    val bottom: Model

    init {
        top = definition.store.parent.models["voxels/blockmodels/half_step/upper_half.dae"]
        bottom = definition.store.parent.models["voxels/blockmodels/half_step/lower_half.dae"]

        val overrides = top.meshes.mapIndexedNotNull { i, mesh ->
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

        val mappedOverrides = overrides.toMap()

        customRenderingRoutine = { cell ->
            if (bottomOrTop(cell.metaData))
                addModel(bottom, materialsOverrides = mappedOverrides)
            else
                addModel(top, materialsOverrides = mappedOverrides)
        }
    }

    private fun bottomOrTop(meta: Int): Boolean {
        return meta % 2 == 0
    }

    override fun getCollisionBoxes(info: Cell): Array<Box>? {
        // System.out.println("kek");
        val box2 = Box.fromExtents(1.0, 0.5, 1.0)
        if (bottomOrTop(info.metaData))
            box2.translate(0.0, -0.0, 0.0)
        else
            box2.translate(0.0, +0.5, 0.0)
        return arrayOf(box2)
    }

    override fun getLightLevelModifier(dataFrom: Cell, dataTo: Cell, side2: VoxelSide): Int {
        val side = side2.ordinal

        // Special cases when half-tiles meet
        if (dataTo.voxel is VoxelHalfTile && side < 4) {
            // If they are the same type, allow the light to transfer
            return if (bottomOrTop(dataFrom.metaData) == bottomOrTop(dataTo.metaData))
                2
            else
                15
        }
        if (bottomOrTop(dataFrom.metaData) && side == 5)
            return 15
        return if (!bottomOrTop(dataFrom.metaData) && side == 4) 15 else 2
    }
}

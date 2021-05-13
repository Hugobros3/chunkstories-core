package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model

/** Most block models use materials named after one of the six sides of a cube. We want those models to use the actual voxel face textures */
fun BlockType.deriveModelOverridesForFaceTextures(model: Model): Map<Int, MeshMaterial> {
    return model.meshes.mapIndexedNotNull { i1, mesh ->
        val texName = when (mesh.material.name) {
            "FrontMaterial" -> textures[BlockSide.FRONT.ordinal].name
            "BackMaterial" -> textures[BlockSide.BACK.ordinal].name
            "LeftMaterial" -> textures[BlockSide.LEFT.ordinal].name
            "RightMaterial" -> textures[BlockSide.RIGHT.ordinal].name
            "TopMaterial" -> textures[BlockSide.TOP.ordinal].name
            "BottomMaterial" -> textures[BlockSide.BOTTOM.ordinal].name
            else -> return@mapIndexedNotNull null
        }

        Pair(i1, MeshMaterial(mesh.material.name, mapOf("albedoTexture" to "voxels/textures/$texName.png")))
    }.toMap()
}
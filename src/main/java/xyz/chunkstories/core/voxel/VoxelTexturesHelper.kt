package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.api.voxel.Voxel

/** Most block models use materials named after one of the six sides of a cube. We want those models to use the actual voxel face textures */
fun Voxel.deriveModelOverridesForFaceTextures(model: Model): Map<Int, MeshMaterial> {
    return model.meshes.mapIndexedNotNull { i1, mesh ->
        val texName = when (mesh.material.name) {
            "FrontMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.FRONT.ordinal].name
            "BackMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.BACK.ordinal].name
            "LeftMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.LEFT.ordinal].name
            "RightMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.RIGHT.ordinal].name
            "TopMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.TOP.ordinal].name
            "BottomMaterial" -> voxelTextures[xyz.chunkstories.api.voxel.VoxelSide.BOTTOM.ordinal].name
            else -> return@mapIndexedNotNull null
        }

        Pair(i1, MeshMaterial(mesh.material.name, mapOf("albedoTexture" to "voxels/textures/$texName.png")))
    }.toMap()
}
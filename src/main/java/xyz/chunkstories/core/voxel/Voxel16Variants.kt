//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.inventory.ItemPile
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.cell.DummyCell
import xyz.chunkstories.api.world.cell.FutureCell

class Voxel16Variants(definition: VoxelDefinition) : Voxel(definition) {
    private val textures: Array<VoxelTexture>// = arrayOfNulls<VoxelTexture>(16)

    init {
        val variantsString = definition.resolveProperty("variants", "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15")

        val split = variantsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        assert(split.size == 16)

        /*for (i in 0..15) {
            val variants = arrayOfNulls<String>(16)
            variants[i] = split[i].replace(" ".toRegex(), "")
            textures[i] = store().textures().get(definition.resolveProperty("texture", definition.name) + "/" + variants[i])
        }*/

        textures = (0..15).map { i ->
            val variant = split[i].replace(" ".toRegex(), "").trim()
            store.textures.get(definition.resolveProperty("texture", definition.name) + "/" + variant)
        }.toTypedArray()
    }

    override fun getVoxelTexture(info: CellData, side: VoxelSide): VoxelTexture {
        return textures[info.metaData]
    }

    override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
        val variantsString = definition.resolveProperty("variants", "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15")
        return variantsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.map { it.trim() }.mapIndexed { i, variant ->
            ItemDefinition(itemStore, "$name.$variant", mapOf(
                    "voxel" to name,
                    "class" to ItemVoxelVariant::class.java.canonicalName,
                    "metaData" to "$i",
                    "variant" to variant
            ))
        }
    }

    override fun getVariant(cell: CellData): ItemDefinition {
        return variants[cell.metaData % variants.size]
    }
}

class ItemVoxelVariant(definition: ItemDefinition) : ItemVoxel(definition) {
    val metadata = definition["metaData"]!!.toInt()
    val variant = definition["variant"]!!

    override fun getTextureName(): String {
        return "voxels/textures/" + voxel.name + "/" + variant + ".png"
    }

    override fun changeBlockData(cell: FutureCell, placingEntity: Entity): Boolean {
        super.changeBlockData(cell, placingEntity)
        cell.metaData = metadata
        return true
    }
}
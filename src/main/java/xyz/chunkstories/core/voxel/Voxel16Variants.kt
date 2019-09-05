//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asArray
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell

class Voxel16Variants(definition: VoxelDefinition) : Voxel(definition) {
	private val textures: Array<VoxelTexture>// = arrayOfNulls<VoxelTexture>(16)
	val variantsString = definition["variants"].asArray!!.elements.mapNotNull { it.asString }

	init {
		//val variantsString = definition.resolveProperty("variants", "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15")

	// val split = variantsString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
	// assert(split.size == 16)

		/*for (i in 0..15) {
			val variants = arrayOfNulls<String>(16)
			variants[i] = split[i].replace(" ".toRegex(), "")
			textures[i] = store().textures().get(definition.resolveProperty("texture", definition.name) + "/" + variants[i])
		}*/

		textures = (0..15).map { i ->
			val variant = variantsString[i]//split[i].replace(" ".toRegex(), "").trim()
			store.textures.get(definition["texture"].asString ?: definition.name + "/" + variant)
			//store.textures.get(definition.resolveProperty("texture", definition.name) + "/" + variant)
		}.toTypedArray()
	}

	override fun getVoxelTexture(cell: Cell, side: VoxelSide): VoxelTexture {
		return textures[cell.metaData]
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		//val variantsString = definition.resolveProperty("variants", "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15")
		return variantsString.mapIndexed { i, variant ->
			ItemDefinition(itemStore, "$name.$variant", Json.Dict(mapOf(
					"voxel" to Json.Value.Text(name),
					"class" to Json.Value.Text(ItemVoxelVariant::class.java.canonicalName!!),
					"metaData" to Json.Value.Number(i.toDouble()),
					"variant" to Json.Value.Text(variant)
			)))
		}
	}

	override fun getVariant(cell: Cell): ItemDefinition {
		return variants[cell.metaData % variants.size]
	}
}

class ItemVoxelVariant(definition: ItemDefinition) : ItemVoxel(definition) {
	val metadata = definition["metaData"].asInt ?: 0
	val variant = definition["variant"].asString

	override fun getTextureName(): String {
		return "voxels/textures/" + voxel.name + "/" + variant + ".png"
	}

	override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
		super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)
		cell.metaData = metadata
		return true
	}
}
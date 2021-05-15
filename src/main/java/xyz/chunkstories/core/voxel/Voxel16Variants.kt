//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.*
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData

class Voxel16Variants(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	private val textures_: Array<BlockTexture>
	val variantsString = definition["variants"].asArray!!.elements.mapNotNull { it.asString }

	init {
		textures_ = (0..15).map { i ->
			val variant = variantsString[i]//split[i].replace(" ".toRegex(), "").trim()
			content.blockTypes.getTextureOrDefault(definition["texture"].asString ?: name + "/" + variant)
			//store.textures.get(definition.resolveProperty("texture", definition.name) + "/" + variant)
		}.toTypedArray()
	}

	override fun getTexture(cell: Cell, side: BlockSide): BlockTexture {
		return textures_[cell.data.extraData % 16]
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		return variantsString.mapIndexed { i, variant ->

			val map = mutableMapOf<String, Json>(
					"voxel" to Json.Value.Text(name),
					"class" to Json.Value.Text(ItemVoxelVariant::class.java.canonicalName!!),
					"metaData" to Json.Value.Number(i.toDouble()),
					"variant" to Json.Value.Text(variant)
			)

			val additionalItems = definition["itemProperties"].asDict?.elements
			if(additionalItems != null)
				map.putAll(additionalItems)

			ItemDefinition(itemStore, "$name.$variant", Json.Dict(map))
		}
	}

	override fun getVariant(data: CellData): ItemDefinition {
		return variants[data.extraData % variants.size]
	}
}

class ItemVoxelVariant(definition: ItemDefinition) : ItemBlock(definition) {
	val metadata = definition.properties["metaData"].asInt ?: 0
	val variant = definition.properties["variant"].asString

	override fun getTextureName(): String {
		return "voxels/textures/" + blockType.name + "/" + variant + ".png"
	}

	override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): CellData {
		var data = super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)!!
		data = data.copy(extraData = metadata)
		return data
	}
}
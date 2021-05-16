//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockTexture
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.block.components.BlockInventory
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDroppedItem
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.server.Host
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.MutableChunkCell
import kotlin.math.abs

class VoxelChest(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {
	override fun onInteraction(entity: Entity, cell: MutableChunkCell, input: Input): Boolean {
		if (input.name == "mouse.right" && cell.world is WorldMaster) {

			val controller = entity.controller
			if (controller is Player) {
				val gameInstance = cell.world.gameInstance
				if (entity.location.distance(cell.location) <= 5 && gameInstance is Host) {
					TODO("open inv")
					// also do furnace
					// player.openInventory(getInventory(cell))
				}
			}
		}
		return false
	}

	private fun getInventory(context: ChunkCell): Inventory {
		val component = context.additionalData.get("chestInventory") as BlockInventory
		return component.inventory
	}

	override fun whenPlaced(cell: MutableChunkCell) {
		// Create a new component and insert it into the chunk
		val component = BlockInventory(cell, 10, 6)
		cell.registerAdditionalData("chestInventory", component)
	}

	override fun getTexture(cell: Cell, side: BlockSide): BlockTexture {
		val actualSide = BlockSide.values()[cell.data.extraData]

		return when (side) {
			BlockSide.TOP -> textures[BlockSide.TOP.ordinal]
			BlockSide.BOTTOM -> textures[BlockSide.BOTTOM.ordinal]
			actualSide -> textures[BlockSide.FRONT.ordinal]
			else -> textures[BlockSide.LEFT.ordinal]
		}
	}

	override fun onRemove(cell: MutableChunkCell): Boolean {
		val location = cell.location
		location.add(0.5, 0.5, 0.5)

		val inventoryComponent = cell.additionalData["chestInventory"] as? BlockInventory ?: return true
		for (itemPile in inventoryComponent.inventory.contents) {
			val velocity = Vector3d(Math.random() * 0.125 - 0.0625, 0.1, Math.random() * 0.125 - 0.0625)
			val lootLocation = Location(cell.location)
			lootLocation.add(0.5, 0.5, 0.5)
			EntityDroppedItem.spawn(itemPile.item, itemPile.amount, lootLocation, velocity)
		}

		//inventoryComponent gets nuked by the engine when the cell is wiped after this
		return true
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val map = mutableMapOf<String, Json>(
				"block" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemChest::class.java.canonicalName!!))

		val additionalItems = definition["itemProperties"].asDict?.elements
		if(additionalItems != null)
			map.putAll(additionalItems)

		val definition = ItemDefinition(itemStore, name, Json.Dict(map))

		return listOf(definition)
	}
}

class ItemChest(definition: ItemDefinition) : ItemBlock(definition) {
	override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): CellData {
		var data = super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)!!

		val loc = placingEntity.location
		val dx = hit.hitPosition.x() - loc.x()
		val dz = hit.hitPosition.z() - loc.z()

		val facing = if (abs(dx) > abs(dz)) {
			if (dx > 0)
				BlockSide.LEFT
			else
				BlockSide.RIGHT
		} else {
			if (dz > 0)
				BlockSide.BACK
			else
				BlockSide.FRONT
		}

		data = data.copy(extraData = facing.ordinal)

		return data
	}
}

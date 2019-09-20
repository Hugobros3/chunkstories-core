//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDroppedItem
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.item.inventory.Inventory
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.voxel.components.VoxelInventoryComponent
import xyz.chunkstories.api.voxel.textures.VoxelTexture
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell
import kotlin.math.abs

class VoxelChest(type: VoxelDefinition) : Voxel(type) {
	override fun handleInteraction(entity: Entity, voxelContext: ChunkCell, input: Input): Boolean {
		if (input.name == "mouse.right" && voxelContext.world is WorldMaster) {

			val controller = entity.traits[TraitControllable::class]?.controller
			if (controller is Player) {
				val player = controller as Player?
				val playerEntity = player!!.controlledEntity

				if (playerEntity != null) {
					if (playerEntity.location.distance(voxelContext.location) <= 5) {
						player.openInventory(getInventory(voxelContext))
					}
				}
			}
		}
		return false
	}

	private fun getInventory(context: ChunkCell): Inventory {
		val component = context.components.getVoxelComponent("chestInventory") as VoxelInventoryComponent?
		return component!!.inventory
	}

	override fun whenPlaced(cell: FreshChunkCell) {
		// Create a new component and insert it into the chunk
		val component = VoxelInventoryComponent(cell.components, 10, 6)
		cell.registerComponent("chestInventory", component)
	}

	override fun getVoxelTexture(cell: Cell, side: VoxelSide): VoxelTexture {
		val actualSide = VoxelSide.values()[cell.metaData]

		if (side == VoxelSide.TOP)
			return voxelTextures[VoxelSide.TOP.ordinal]
		if (side == VoxelSide.BOTTOM)
			return voxelTextures[VoxelSide.BOTTOM.ordinal]

		return if (side == actualSide) voxelTextures[VoxelSide.FRONT.ordinal] else voxelTextures[VoxelSide.LEFT.ordinal]
	}

	override fun onRemove(cell: ChunkCell, cause: WorldModificationCause?) {
		val location = cell.location
		location.add(0.5, 0.5, 0.5)

		val inventoryComponent = cell.components.getVoxelComponent("chestInventory") as? VoxelInventoryComponent ?: return
		for (itemPile in inventoryComponent.inventory.contents) {
			val velocity = Vector3d(Math.random() * 0.125 - 0.0625, 0.1, Math.random() * 0.125 - 0.0625)
			val lootLocation = Location(cell.location)
			lootLocation.add(0.5, 0.5, 0.5)
			EntityDroppedItem.spawn(itemPile.item, itemPile.amount, lootLocation, velocity)
		}

		//inventoryComponent gets nuked by the engine when the cell is wiped after this
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val definition = ItemDefinition(itemStore, name, Json.Dict(mapOf(
				"voxel" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemChest::class.java.canonicalName!!)
		)))

		return listOf(definition)
	}
}

class ItemChest(definition: ItemDefinition) : ItemVoxel(definition) {
	override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
		super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)

		val loc = placingEntity.location
		val dx = (cell.x + 0.5) - loc.x()
		val dz = (cell.z + 0.5) - loc.z()

		val facing = if (abs(dx) > abs(dz)) {
			if (dx > 0)
				VoxelSide.LEFT
			else
				VoxelSide.RIGHT
		} else {
			if (dz > 0)
				VoxelSide.BACK
			else
				VoxelSide.FRONT
		}

		cell.metaData = facing.ordinal

		return true
	}
}

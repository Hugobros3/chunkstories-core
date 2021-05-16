//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.core.voxel.components.SignData
import xyz.chunkstories.api.block.BlockSide
import xyz.chunkstories.api.block.BlockType
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.item.ItemBlock
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.world.cell.Cell
import xyz.chunkstories.api.world.cell.CellData
import xyz.chunkstories.api.world.chunk.MutableChunkCell

/** Signs are voxels you can write stuff on  */
// TODO implement a gui when placing a sign to actually set the text
// currently only the map converter can make signs have non-default text
// TODO expose the gui to the api to enable this
class VoxelSign(name: String, definition: Json.Dict, content: Content) : BlockType(name, definition, content) {

	override fun onInteraction(entity: Entity, cell: MutableChunkCell, input: Input): Boolean {
		return false
	}

	/* @Override public VoxelDynamicRenderer getVoxelRenderer(CellData info) {
	* return signRenderer; } */

	/*fun onPlace(cell: FutureCell, cause: WorldModificationCause?) {
		// We don't create the components here, as the cell isn't actually changed yet!
		val x = cell.x
		val y = cell.y
		val z = cell.z

		if (cause != null && cause is Entity) {
			val blockLocation = Vector3d(x + 0.5, y.toDouble(), z + 0.5)
			blockLocation.sub((cause as Entity).location)
			blockLocation.negate()

			val direction = Vector2f(blockLocation.x().toFloat(), blockLocation.z().toFloat())
			direction.normalize()
			// System.out.println("x:"+direction.x+"y:"+direction.y);

			var asAngle = Math.acos(direction.y().toDouble()) / Math.PI * 180
			asAngle *= -1.0
			if (direction.x() < 0)
				asAngle *= -1.0

			// asAngle += 180.0;

			asAngle %= 360.0
			asAngle += 360.0
			asAngle %= 360.0

			// System.out.println(asAngle);

			val meta = (16 * asAngle / 360).toInt()
			cell.metaData = meta
		}
	}*/

	override fun whenPlaced(cell: MutableChunkCell) {
		val signTextComponent = SignData(cell)
		signTextComponent.signText = "Random String ${Math.random()}"
		cell.registerAdditionalData("signData", signTextComponent)
	}

	/** Gets the sign component from a chunk cell, assuming it is indeed a sign cell */
	fun getSignData(cell: ChunkCell): SignData {
		return cell.additionalData["signData"] as SignData
	}

	override fun enumerateVariants(itemStore: Content.ItemsDefinitions): List<ItemDefinition> {
		val map = mutableMapOf<String, Json>(
				"block" to Json.Value.Text(name),
				"class" to Json.Value.Text(ItemSign::class.java.canonicalName!!)
		)

		val additionalItems = definition["itemProperties"].asDict?.elements
		if (additionalItems != null)
			map.putAll(additionalItems)

		val definition = ItemDefinition(itemStore, name, Json.Dict(map))

		return listOf(definition)
	}
}

class ItemSign(definition: ItemDefinition) : ItemBlock(definition) {
	override fun prepareNewBlockData(adjacentCell: Cell, adjacentCellSide: BlockSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): CellData? {
		return super.prepareNewBlockData(adjacentCell, adjacentCellSide, placingEntity, hit)
	}
}
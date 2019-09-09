//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.events.voxel.WorldModificationCause
import xyz.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.VoxelDefinition
import xyz.chunkstories.api.world.cell.FutureCell
import xyz.chunkstories.api.world.chunk.ChunkCell
import xyz.chunkstories.api.world.chunk.FreshChunkCell
import xyz.chunkstories.core.voxel.components.VoxelComponentSignText
import org.joml.Vector2f
import org.joml.Vector3d
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.item.ItemVoxel
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.cell.Cell

/** Signs are voxels you can write stuff on  */
// TODO implement a gui when placing a sign to actually set the text
// currently only the map converter can make signs have non-default text
// TODO expose the gui to the api to enable this
class VoxelSign(definition: VoxelDefinition) : Voxel(definition) {

	override fun handleInteraction(entity: Entity, voxelContext: ChunkCell, input: Input): Boolean {
		return false
	}

	/* @Override public VoxelDynamicRenderer getVoxelRenderer(CellData info) {
	* return signRenderer; } */

	@Throws(IllegalBlockModificationException::class)
	fun onPlace(cell: FutureCell, cause: WorldModificationCause?) {
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
	}

	override fun whenPlaced(cell: FreshChunkCell) {
		val signTextComponent = VoxelComponentSignText(cell.components)
		signTextComponent.signText = "Random String ${Math.random()}"
		cell.registerComponent("signData", signTextComponent)
	}

	/** Gets the sign component from a chunkcell, assuming it is indeed a sign
	* cell  */
	fun getSignData(context: ChunkCell): VoxelComponentSignText {
		return context.components.getVoxelComponent("signData") as VoxelComponentSignText
	}
}

class ItemSign(definition: ItemDefinition) : ItemVoxel(definition) {
	override fun prepareNewBlockData(cell: FutureCell, adjacentCell: Cell, adjacentCellSide: VoxelSide, placingEntity: Entity, hit: RayResult.Hit.VoxelHit): Boolean {
		return super.prepareNewBlockData(cell, adjacentCell, adjacentCellSide, placingEntity, hit)
	}
}
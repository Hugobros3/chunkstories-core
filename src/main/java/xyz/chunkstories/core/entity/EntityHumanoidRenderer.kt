//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector4f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.entity.traits.TraitAnimated
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.representation.drawCube
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.core.entity.traits.TraitMining

open class EntityHumanoidRenderer(entity: EntityHumanoid, private val customSkin: MeshMaterial? = null) : TraitRenderable<EntityHumanoid>(entity) {

	override fun buildRepresentation(representationsGobbler: RepresentationsGobbler) {
		val model = entity.world.content.models["./models/human/human.dae"]

		val matrix = Matrix4f()
		matrix.translate(entity.location.toVec3f())
		val position = ModelPosition(matrix)

		val isPlayerEntity = (entity.world.gameContext as? Client)?.ingame?.player?.controlledEntity == this.entity

		var visibility = 0
		for (i in 0 until representationsGobbler.renderTaskInstances.size) {
			val isPassShadow = representationsGobbler.renderTaskInstances[i].name.startsWith("shadow")

			val ithBit = isPassShadow or !isPlayerEntity
			visibility = visibility or (ithBit.toInt() shl i)
		}

		val materials = if (customSkin != null)
			model.meshes.mapIndexed { index, _ -> index }.associateWith { customSkin!! }
		else
			emptyMap()

		val animator = entity.traits[TraitAnimated::class]?.animatedSkeleton !!
		val modelInstance = ModelInstance(model, position, materials, visibility, animator = animator)

		representationsGobbler.acceptRepresentation(modelInstance, visibility)

		val itemInHand = entity.traits[TraitSelectedItem::class]?.selectedItem// ?: (entity.world.gameContext as? Client)?.ingame?.player?.controlledEntity?.traits?.get(TraitInventory::class)?.getItemPileAt(0, 0)
		if(itemInHand != null) {
			val itemMatrix = Matrix4f(matrix)

			val realWorldTimeTruncated = (System.nanoTime() % 1000_000_000_000)
			val realWorldTimeMs = realWorldTimeTruncated / 1000_000
			val animationTime = (realWorldTimeMs / 1000.0) * 1000.0

			itemMatrix.mul(animator.getBoneHierarchyTransformationMatrix("boneItemInHand", animationTime))

			itemInHand.item.buildRepresentation(itemMatrix, representationsGobbler)
		}

		if(isPlayerEntity) {
			val selectionColor = Vector4f(1f)
			val selectedBlock = entity.traits[TraitSight::class]?.getSelectableBlockLookingAt(5.0)?.location

			val miningProgress = entity.traits[TraitMining::class]?.progress
			if(miningProgress != null) {
				selectionColor.set(1.0f, 0.0f, 0.0f, 1.0f)
				selectionColor.y = 1f - miningProgress.progress.toFloat().coerceAtMost(1.0f)
			}

			//selectionColor.set(1.0f, Float.POSITIVE_INFINITY, 0.0f, 1.0f)

			if(selectedBlock != null) {
				val cell = entity.world.peek(selectedBlock)
				val boxes = cell.voxel.getTranslatedCollisionBoxes(cell)
				if(boxes != null) {
					for(box in boxes) {
						representationsGobbler.drawCube(box.min, box.max, selectionColor)
					}
				} else {
					representationsGobbler.drawCube(Vector3d(selectedBlock), Vector3d(selectedBlock).also { it.add(1.0, 1.0, 1.0) }, selectionColor)
				}
			}
		}
	}

}

private inline fun Boolean.toInt() = if (this) 1 else 0
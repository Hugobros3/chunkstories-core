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
import xyz.chunkstories.api.client.IngameClient
import xyz.chunkstories.api.entity.traits.TraitAnimated
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.representation.drawCube
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.player.entityIfIngame
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.animationTime
import xyz.chunkstories.core.entity.traits.TraitMining

open class EntityHumanoidRenderer(entity: EntityHumanoid, private val customSkin: MeshMaterial? = null) : TraitRenderable<EntityHumanoid>(entity) {

	override fun buildRepresentation(representationsGobbler: RepresentationsGobbler) {
		val model = entity.world.gameInstance.content.models["./models/human/human.dae"]

		val matrix = Matrix4f()
		matrix.translate(entity.location.toVec3f())
		val position = ModelPosition(matrix)

		val isPlayerEntity = (entity.world.gameInstance as? IngameClient)?.player?.entityIfIngame == this.entity

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

			itemMatrix.mul(animator.getBoneHierarchyTransformationMatrix("boneItemInHand", entity.world.animationTime))

			itemInHand.item.buildRepresentation(itemMatrix, representationsGobbler)
		}

		if(isPlayerEntity) {
			val selectionColor = Vector4f(1f)
			val selectedBlock = entity.traits[TraitSight::class]?.getSelectableBlockLookingAt(5.0)

			val miningProgress = entity.traits[TraitMining::class]?.progress
			if(miningProgress != null) {
				miningProgress.represent(representationsGobbler)
				//selectionColor.set(1.0f, 0.0f, 0.0f, 1.0f)
				//selectionColor.y = 1f - miningProgress.progress.toFloat().coerceAtMost(1.0f)
				//
			} else {
				if(selectedBlock != null) {
					val boxes = selectedBlock.data.blockType.getTranslatedCollisionBoxes(selectedBlock)
					for(box in boxes) {
						representationsGobbler.drawCube(box.min, box.max, selectionColor)
					}
				}
			}

			//selectionColor.set(1.0f, Float.POSITIVE_INFINITY, 0.0f, 1.0f)


		}
	}

}

private inline fun Boolean.toInt() = if (this) 1 else 0
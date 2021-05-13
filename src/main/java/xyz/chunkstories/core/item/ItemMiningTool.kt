//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item

import org.joml.Matrix4f
import xyz.chunkstories.api.block.MiningTool
import xyz.chunkstories.api.content.json.asFloat
import xyz.chunkstories.api.content.json.asInt
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.item.Item
import xyz.chunkstories.api.item.ItemDefinition
import xyz.chunkstories.api.graphics.representation.ModelInstance

class ItemMiningTool(definition: ItemDefinition) : Item(definition), MiningTool {
	override val toolTypeName: String = definition.properties["toolType"].asString ?: "pickaxe"
	override val miningEfficiency: Float = definition.properties["miningEfficiency"].asFloat ?: 0.5f //java.lang.Float.parseFloat(type.resolveProperty(, "0.5"))

	val animationCycleDuration: Long = (definition.properties["animationCycleDuration"].asInt ?: 500).toLong()

	override fun buildRepresentation(worldPosition: Matrix4f, representationsGobbler: RepresentationsGobbler) {
		//TODO move that logic where it belongs: the player rendering class
		//val owner = pile.inventory.owner
		//val miningAction = (owner as? Entity)?.traits?.get(MinerTrait::class)?.progress

		val modelName = definition.properties["model"].asString
		if(modelName != null) {
			val model = definition.store.parent.models[modelName]

			val handTransformation = Matrix4f(worldPosition)

			/*if (miningAction != null) {
				handTransformation.rotate(Math.PI.toFloat(), 0f, 0f, 1f)
			}*/
			handTransformation.rotate(Math.PI.toFloat() * 1.5f, 0f, 1f, 0f)
			handTransformation.translate(0f, -0.2f, 0f)
			handTransformation.scale(0.5f)

			val position = ModelPosition(handTransformation)

			val modelInstance = ModelInstance(model, position)
			representationsGobbler.acceptRepresentation(modelInstance)

			return
		}

		super.buildRepresentation(worldPosition, representationsGobbler)
	}
}

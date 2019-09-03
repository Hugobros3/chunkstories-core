//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.pig

import org.joml.Matrix4f
import xyz.chunkstories.api.client.Client
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.ai.TraitAi
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.TraitRenderable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.graphics.MeshMaterial
import xyz.chunkstories.api.graphics.representation.ModelInstance
import xyz.chunkstories.api.graphics.representation.ModelPosition
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsGobbler
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.util.kotlin.toVec3f
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.EntityLiving
import xyz.chunkstories.core.entity.ai.GenericAI
import xyz.chunkstories.core.entity.traits.TraitBasicMovement
import xyz.chunkstories.core.entity.traits.TraitHumanoidStance

class EntityPig(definition: EntityDefinition, world: World) : EntityLiving(definition, world) {
	private val ai: PigAi = PigAi(this)
	internal val traitAi = TraitAi(this, ai)

	init {
		TraitBasicMovement(this)
		PigRenderer(this)
		object : TraitHealth(this) {
			override fun playDamageSound() {
				if (!isDead) {
					val i = 1 + Math.random().toInt() * 3
					entity.world.soundManager.playSoundEffect("sounds/entities/pig/hurt$i.ogg", SoundSource.Mode.NORMAL, entity.location, Math.random().toFloat() * 0.4f + 0.8f, 5.0f)
				}
			}
		}

		object : TraitCollidable(this@EntityPig) {
			override val collisionBoxes: Array<Box>
				get() {
					return arrayOf(Box.fromExtentsCenteredHorizontal(1.0, 0.8, 1.0))
				}

		}
	}

	override fun getBoundingBox(): Box {
		return Box.fromExtentsCentered(2.0, 2.0, 2.0)
	}
}

class PigRenderer(pig: EntityPig) : TraitRenderable<EntityPig>(pig) {
	override fun buildRepresentation(representationsGobbler: RepresentationsGobbler) {
		val model = entity.world.content.models["entities/pig/pig.dae"]

		val matrix = Matrix4f()
		matrix.translate(entity.location.toVec3f())
		matrix.translate(0f, 0.75f, 0f)
		matrix.scale(0.75f)
		val rotH = -90f + (entity.traits[TraitRotation::class]?.horizontalRotation ?: Math.random().toFloat())
		matrix.rotate(rotH / 180 * Math.PI.toFloat(), 0f, 1f, 0f)
		val position = ModelPosition(matrix)

		if(entity.traits[TraitHealth::class]?.isDead == true)
			matrix.scale(1f, -1f, 1f)

		val isPlayerEntity = (entity.world.gameContext as? Client)?.ingame?.player?.controlledEntity == this.entity

		var visibility = 0
		for (i in 0 until representationsGobbler.renderTaskInstances.size) {
			val isPassShadow = representationsGobbler.renderTaskInstances[i].name.startsWith("shadow")

			val ithBit = isPassShadow or !isPlayerEntity
			visibility = visibility or (ithBit.toInt() shl i)
		}

		val materials = model.meshes.mapIndexed { index, _ -> index }.associateWith { MeshMaterial("piggy", mapOf("albedoTexture" to "entities/pig/pig.png")) }


		//val animator = entity.traits[TraitAnimated::class]?.animatedSkeleton !!
		val modelInstance = ModelInstance(model, position, materials, visibility)

		representationsGobbler.acceptRepresentation(modelInstance, visibility)
	}
}

class PigAi(pig: EntityPig) : GenericAI<EntityPig>(pig) {
	override fun bark() {
		entity.world.soundManager.playSoundEffect("sounds/entities/pig/groink.ogg",
				SoundSource.Mode.NORMAL, entity.location, (0.9 + Math.random() * 0.2).toFloat(), 1.0f)
	}

}

private inline fun Boolean.toInt() = if (this) 1 else 0
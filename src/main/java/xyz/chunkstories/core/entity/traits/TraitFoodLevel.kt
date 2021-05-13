//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asFloat
import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.Subscriber
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.*
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster
import java.io.DataInputStream
import java.io.DataOutputStream

class TraitFoodLevel(entity: Entity, val defaultValue: Float) : Trait(entity), TraitSerializable, TraitNetworked<TraitFoodLevel.FoodLevelUpdate> {
	override val traitName = "food"

	var foodLevel: Float = defaultValue
		set(value) {
			field = value
			sendMessageController(FoodLevelUpdate(value))
		}

	data class FoodLevelUpdate(val value: Float) : TraitMessage() {
		override fun write(dos: DataOutputStream) {
			dos.writeFloat(value)
		}
	}

	override fun readMessage(dis: DataInputStream) = FoodLevelUpdate(dis.readFloat())

	override fun processMessage(message: FoodLevelUpdate, player: Player?) {
		if(entity.world is WorldMaster)
			return

		foodLevel = message.value
	}

	override fun whenSubscriberRegisters(subscriber: Subscriber) {
		if(subscriber == entity.controller)
			sendMessage(subscriber, FoodLevelUpdate(foodLevel))
	}

	override fun serialize() = Json.Value.Number(foodLevel.toDouble())

	override fun deserialize(json: Json) {
		foodLevel = json.asFloat ?: foodLevel
	}

	companion object {
		var HUNGER_DAMAGE_CAUSE: DamageCause = object : DamageCause {

			override val name: String
				get() = "Hunger"
		}
	}

	override fun tick() {
		val world = entity.world as? WorldMaster ?: return
		val traitHealth = entity.traits[TraitHealth::class] ?: throw Exception("TraitFoodLevel requires TraitHealth")
		val traitVelocity = entity.traits[TraitVelocity::class]?: throw Exception("TraitFoodLevel requires TraitVelocity")

		// Take damage when starving
		if (world.ticksElapsed % 100L == 0L) {
			if (foodLevel <= 0f)
				traitHealth.damage(HUNGER_DAMAGE_CAUSE, 1f)
			else {
				// 27 minutes to start starving at 0.1 starveFactor
				// Takes 100hp / ( 0.6rtps * 0.1 hp/hit )

				// Starve slowly if inactive
				var starve = 0.03f

				// Walking drains you
				if (traitVelocity.velocity.length() > 0.3) {
					starve = 0.06f
					// Running is even worse
					if (traitVelocity.velocity.length() > 0.7)
						starve = 0.15f
				}

				foodLevel -= starve
			}
		}

		// Having some food energy allows to restore HP, but also makes the entity go hungry
		if (foodLevel > 20 && !traitHealth.isDead) {
			if (traitHealth.health < traitHealth.maxHealth) {
				traitHealth.health += 0.01f
				foodLevel -= 0.01f
			}
		}
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.Subscriber
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitMessage
import xyz.chunkstories.api.entity.traits.serializable.TraitNetworked
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class TraitHumanoidStance(entity: Entity) : Trait(entity), TraitSerializable, TraitNetworked<TraitHumanoidStance.HumanoidStanceUpdate> {
	override val traitName = "stance"

	var stance = HumanoidStance.STANDING
		set(value) {
			field = value
			sendMessageAllSubscribersButController(HumanoidStanceUpdate(value))
		}

	enum class HumanoidStance private constructor(val eyeLevel: Double) {
		STANDING(1.65), CROUCHING(1.15)
	}

	data class HumanoidStanceUpdate(val stance: HumanoidStance) : TraitMessage() {
		override fun write(dos: DataOutputStream) {
			dos.write(stance.ordinal)
		}
	}

	override fun readMessage(dis: DataInputStream) = HumanoidStanceUpdate(HumanoidStance.values()[dis.read()])

	override fun processMessage(message: HumanoidStanceUpdate, from: Player?) {
		if (entity.world is WorldMaster && from != entity.controller)
			return

		stance = message.stance
	}

	override fun whenSubscriberRegisters(subscriber: Subscriber) {
		sendMessage(subscriber, HumanoidStanceUpdate(stance))
	}

	override fun serialize() = Json.Value.Text(stance.name.toLowerCase())

	override fun deserialize(json: Json) {
		json.asString?.toUpperCase()?.let {
			stance = HumanoidStance.valueOf(it)
		}
	}
}

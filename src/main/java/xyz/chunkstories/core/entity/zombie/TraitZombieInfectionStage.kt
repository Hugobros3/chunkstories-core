//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.zombie

import xyz.chunkstories.api.content.json.Json
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.Subscriber
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.TraitMessage
import xyz.chunkstories.api.entity.traits.serializable.TraitNetworked
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.player.Player
import xyz.chunkstories.api.world.WorldMaster

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

internal class TraitZombieInfectionStage(entity: Entity, initialStage: ZombieInfectionStage) : Trait(entity), TraitSerializable, TraitNetworked<TraitZombieInfectionStage.InfectionStageUpdate> {
	override val traitName = "stage"
	var stage: ZombieInfectionStage = initialStage
		set(value) {
			field = value
			sendMessageAllSubscribers(InfectionStageUpdate(value))
		}

	data class InfectionStageUpdate(val stage: ZombieInfectionStage) : TraitMessage() {
		override fun write(dos: DataOutputStream) {
			dos.write(stage.ordinal)
		}
	}

	override fun readMessage(dis: DataInputStream) = InfectionStageUpdate(ZombieInfectionStage.values()[dis.read()])

	override fun processMessage(message: InfectionStageUpdate, from: Player?) {
		if(entity.world is WorldMaster)
			return
		stage = message.stage
	}

	override fun whenSubscriberRegisters(subscriber: Subscriber) {
		sendMessage(subscriber, InfectionStageUpdate(stage))
	}

	override fun serialize() = Json.Value.Text(stage.name.toLowerCase())

	override fun deserialize(json: Json) {
		json.asString!!.toUpperCase().let {
			stage = ZombieInfectionStage.valueOf(it)
		}
	}
}

package xyz.chunkstories.core.entity.zombie

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.world.serialization.StreamSource
import xyz.chunkstories.api.world.serialization.StreamTarget

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

internal class TraitZombieInfectionStage(entity: Entity, initialStage: ZombieInfectionStage) : TraitSerializable(entity) {
    var stage: ZombieInfectionStage = initialStage
        set(value) {
            field = value
            pushComponentEveryone()
        }

    val serializedComponentName: String
        get() = "stage"

    @Throws(IOException::class)
    override fun push(destinator: StreamTarget, dos: DataOutputStream) {
        dos.writeByte(stage.ordinal)
    }

    @Throws(IOException::class)
    override fun pull(from: StreamSource, dis: DataInputStream) {
        val ok = dis.readByte()
        val i = ok.toInt()

        stage = ZombieInfectionStage.values()[i]
    }
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable
import xyz.chunkstories.api.world.serialization.StreamSource
import xyz.chunkstories.api.world.serialization.StreamTarget

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class TraitHumanoidStance(entity: Entity) : TraitSerializable(entity) {
    var stance = HumanoidStance.STANDING
        private set

    fun set(flying: HumanoidStance) {
        this.stance = flying
        this.pushComponentEveryone()
    }

    @Throws(IOException::class)
    override fun push(destinator: StreamTarget, dos: DataOutputStream) {
        dos.writeByte(this.stance.ordinal)
    }

    @Throws(IOException::class)
    override fun pull(from: StreamSource, dis: DataInputStream) {
        stance = HumanoidStance.values()[dis.readByte().toInt()]

        // the server accepts these from the player, and thus replicates them
        this.pushComponentEveryoneButController()
    }

    enum class HumanoidStance private constructor(val eyeLevel: Double) {
        STANDING(1.65), CROUCHING(1.15)
    }
}

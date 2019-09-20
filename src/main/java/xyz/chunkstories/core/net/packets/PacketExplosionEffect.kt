//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.net.packets

import xyz.chunkstories.api.client.net.ClientPacketsProcessor
import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.net.*
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.util.WorldEffects
import org.joml.Vector3d

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketExplosionEffect : PacketWorld {
    internal lateinit var center: Vector3d
    internal var radius: Double = 0.toDouble()
    internal var debrisSpeed: Double = 0.toDouble()
    internal var f: Float = 0.toFloat()

    constructor(world: World) : super(world)

    constructor(world: World, center: Vector3d, radius: Double, debrisSpeed: Double, f: Float) : super(world) {
        this.center = center
        this.radius = radius
        this.debrisSpeed = debrisSpeed
        this.f = f
    }

    @Throws(IOException::class)
    override fun send(destinator: PacketDestinator, out: DataOutputStream, ctx: PacketSendingContext) {
        out.writeDouble(center.x())
        out.writeDouble(center.y())
        out.writeDouble(center.z())

        out.writeDouble(radius)
        out.writeDouble(debrisSpeed)

        out.writeFloat(f)
    }

    @Throws(IOException::class, PacketProcessingException::class)
    override fun process(sender: PacketSender, dis: DataInputStream, processor: PacketReceptionContext) {
        center = Vector3d(dis.readDouble(), dis.readDouble(), dis.readDouble())
        radius = dis.readDouble()
        debrisSpeed = dis.readDouble()
        f = dis.readFloat()

        if (processor is ClientPacketsProcessor) {
            WorldEffects.createFireballFx(processor.world, center, radius, debrisSpeed, f)
        }
    }

}

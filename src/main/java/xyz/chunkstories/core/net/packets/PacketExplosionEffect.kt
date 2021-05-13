//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.net.packets

import xyz.chunkstories.api.exceptions.PacketProcessingException
import xyz.chunkstories.api.net.*
import xyz.chunkstories.api.world.World
import xyz.chunkstories.core.util.WorldEffects
import org.joml.Vector3d
import xyz.chunkstories.api.player.Player

import java.io.DataInputStream
import java.io.DataOutputStream

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

    override fun send(out: DataOutputStream) {
        out.writeDouble(center.x())
        out.writeDouble(center.y())
        out.writeDouble(center.z())

        out.writeDouble(radius)
        out.writeDouble(debrisSpeed)

        out.writeFloat(f)
    }

    override fun receive(dis: DataInputStream, player: Player?) {
        center = Vector3d(dis.readDouble(), dis.readDouble(), dis.readDouble())
        radius = dis.readDouble()
        debrisSpeed = dis.readDouble()
        f = dis.readFloat()

        WorldEffects.createFireballFx(world, center, radius, debrisSpeed, f)
    }

}

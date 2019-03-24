package xyz.chunkstories.core.particles

import org.joml.Vector3d
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.graphics.structs.IgnoreGLSL
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.world.World

class ParticleTypeSolid(definition: ParticleTypeDefinition) : ParticleType<ParticleTypeSolid.ParticleWithVelocity>(definition) {

    override fun new(location: Location): ParticleWithVelocity {
        val p = ParticleWithVelocity()
        p.position.set(location)
        return p
    }

    open class ParticleWithVelocity : Particle() {
        @IgnoreGLSL
        val velocity = Vector3d()
        @IgnoreGLSL
        var timer = 60 * 5
    }

    override val iterationLogic = solidParticleIterationLogic
}

val solidParticleIterationLogic: ParticleTypeSolid.ParticleWithVelocity.(World) -> ParticleTypeSolid.ParticleWithVelocity? = { world ->
    position.add(velocity)

    if(world.collisionsManager.isPointSolid(position))
        velocity.set(0.0)
    else if(velocity.y > - 10.0 * DELTA_60TPS) // if we're still not falling at 10m/s
        velocity.y += -9.81 * DELTA_60TPS // accelerate at 9.81m/s²

    // 60th square of 0.5

    velocity.mul(0.98581402)
    if (velocity.length() < 0.1 * DELTA_60TPS)
        velocity.set(0.0)

    if (timer-- <= 0)
        null
    else
        this
}

const val DELTA_60TPS = 1.0 / 60.0
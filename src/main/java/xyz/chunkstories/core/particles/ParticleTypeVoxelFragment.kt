package xyz.chunkstories.core.particles

import org.joml.Vector3f
import xyz.chunkstories.api.Location
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.api.voxel.VoxelSide
import xyz.chunkstories.api.world.World
import java.util.*

class ParticleTypeVoxelFragment(definition: ParticleTypeDefinition) : ParticleType<ParticleTypeVoxelFragment.ParticleVoxelFragment>(definition) {

    override fun new(location: Location): ParticleVoxelFragment = ParticleVoxelFragment().apply {
        position.set(location)

        val cell = location.world.peek(location)
        val voxel = cell.voxel ?: return@apply

        val voxelColor = voxel.getVoxelTexture(cell, VoxelSide.values()[Random().nextInt(6)]).color

        color.set(voxelColor.x(), voxelColor.y(), voxelColor.z())
    }

    class ParticleVoxelFragment : ParticleTypeSolid.ParticleWithVelocity() {
        val color = Vector3f(1f, 0f, 1f)
    }

    override val iterationLogic: ParticleVoxelFragment.(World) -> ParticleVoxelFragment? = { world ->

        position.add(velocity)

        if (world.collisionsManager.isPointSolid(position))
            velocity.set(0.0)
        else if (velocity.y > -10.0 * DELTA_60TPS) // if we're still not falling at 10m/s
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
}
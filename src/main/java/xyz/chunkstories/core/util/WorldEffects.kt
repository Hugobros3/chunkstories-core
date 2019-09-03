//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.util

import xyz.chunkstories.api.server.RemotePlayer
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.net.packets.PacketExplosionEffect
import org.joml.Vector3d

/** Creates an explosion with particles and sounds  */
object WorldEffects {
	fun createFireball(world: World, center: Vector3d, radius: Double, debrisSpeed: Double, f: Float) {
		// Play effect directly in SP
		if (world is WorldClient && world is WorldMaster)
			createFireballFx(world, center, radius, debrisSpeed, f)

		if (world is WorldMaster) {
			val packet = PacketExplosionEffect(world, center, radius, debrisSpeed, f)
			for (player in world.players) {
				val playerEntity = player.controlledEntity
				if (playerEntity != null && player is RemotePlayer) {
					val entityLocation = playerEntity.location
					if (entityLocation.distance(center) > 1024)
						continue
					player.pushPacket(packet)
				}
			}
		}

		// Play the sound more directly
		world.soundManager.playSoundEffect("./sounds/environment/kboom.ogg", Mode.NORMAL, center,
				(0.9f + Math.random() * 0.2f).toFloat(), (debrisSpeed * debrisSpeed * 10.0).toFloat(), 1f, 150f)
	}

	fun createFireballFx(world: World, center: Vector3d, radius: Double, debrisSpeed: Double, f: Float) {
		var z = 0
		while (z < 250 * f) {
			val lol = Vector3d(Math.random() * 2.0 - 1.0, Math.random() * 2.0 - 1.0,
					Math.random() * 2.0 - 1.0)
			lol.normalize()

			val spd = Vector3d(lol)
			spd.mul(debrisSpeed * (0.5 + Math.random()))

			lol.mul(radius)
			lol.add(center)

			//TODO
			//world.particlesManager.spawnParticleAtPositionWithVelocity("fire", lol, spd)
			z++
		}

		//TODO
		//world.particlesManager.spawnParticleAtPositionWithVelocity("fire_light", center, Vector3d(1.0, 0.0, 0.0).normalize().mul(debrisSpeed * 1.5f))
	}
}

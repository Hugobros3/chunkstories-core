//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitCollidable
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.sound.SoundSource.Mode
import xyz.chunkstories.api.voxel.Voxel
import xyz.chunkstories.api.voxel.materials.VoxelMaterial
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.core.voxel.VoxelLiquid
import org.joml.Vector3d

class TraitWalkingSounds(entity: Entity) : Trait(entity) {

    private var metersWalked = 0.0

    private var justJumped = false
    private var justLanded = false

    private var lastTickOnGround: Boolean = false

    fun handleWalkingEtcSounds() {
        // This is strictly a clientside thing
        if (entity.world !is WorldClient)
            return

        val collisions = entity.traits[TraitCollidable::class.java]
        val entityVelocity = entity.traits[TraitVelocity::class.java]
        val locomotion = entity.traits[TraitBasicMovement::class.java]

        if (collisions == null || entityVelocity == null || locomotion == null)
            return

        val playerEntity = (entity.world as WorldClient).client.player.controlledEntity

        // When the entities are too far from the player, don't play any sounds
        if (playerEntity != null)
            if (playerEntity.location.distance(entity.location) > 25f)
                return

        // Sound stuff
        if (collisions.isOnGround && !lastTickOnGround) {
            justLanded = true
            metersWalked = 0.0
        }

        // Used to trigger landing sound
        lastTickOnGround = collisions.isOnGround

        // Bobbing
        val horizontalSpeed = Vector3d(entityVelocity.velocity)
        horizontalSpeed.y = 0.0

        if (collisions.isOnGround)
            metersWalked += Math.abs(horizontalSpeed.length())

        val inWater = locomotion.isInWater

        var voxelStandingOn = entity.world.peekSafely(Vector3d(entity.location).add(0.0, -0.01, 0.0)).voxel

        if (voxelStandingOn == null || !voxelStandingOn.solid && voxelStandingOn !is VoxelLiquid)
            voxelStandingOn = entity.world.content.voxels().air()

        val material = voxelStandingOn.voxelMaterial

        if (justJumped && !inWater) {
            // TODO useless
            justJumped = false
            entity.world.soundManager.playSoundEffect(material.resolveProperty("jumpingSounds"), Mode.NORMAL, entity.location,
                    (0.9f + Math.sqrt(entityVelocity.velocity.x() * entityVelocity.velocity.x() + entityVelocity.velocity.z() * entityVelocity.velocity.z()) * 0.1f).toFloat(), 1f).attenuationEnd = 10f
        }
        if (justLanded) {
            justLanded = false
            entity.world.soundManager.playSoundEffect(material.resolveProperty("landingSounds"), Mode.NORMAL, entity.location,
                    (0.9f + Math.sqrt(entityVelocity.velocity.x() * entityVelocity.velocity.x() + entityVelocity.velocity.z() * entityVelocity.velocity.z()) * 0.1f).toFloat(), 1f).attenuationEnd = 10f
        }

        if (metersWalked > 0.2 * Math.PI * 2.0) {
            metersWalked %= 0.2 * Math.PI * 2.0
            if (horizontalSpeed.length() <= 0.06) {
                entity.world.soundManager.playSoundEffect(material.resolveProperty("walkingSounds"), Mode.NORMAL, entity.location,
                        (0.9f + Math.sqrt(entityVelocity.velocity.x() * entityVelocity.velocity.x() + entityVelocity.velocity.z() * entityVelocity.velocity.z()) * 0.1f).toFloat(), 1f).attenuationEnd = 10f
            } else {
                entity.world.soundManager.playSoundEffect(material.resolveProperty("runningSounds"), Mode.NORMAL, entity.location,
                        (0.9f + Math.sqrt(entityVelocity.velocity.x() * entityVelocity.velocity.x() + entityVelocity.velocity.z() * entityVelocity.velocity.z()) * 0.1f).toFloat(), 1f).attenuationEnd = 10f
            }
        }
    }
}

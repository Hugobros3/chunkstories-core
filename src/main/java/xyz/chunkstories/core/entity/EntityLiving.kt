//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.world.World
import xyz.chunkstories.api.world.WorldClient
import xyz.chunkstories.api.world.WorldMaster
import xyz.chunkstories.core.entity.traits.TraitBasicMovement
import xyz.chunkstories.core.entity.traits.TraitTakesFallDamage

abstract class EntityLiving(t: EntityDefinition, world: World) : Entity(t, world) {
    val traitRotation: TraitRotation
    val traitVelocity: TraitVelocity
    var traitHealth: TraitHealth

    init {

        traitVelocity = TraitVelocity(this)
        traitRotation = TraitRotation(this)
        traitHealth = TraitHealth(this)

        // Adds the trait that makes it so these entities take fall damage
        TraitTakesFallDamage(this)
    }

    override fun tick() {
        if (world is WorldMaster)
            this.traitHealth.removeCorpseAfterDelay()

        // Are we the global authority ?
        val has_global_authority = world is WorldMaster

        // Does this entity has a controller ?
        val controller = this.traits[TraitControllable::class]?.controller

        // Are we a client, that controller ?
        val owns_entity = world is WorldClient && controller === world.client.player

        // Servers get to tick all the entities that aren't controlled; the entities
        // that are are updated by their respective clients
        val should_tick_movement = owns_entity || has_global_authority && controller == null

        if (should_tick_movement)
            this.traits[TraitBasicMovement::class]?.tick()

        traits[TraitTakesFallDamage::class]?.tick()
    }
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDefinition
import xyz.chunkstories.api.entity.traits.TraitLoot
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
		TraitLoot(this)
	}
}

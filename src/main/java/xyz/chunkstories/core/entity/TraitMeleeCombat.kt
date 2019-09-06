//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.MeleeWeapon
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.TraitSight
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitSelectedItem
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity
import xyz.chunkstories.api.input.Input
import xyz.chunkstories.api.physics.EntityHitbox
import xyz.chunkstories.api.physics.RayResult
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.world.WorldMaster

open class TraitMeleeCombat<E>(entity: E, val naturalWeapon: MeleeWeapon?) : Trait(entity)
		where E : Entity {
	var ongoingAttack: Attack? = null

	override fun handleInput(input: Input): Boolean {
		if (input.name == "mouse.left") {
			val weapon = entity.traits[TraitSelectedItem::class]?.selectedItem?.item as? MeleeWeapon ?: naturalWeapon
			if (weapon != null && ongoingAttack == null) {
				val lookingAt = entity.traits[TraitSight::class]?.getLookingAt(5.0) ?: throw Exception("TraitMeleeCombat: Entity lacks a TraitSight !")
				//(entity.world.gameContext as? IngameClient)?.print("lookingAt: $lookingAt")
				when (lookingAt) {
					is RayResult.Hit.EntityHit -> {
						attack(weapon, lookingAt.entity, lookingAt.part)
						return true
					}
				}
			}
		}

		return false
	}

	protected open fun attack(weapon: MeleeWeapon, entity: Entity, part: EntityHitbox?) {
		//TODO replicate on server
		val attack = Attack(weapon, entity, part, weapon.damage, System.currentTimeMillis())
		val attackSound = attack.weapon.attackSound
		if(attackSound != null)
			entity.world.soundManager.playSoundEffect(attackSound, SoundSource.Mode.NORMAL, entity.location, 0.90f + Math.random().toFloat() * 0.20f, 1.0f)
		ongoingAttack = attack
	}

	private fun landAttack(ongoingAttack: Attack) {
		ongoingAttack.target.traits[TraitHealth::class]?.damage(DamageCause.Entity(entity, ongoingAttack.weapon), ongoingAttack.targetPart, ongoingAttack.damage)
		ongoingAttack.target.traits[TraitVelocity::class]?.let { ev ->
			val attacker = entity
			val attackKnockback = ongoingAttack.target.location.sub(attacker.location.add(0.0, 0.0, 0.0))
			attackKnockback.y = 0.0
			attackKnockback.normalize()

			val knockback = Math.max(1.0, Math.pow(ongoingAttack.damage.toDouble(), 0.5)).toFloat() * 0.1

			attackKnockback.mul(knockback * 0.2)
			attackKnockback.y = knockback * 0.2

			//println("attackKnockback: $attackKnockback")

			ev.addVelocity(attackKnockback)
		}
	}

	override fun tick() {
		ongoingAttack?.let {
			val now = System.currentTimeMillis()
			val delta = now - it.started

			if (it.stage == Attack.AttackStage.WARMUP && delta > it.weapon.warmupMillis) {
				if (entity.world is WorldMaster)
					landAttack(it)
				it.stage = Attack.AttackStage.COOLDOWN
			}

			if (it.stage == Attack.AttackStage.COOLDOWN && delta > it.weapon.warmupMillis + it.weapon.cooldownMillis) {
				ongoingAttack = null
			}
		}
	}

	class Attack(val weapon: MeleeWeapon, val target: Entity, val targetPart: EntityHitbox?, val damage: Float, val started: Long) {
		var stage: AttackStage = AttackStage.WARMUP

		enum class AttackStage {
			WARMUP,
			COOLDOWN
		}
	}
}
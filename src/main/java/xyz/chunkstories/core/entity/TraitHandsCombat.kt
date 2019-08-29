package xyz.chunkstories.core.entity

import xyz.chunkstories.api.entity.DamageCause
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.physics.EntityHitbox

class TraitHandsCombat<E>(entity: E, val punchDamage: Float, val punchCooldownMillis: Int) : Trait(entity)
where E: Entity, E: DamageCause{
    var lastHit = 0L

    fun tryAttack(target: Entity, hitBox: EntityHitbox?) {
        val now = System.currentTimeMillis()
        if(now - lastHit > punchCooldownMillis) {
            target.traits[TraitHealth::class]?.damage(entity as DamageCause, hitBox, punchDamage)
            lastHit = now
        }
    }
}
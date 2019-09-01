package xyz.chunkstories.core.entity.traits

import org.joml.Vector3d
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityGroundItem
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.world.WorldMaster
import java.lang.Exception

class TraitCanPickupItems(entity: Entity) : Trait(entity) {
    private val traitInventory: TraitInventory by lazy { entity.traits[TraitInventory::class] ?: throw Exception("TraitCanPickupItems requires TraitInventory") }

    override fun tick() {
        // Auto-pickups items on the ground
        if (entity.world is WorldMaster && entity.world.ticksElapsed % 60L == 0L) {

            for (e in  entity.world.getEntitiesInBox(Box.fromExtentsCentered(Vector3d(3.0)).translate(entity.location) )) {
                if (e is EntityGroundItem && e.location.distance(entity.location) < 3.0f) {
                    if (!e.canBePickedUpYet())
                        continue

                    entity.world.soundManager.playSoundEffect("sounds/item/pickup.ogg", SoundSource.Mode.NORMAL, entity.location, 1.0f, 1.0f)

                    val groundInventoy = e.traits[TraitInventory::class.java]!!.inventory

                    val pileToCollect = groundInventoy.getItemPileAt(0, 0)

                    val overflow = traitInventory.inventory.addItem(pileToCollect!!.item, pileToCollect.amount)
                    pileToCollect.amount = overflow

                    if (pileToCollect.amount <= 0)
                        entity.world.removeEntity(e)
                }
            }
        }
    }
}
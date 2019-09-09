//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.traits

import org.joml.Vector3d
import xyz.chunkstories.api.entity.Entity
import xyz.chunkstories.api.entity.EntityDroppedItem
import xyz.chunkstories.api.entity.TraitItemContainer
import xyz.chunkstories.api.entity.traits.Trait
import xyz.chunkstories.api.entity.traits.serializable.TraitHealth
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory
import xyz.chunkstories.api.physics.Box
import xyz.chunkstories.api.sound.SoundSource
import xyz.chunkstories.api.world.WorldMaster

class TraitCanPickupItems(entity: Entity) : Trait(entity) {
	private val traitInventory: TraitInventory by lazy {
		entity.traits[TraitInventory::class] ?: throw Exception("TraitCanPickupItems requires TraitInventory")
	}

	override fun tick() {
		// Auto-pickups items on the ground
		if (entity.world is WorldMaster && entity.world.ticksElapsed % 10L == 0L && entity.traits[TraitHealth::class]?.isDead != true) {

			for (e in entity.world.getEntitiesInBox(Box.fromExtentsCentered(Vector3d(3.0)).translate(entity.location))) {
				if (e is EntityDroppedItem && e.location.distance(entity.location) < 3.0f) {
					if (!e.canBePickedUpYet())
						continue

					entity.world.soundManager.playSoundEffect("sounds/item/pickup.ogg", SoundSource.Mode.NORMAL, entity.location, 1.0f, 1.0f)

					val itemContainer = e.traits[TraitItemContainer::class.java]!!

					//val pileToCollect = itemContainer.getItemPileAt(0, 0)

					//if(pileToCollect != null) {
					val overflow = traitInventory.inventory.addItem(itemContainer.item!!, itemContainer.amount)
					itemContainer.amount = overflow

					if (itemContainer.amount <= 0)
						entity.world.removeEntity(e)
					//} else {
					//	entity.world.removeEntity(e)
					//}
				}
			}
		}
	}
}
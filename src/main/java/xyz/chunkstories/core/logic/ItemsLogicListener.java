//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.logic;

import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitInventory;
import xyz.chunkstories.api.entity.traits.serializable.TraitRotation;
import xyz.chunkstories.api.entity.traits.serializable.TraitVelocity;
import xyz.chunkstories.api.events.EventHandler;
import xyz.chunkstories.api.events.Listener;
import xyz.chunkstories.api.events.item.EventItemDroppedToWorld;
import xyz.chunkstories.api.math.Math2;
import xyz.chunkstories.core.CoreContentPlugin;
import xyz.chunkstories.core.entity.EntityGroundItem;
import org.joml.Vector3d;

public class ItemsLogicListener implements Listener {
	private final CoreContentPlugin core;

	public ItemsLogicListener(CoreContentPlugin core) {
		this.core = core;
	}

	@EventHandler
	public void onDroppedItem(EventItemDroppedToWorld event) {
		// Create an EntityGroundItem and add it to the event
		Location throwLocation = event.getLocation();
		Vector3d throwForce = new Vector3d(0.0);

		// Throw it when dropping it from a player's inventory ?
		System.out.println(event.getInventoryFrom());
		if (event.getInventoryFrom() != null && event.getInventoryFrom() instanceof TraitInventory) {
			System.out.println("from som 1");
			TraitInventory TraitInventory = (TraitInventory) event.getInventoryFrom();
			Entity entity = TraitInventory.getEntity();

			entity.traits.with(TraitRotation.class, er -> {

				throwForce.set(new Vector3d(er.getDirectionLookingAt())
						.mul(0.15 - Math2.clampd(er.getVerticalRotation(), -45, 20) / 45f * 0.0f));

				if (entity.traits.has(TraitVelocity.class))
					throwForce.add(entity.traits.get(TraitVelocity.class).getVelocity());
			});

			/*
			 * TODO remake if(TraitInventory instanceof EntityLiving) { EntityLiving owner =
			 * (EntityLiving)TraitInventory; throwLocation = new Location(pos.getWorld(),
			 * pos.x(), pos.y() + ((EntityPlayer)owner).eyePosition, pos.z());
			 * 
			 * }
			 */
		}

		EntityGroundItem thrownItem = (EntityGroundItem) core.getPluginExecutionContext().getContent().entities()
				.getEntityDefinition("groundItem").newEntity(throwLocation.world);
		thrownItem.traitLocation.set(throwLocation);
		thrownItem.entityVelocity.setVelocity(throwForce);
		thrownItem.setItemPile(event.getItemPile());

		// EntityGroundItem entity = new
		// EntityGroundItem(core.getPluginExecutionContext().getContent().entities().getEntityDefinitionByName("groundItem"),
		// event.getLocation(), event.getItemPile());
		event.setItemEntity(thrownItem);
	}
}

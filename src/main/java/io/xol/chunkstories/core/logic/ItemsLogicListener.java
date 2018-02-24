//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.logic;

import org.joml.Vector3d;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.EntityLiving;
import io.xol.chunkstories.api.entity.interfaces.EntityWithInventory;
import io.xol.chunkstories.api.events.EventHandler;
import io.xol.chunkstories.api.events.Listener;
import io.xol.chunkstories.api.events.item.EventItemDroppedToWorld;
import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.core.CoreContentPlugin;
import io.xol.chunkstories.core.entity.EntityGroundItem;
import io.xol.chunkstories.core.entity.EntityPlayer;

public class ItemsLogicListener implements Listener {
	private final CoreContentPlugin core;
	
	public ItemsLogicListener(CoreContentPlugin core) {
		this.core = core;
	}

	@EventHandler
	public void onDroppedItem(EventItemDroppedToWorld event) {
		//Create an EntityGroundItem and add it to the event
		Location throwLocation = event.getLocation();
		Vector3d throwForce = new Vector3d(0.0);
		
		//Throw it when dropping it from a player's inventory ?
		System.out.println(event.getInventoryFrom());
		if(event.getInventoryFrom() != null && event.getInventoryFrom().getHolder() != null && event.getInventoryFrom().getHolder() instanceof Entity) {
			
			System.out.println("from som 1");
			EntityWithInventory entity = ((EntityWithInventory)event.getInventoryFrom().getHolder());
			Location pos = entity.getLocation();
			
			if(entity instanceof EntityLiving) {
				System.out.println("he l i v e s");
				EntityLiving owner = (EntityLiving)entity;
				throwLocation = new Location(pos.getWorld(), pos.x(), pos.y() + ((EntityPlayer)owner).eyePosition, pos.z());
				throwForce = new Vector3d(((EntityPlayer)owner).getDirectionLookingAt()).mul(0.15 - Math2.clampd(((EntityPlayer)owner).getEntityRotationComponent().getVerticalRotation(), -45, 20) / 45f * 0.0f);
				throwForce.add(((EntityPlayer)owner).getVelocityComponent().getVelocity());
			}
		}
		
		EntityGroundItem thrownItem = (EntityGroundItem) core.getPluginExecutionContext().getContent().entities().getEntityTypeByName("groundItem").create(throwLocation);
		thrownItem.positionComponent.setPosition(throwLocation);
		thrownItem.velocityComponent.setVelocity(throwForce);
		thrownItem.setItemPile(event.getItemPile());
		
		//EntityGroundItem entity = new EntityGroundItem(core.getPluginExecutionContext().getContent().entities().getEntityDefinitionByName("groundItem"), event.getLocation(), event.getItemPile());
		event.setItemEntity(thrownItem);
	}
}

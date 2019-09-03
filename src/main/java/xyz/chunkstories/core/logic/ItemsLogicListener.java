//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.logic;

import xyz.chunkstories.api.events.Listener;
import xyz.chunkstories.core.CoreContentPlugin;

public class ItemsLogicListener implements Listener {
	private final CoreContentPlugin core;

	public ItemsLogicListener(CoreContentPlugin core) {
		this.core = core;
	}

	/* @EventHandler public void onDroppedItem(EventItemDroppedToWorld event) { //
	 * Create an EntityGroundItem and add it to the event Location throwLocation =
	 * event.getLocation(); Vector3d throwForce = new Vector3d(0.0);
	 * 
	 * // Throw it when dropping it from a player's inventory ?
	 * System.out.println(event.getInventoryFrom()); if (event.getInventoryFrom() !=
	 * null && event.getInventoryFrom() instanceof TraitInventory) {
	 * System.out.println("from som 1"); TraitInventory TraitInventory =
	 * (TraitInventory) event.getInventoryFrom(); Entity entity =
	 * TraitInventory.getEntity();
	 * 
	 * entity.traits.with(TraitRotation.class, er -> {
	 * 
	 * throwForce.set(new Vector3d(er.getDirectionLookingAt()) .mul(0.15 -
	 * Math2.clampd(er.getVerticalRotation(), -45, 20) / 45f * 0.0f));
	 * 
	 * if (entity.traits.has(TraitVelocity.class))
	 * throwForce.add(entity.traits.get(TraitVelocity.class).getVelocity()); });
	 * 
	 * }
	 * 
	 * EntityGroundItem thrownItem = (EntityGroundItem)
	 * core.getGameContext().getContent().entities()
	 * .getEntityDefinition("groundItem").newEntity(throwLocation.getWorld());
	 * thrownItem.traitLocation.set(throwLocation);
	 * thrownItem.getEntityVelocity().setVelocity(throwForce);
	 * thrownItem.setItemPile(event.getItemPile());
	 * 
	 * // EntityGroundItem entity = new //
	 * EntityGroundItem(core.getPluginExecutionContext().getContent().entities().
	 * getEntityDefinitionByName("groundItem"), // event.getLocation(),
	 * event.getItemPile()); event.setItemEntity(thrownItem); } */
}

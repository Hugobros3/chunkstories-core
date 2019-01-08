//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.item;

import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.events.Event;
import xyz.chunkstories.api.events.EventListeners;

public class FirearmShotEvent extends Event {
	// Every event class has to have this
	private static EventListeners listeners = new EventListeners(FirearmShotEvent.class);

	@Override
	public EventListeners getListeners() {
		return listeners;
	}

	public static EventListeners getListenersStatic() {
		return listeners;
	}

	// Specific event code

	private final ItemFirearm itemFirearm;
	private final Entity shooter;
	private final Controller controller;

	FirearmShotEvent(ItemFirearm itemFirearm, Entity entity, Controller controller) {
		this.itemFirearm = itemFirearm;
		this.shooter = entity;
		this.controller = controller;
	}

	public ItemFirearm getItemFirearm() {
		return itemFirearm;
	}

	public Entity getShooter() {
		return shooter;
	}

	public Controller getController() {
		return controller;
	}
}

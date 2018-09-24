//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item;

import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.Event;
import io.xol.chunkstories.api.events.EventListeners;

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

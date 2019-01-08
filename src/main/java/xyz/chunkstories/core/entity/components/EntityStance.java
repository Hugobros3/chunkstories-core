//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.entity.components;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitSerializable;
import xyz.chunkstories.api.world.serialization.StreamSource;
import xyz.chunkstories.api.world.serialization.StreamTarget;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EntityStance extends TraitSerializable {
	private EntityHumanoidStance value = EntityHumanoidStance.STANDING;

	public EntityHumanoidStance get() {
		return value;
	}

	public void set(EntityHumanoidStance flying) {
		this.value = flying;
		this.pushComponentEveryone();
	}

	public EntityStance(Entity entity) {
		super(entity);
	}

	@Override
	protected void push(StreamTarget destinator, DataOutputStream dos) throws IOException {
		dos.writeByte(this.value.ordinal());
	}

	@Override
	protected void pull(StreamSource from, DataInputStream dis) throws IOException {
		value = EntityHumanoidStance.values()[dis.readByte()];
		this.pushComponentEveryoneButController();
	}

	public enum EntityHumanoidStance {
		STANDING(1.65), CROUCHING(1.15),;

		public final double eyeLevel;

		EntityHumanoidStance(double eyeLevel) {
			this.eyeLevel = eyeLevel;
		}
	}
}

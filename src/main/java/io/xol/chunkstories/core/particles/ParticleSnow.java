//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.world.World;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class ParticleSnow extends ParticleTypeHandler {
	public ParticleSnow(ParticleTypeDefinition type) {
		super(type);
	}

	public class SnowData extends ParticleData implements ParticleDataWithVelocity {

		int hp = 60 * 2; // 5s
		Vector3d vel = new Vector3d((Math.random() * 0.5 - 0.25) * 0.5, -Math.random() * 0.15 - 0.10,
				(Math.random() * 0.5 - 0.25) * 0.5);

		public SnowData(float x, float y, float z) {
			super(x, y, z);
		}

		public void setVelocity(Vector3dc vel) {
			this.vel.set(vel);
		}

		@Override
		public void setVelocity(Vector3fc vel) {
			this.vel.set(vel);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z) {
		return new SnowData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		SnowData b = (SnowData) data;

		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));

		if (b.isCollidingAgainst(world))
		{
			b.hp--;
			b.vel.set(0d, 0d, 0d);
		}

		if (b.hp < 0 || b.y() < 0)
			b.destroy();
	}
}

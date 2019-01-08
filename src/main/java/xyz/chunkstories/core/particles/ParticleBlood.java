//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.particles;

import xyz.chunkstories.api.particles.ParticleDataWithVelocity;
import xyz.chunkstories.api.particles.ParticleTypeDefinition;
import xyz.chunkstories.api.particles.ParticleTypeHandler;
import xyz.chunkstories.api.world.World;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class ParticleBlood extends ParticleTypeHandler {
	public ParticleBlood(ParticleTypeDefinition type) {
		super(type);
	}

	public class BloodData extends ParticleData implements ParticleDataWithVelocity {

		int timer = 60 * 5; // 5s
		Vector3d vel = new Vector3d();

		public BloodData(float x, float y, float z) {
			super(x, y, z);
		}

		@Override
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
		return new BloodData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		BloodData b = (BloodData) data;

		b.timer--;
		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));

		if (!b.isCollidingAgainst(world))
			// if (!((WorldImplementation) world).checkCollisionPoint(b.x(), b.y(), b.z()))
			b.vel.y = (b.vel.y() + -0.89 / 60.0);
		else
			b.vel.set(0d, 0d, 0d);

		// 60th square of 0.5
		b.vel.mul(0.98581402);
		if (b.vel.length() < 0.1 / 60.0)
			b.vel.set(0d, 0d, 0d);

		if (b.timer < 0)
			b.destroy();
	}
}

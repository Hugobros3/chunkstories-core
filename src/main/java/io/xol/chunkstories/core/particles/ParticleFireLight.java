//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.world.World;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class ParticleFireLight extends ParticleTypeHandler {
	public ParticleFireLight(ParticleTypeDefinition type) {
		super(type);
	}

	public class ParticleFireData extends ParticleData implements ParticleDataWithVelocity {

		public int timer = 60 * 60;
		public float temp = 7000;
		Vector3d vel = new Vector3d();
		int decay;

		public ParticleFireData(float x, float y, float z) {
			super(x, y, z);

			decay = 15 + 5;
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
		return new ParticleFireData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		ParticleFireData b = (ParticleFireData) data;

		b.timer--;

		b.vel.mul(0.93);

		if (b.vel.length() < 0.01 / 60.0)
			b.vel.set(0d, 0d, 0d);

		if (b.temp > 3000)
			b.temp -= 10 + b.decay;
		else if (b.temp > 0)
			b.temp -= b.decay;
		else if (b.temp <= 0) {
			b.destroy();
			b.temp = 1;
		}
	}
}

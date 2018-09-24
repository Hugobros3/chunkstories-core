//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.world.World;

public class ParticleMuzzleFlash extends ParticleTypeHandler {
	public ParticleMuzzleFlash(ParticleTypeDefinition type) {
		super(type);
	}

	public class MuzzleData extends ParticleData {

		public int timer = 2;

		public MuzzleData(float x, float y, float z) {
			super(x, y, z);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z) {
		return new MuzzleData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		((MuzzleData) data).timer--;
		if (((MuzzleData) data).timer < 0)
			data.destroy();
	}
}

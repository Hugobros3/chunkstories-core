//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.world.World;

public class ParticleLightningStrike extends ParticleTypeHandler {
	public ParticleLightningStrike(ParticleTypeDefinition type) {
		super(type);
	}

	public class MuzzleData extends ParticleData {

		public int timer = (int) (Math.random() * 10 + 5 + Math.random() * Math.random() * 150);

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

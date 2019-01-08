//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.particles;

import xyz.chunkstories.api.particles.ParticleTypeDefinition;
import xyz.chunkstories.api.particles.ParticleTypeHandler;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.core.particles.ParticleMuzzleFlash.MuzzleData;
import org.joml.Vector3f;

public class ParticleLight extends ParticleTypeHandler {
	public ParticleLight(ParticleTypeDefinition type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	public class ParticleLightData extends ParticleData {

		public int timer = 2;
		public Vector3f c;

		public ParticleLightData(float x, float y, float z) {
			super(x, y, z);
			c = new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z) {
		return new ParticleLightData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		((MuzzleData) data).timer--;
		if (((MuzzleData) data).timer < 0)
			data.destroy();
	}
}

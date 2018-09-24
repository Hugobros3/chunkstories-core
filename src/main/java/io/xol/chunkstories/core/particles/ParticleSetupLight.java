//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.world.World;
import org.joml.Vector3f;

public class ParticleSetupLight extends ParticleTypeHandler {
	public ParticleSetupLight(ParticleTypeDefinition type) {
		super(type);
		// TODO Auto-generated constructor stub
	}

	public class ParticleSetupLightData extends ParticleData {

		public int timer = 4800;
		public Vector3f c;

		public ParticleSetupLightData(float x, float y, float z) {
			super(x, y, z);
			c = new Vector3f((float) Math.random(), (float) Math.random(), (float) Math.random());
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z) {
		return new ParticleSetupLightData(x, y, z);
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		// TODO Auto-generated method stub

	}
}

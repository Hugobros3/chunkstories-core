//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.entity.Entity;
import org.joml.Vector3f;
import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.world.World;

public class ParticleLightningStrike extends ParticleTypeHandler
{
	public ParticleLightningStrike(ParticleTypeDefinition type) {
		super(type);
	}

	public class MuzzleData extends ParticleData {

		public int timer = (int) (Math.random() * 10 + 5 + Math.random() * Math.random() * 150);
		
		public MuzzleData(float x, float y, float z)
		{
			super(x, y, z);
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new MuzzleData(x, y, z);
	}

	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				Content content = ParticleLightningStrike.this.getType().store().parent();
				if(content instanceof ClientContent) {
					ClientContent clientContent = (ClientContent)content;
					LocalPlayer player = clientContent.getClient().getPlayer();
					
					Entity entity = player.getControlledEntity();
					if(entity != null) {
						Location loc = entity.getLocation();
						data.set((float)(double)data.x(), (float)(double)loc.y() + 1024, (float)(double)data.z());
					}
				}
				
				renderingContext.getLightsRenderer().queueLight(new Light(new Vector3f(226/255f, 255/255f, 226/255f).mul((float) (1f + Math.random())),
						new Vector3f((float) data.x(), (float) data.y(), (float) data.z()),
						102004f + (float) Math.random() * 5f));
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
	
	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		((MuzzleData)data).timer--;
		if(((MuzzleData)data).timer < 0)
			data.destroy();
	}
}

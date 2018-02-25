//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.particles;

import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.particles.ParticleDataWithTextureCoordinates;
import io.xol.chunkstories.api.particles.ParticleDataWithVelocity;
import io.xol.chunkstories.api.particles.ParticleTypeDefinition;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.api.particles.ParticlesRenderer;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.CellData;

public class ParticleVoxelFragment extends ParticleTypeHandler
{
	Voxels voxelStore;
	
	public ParticleVoxelFragment(ParticleTypeDefinition type) {
		super(type);
		
		voxelStore = type.store().parent().voxels();
	}

	public class FragmentData extends ParticleData implements ParticleDataWithTextureCoordinates, ParticleDataWithVelocity {
		
		VoxelTexture tex;
		
		int timer = 1450; // 30s
		Vector3d vel = new Vector3d();
		
		float rightX, topY, leftX, bottomY;
		
		public FragmentData(float x, float y, float z, CellData cell)
		{
			super(x, y, z);
			
			tex = cell.getTexture(VoxelSides.LEFT);
			
			int qx = (int) Math.floor(Math.random() * 4.0);
			int rx = qx + 1;
			int qy = (int) Math.floor(Math.random() * 4.0);
			int ry = qy + 1;
			
			leftX = (tex.getAtlasS()) / 32768f + tex.getAtlasOffset() / 32768f * (qx / 4.0f);
			rightX = (tex.getAtlasS()) / 32768f + tex.getAtlasOffset() / 32768f * (rx / 4.0f);
			
			topY = (tex.getAtlasT()) / 32768f + tex.getAtlasOffset() / 32768f * (qy / 4.0f);
			bottomY = (tex.getAtlasT()) / 32768f + tex.getAtlasOffset() / 32768f * (ry / 4.0f);
		}
		
		public void setVelocity(Vector3dc vel)
		{
			this.vel.set(vel);
			this.add((float)vel.x(), (float)vel.y(), (float)vel.z());
		}

		@Override
		public void setVelocity(Vector3fc vel) {
			this.vel.set(vel);
			this.add(vel);
		}

		@Override
		public float getTextureCoordinateXTopLeft()
		{
			return leftX;
			//return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXTopRight()
		{
			return rightX;
			//return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomLeft()
		{
			return leftX;
			//return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomRight()
		{
			return rightX;
			//return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYTopLeft()
		{
			return topY;
		}

		@Override
		public float getTextureCoordinateYTopRight()
		{
			return topY;
		}

		@Override
		public float getTextureCoordinateYBottomLeft()
		{
			return bottomY;
		}

		@Override
		public float getTextureCoordinateYBottomRight()
		{
			return bottomY;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z)
	{
		return new FragmentData(x, y, z, world.peekSafely((int)x, (int)y, (int)z));
	}

	@Override
	public void forEach_Physics(World world, ParticleData data)
	{
		FragmentData b = (FragmentData) data;
		
		b.timer--;
		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));
		
		if(!b.isCollidingAgainst(world, b.x, b.y - 0.1f, b.z))//if (!((WorldImplementation) world).checkCollisionPoint(b.x(), b.y() - 0.1, b.z()))
			b.vel.y = (b.vel.y() + -0.89/60.0);
		else
			b.vel.set(0d, 0d, 0d);
		
		// 60th square of 0.5
		b.vel.mul(0.98581402);
		if(b.vel.length() < 0.1/60.0)
			b.vel.set(0d, 0d, 0d);
		
		if(b.timer < 0)
			b.destroy();
	}
	
	@Override
	public ParticleTypeRenderer getRenderer(ParticlesRenderer particlesRenderer) {
		return new ParticleTypeRenderer(particlesRenderer) {
			
			@Override
			public Texture2D getAlbedoTexture()
			{
				return particlesRenderer.getContent().voxels().textures().getDiffuseAtlasTexture();
			}
			
			@Override
			public void forEach_Rendering(RenderingInterface renderingContext, ParticleData data)
			{
				// TODO Auto-generated method stub
				
			}

			@Override
			public void destroy() {
				
			}
			
		};
	}
}

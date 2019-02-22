//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.particles;

import xyz.chunkstories.api.content.Content.Voxels;
import xyz.chunkstories.api.particles.*;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.voxel.textures.VoxelTexture;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.cell.CellData;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

public class ParticleVoxelFragment extends ParticleTypeHandler {
	private Voxels voxelStore;

	public ParticleVoxelFragment(ParticleTypeDefinition type) {
		super(type);

		voxelStore = type.store().parent().voxels();
	}

	public class FragmentData extends ParticleData
			implements ParticleDataWithTextureCoordinates, ParticleDataWithVelocity {

		VoxelTexture tex;

		int timer = 1450; // 30s
		Vector3d vel = new Vector3d();

		float rightX, topY, leftX, bottomY;

		public FragmentData(float x, float y, float z, CellData cell) {
			super(x, y, z);

			tex = null;//cell.getTexture(VoxelSide.LEFT);

			int qx = (int) Math.floor(Math.random() * 4.0);
			int rx = qx + 1;
			int qy = (int) Math.floor(Math.random() * 4.0);
			int ry = qy + 1;

			/*leftX = (tex.getAtlasS()) / 32768f + tex.getAtlasOffset() / 32768f * (qx / 4.0f);
			rightX = (tex.getAtlasS()) / 32768f + tex.getAtlasOffset() / 32768f * (rx / 4.0f);

			topY = (tex.getAtlasT()) / 32768f + tex.getAtlasOffset() / 32768f * (qy / 4.0f);
			bottomY = (tex.getAtlasT()) / 32768f + tex.getAtlasOffset() / 32768f * (ry / 4.0f);*/
			throw new UnsupportedOperationException("TODO");
		}

		public void setVelocity(Vector3dc vel) {
			this.vel.set(vel);
			this.add((float) vel.x(), (float) vel.y(), (float) vel.z());
		}

		@Override
		public void setVelocity(Vector3fc vel) {
			this.vel.set(vel);
			this.add(vel);
		}

		@Override
		public float getTextureCoordinateXTopLeft() {
			return leftX;
			// return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXTopRight() {
			return rightX;
			// return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomLeft() {
			return leftX;
			// return tex.atlasS / 32768f;
		}

		@Override
		public float getTextureCoordinateXBottomRight() {
			return rightX;
			// return (tex.atlasS + tex.atlasOffset) / 32768f;
		}

		@Override
		public float getTextureCoordinateYTopLeft() {
			return topY;
		}

		@Override
		public float getTextureCoordinateYTopRight() {
			return topY;
		}

		@Override
		public float getTextureCoordinateYBottomLeft() {
			return bottomY;
		}

		@Override
		public float getTextureCoordinateYBottomRight() {
			return bottomY;
		}
	}

	@Override
	public ParticleData createNew(World world, float x, float y, float z) {
		return new FragmentData(x, y, z, world.peekSafely((int) x, (int) y, (int) z));
	}

	@Override
	public void forEach_Physics(World world, ParticleData data) {
		FragmentData b = (FragmentData) data;

		b.timer--;
		b.x = ((float) (b.x() + b.vel.x()));
		b.y = ((float) (b.y() + b.vel.y()));
		b.z = ((float) (b.z() + b.vel.z()));

		if (!b.isCollidingAgainst(world, b.x, b.y - 0.1f, b.z))// if (!((WorldImplementation)
																// world).checkCollisionPoint(b.x(), b.y() - 0.1,
																// b.z()))
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

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator;

import xyz.chunkstories.api.math.Math2;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.structures.Structure;
import xyz.chunkstories.api.world.chunk.Chunk;
import org.joml.Vector3i;

public class CaveBuilderStructure extends Structure {

	final Vector3i from;
	final Vector3i to;
	final double fromSize, toSize;

	public CaveBuilderStructure(Vector3i from, Vector3i to, double fromSize, double toSize, int width, int height, int length) {
		super(width, height, length);
		this.from = from;
		this.to = to;

		this.fromSize = fromSize;
		this.toSize = toSize;
	}

	public static CaveBuilderStructure caveSegment(Vector3i fromP, Vector3i toP, double fromS, double toS) {
		Vector3i from = new Vector3i(fromP);
		Vector3i to = new Vector3i(toP);

		Vector3i size = new Vector3i();
		size.x = 6 + Math.abs(from.x - to.x);
		size.y = 6 + Math.abs(from.y - to.y);
		size.z = 6 + Math.abs(from.z - to.z);

		return new CaveBuilderStructure(from, to, fromS, toS, size.x, size.y, size.z);
	}

	@Override
	public void paste(Chunk chunk, Vector3i position, int flags) {
		// System.out.println("pasting cave @"+from+" to "+to);
		Vector3i whereTho = new Vector3i();
		double d = from.distance(to);
		for (int i = 0; i <= d; i++) {
			whereTho.x = (int) Math2.mix(from.x, to.x, i / d);
			whereTho.y = (int) Math2.mix(from.y, to.y, i / d);
			whereTho.z = (int) Math2.mix(from.z, to.z, i / d);
			// System.out.println("pasting cave @"+from+" to "+to +"prog="+i / d);
			sphereOfDoom(chunk, whereTho, Math2.mix(fromSize, toSize, i / d));
		}
		sphereOfDoom(chunk, to, toSize);
		// cubeOfDoom(chunk, from, 3);
		// cubeOfDoom(chunk, to, 3);
	}

	private void sphereOfDoom(Chunk chunk, Vector3i position, double radius) {
		int size = (int) Math.ceil(radius);
		Voxel air = chunk.getWorld().getContent().getVoxels().getAir();
		for (int x = position.x - size; x <= position.x + size; x++)
			for (int y = position.y - size; y <= position.y + size; y++)
				for (int z = position.z - size; z <= position.z + size; z++) {
					if (inchunk(x, y, z, chunk)) {
						int rx = position.x - x;
						int ry = position.y - y;
						int rz = position.z - z;

						if (rx * rx + ry * ry + rz * rz <= size * size) {
							Voxel voxel = air;
							if (y == 0)
								voxel = chunk.getWorld().getContent().getVoxels().getVoxel("stone");
							else if (y < 10)
								voxel = chunk.getWorld().getContent().getVoxels().getVoxel("lava");

							chunk.pokeSimpleSilently(x, y, z, voxel, 0, 0, 0);
						}
					}
				}
	}

	private boolean inchunk(int x, int y, int z, Chunk chunk) {

		return x >= chunk.getChunkX() * 32 && x < chunk.getChunkX() * 32 + 32 && y >= chunk.getChunkY() * 32 && y < chunk.getChunkY() * 32 + 32
				&& z >= chunk.getChunkZ() * 32 && z < chunk.getChunkZ() * 32 + 32;
	}
}

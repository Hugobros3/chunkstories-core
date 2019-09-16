//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.generator;

import xyz.chunkstories.api.math.Math2;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.core.generator.HorizonGenerator.SliceData;
import xyz.chunkstories.core.generator.HorizonGenerator.StructureToPaste;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.Random;

public class CaveBuilder {
	private final World world;
	private final HorizonGenerator generator;

	public CaveBuilder(World world, HorizonGenerator generator) {
		this.world = world;
		this.generator = generator;
	}

	public void generateCaves(int cx, int cz, SliceData sliceData) {
		int rx = cx >> 3;
		int rz = cz >> 3;

		int sir = world.getWorldInfo().getSize().sizeInChunks >> 3;

		Random rnd = new Random();

		for (int irx = rx - 2; irx <= rx + 2; irx++)
			for (int irz = rz - 2; irz <= rz + 2; irz++) {
				int arx = irx % sir;
				int arz = irz % sir;

				int seed = (arx * sir + arz) % 44873 + arx * 1848 + arz * 874;
				rnd.setSeed(seed);

				// MAX 16 snakes per region
				int nsnakes = rnd.nextInt(8);
				// System.out.println(nsnakes+" snake in region (seed="+seed+") arx"+arx+"
				// arz"+arz);

				for (int i = 0; i < nsnakes; i++) {
					Vector3i pos = new Vector3i(arx * 256 + rnd.nextInt(256), 0, arz * 256 + rnd.nextInt(256));
					int groundHeight = generator.getHeightAtInternal(pos.x, pos.z);
					if (groundHeight <= 32)
						continue;

					pos.y = 32 + rnd.nextInt(groundHeight);

					int length = rnd.nextInt(64);
					propagateSnake(cx, cz, pos, length, 0, sliceData);
				}
			}
	}

	private void propagateSnake(int cx, int cz, Vector3i pos, int length, int forkdepth, SliceData sliceData) {
		// System.out.println("propagating snake");
		if (length <= 0)
			return;

		Random rnd = new Random();
		rnd.setSeed(pos.x * 1548 + pos.y * 487 + pos.z);

		Vector3d direction = new Vector3d(rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0);
		direction.normalize();

		int step = 8;

		int nextFork = 2 + rnd.nextInt(1 + (int) (length / 1.5));
		if (forkdepth > 3)
			nextFork = -1;

		// center of the focus chunk
		int ccx = cx * 32 + 16;
		int ccz = cz * 32 + 16;
		Vector2i chunkpos = new Vector2i(ccx, ccz);

		double sizemin = 1.2d;
		double sizemax = 5.5d;

		double size = rnd.nextDouble() * (sizemax - sizemin) + sizemin;

		for (int j = 0; j < length; j++) {
			Vector3i oldPosition = new Vector3i(pos);

			Vector3i delta = new Vector3i();
			delta.x = (int) (direction.x * step);
			delta.y = (int) (direction.y * step);
			delta.z = (int) (direction.z * step);
			pos.add(delta);

			double oldsize = size;
			size += rnd.nextDouble() * 0.25 - 0.5;
			size = Math2.clampd(size, sizemin, sizemax);

			Vector3d newdirection = new Vector3d(rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0, rnd.nextFloat() * 2.0 - 1.0);
			newdirection.normalize();

			double newdirectioninfluence = rnd.nextDouble();
			// newdirectioninfluence *= newdirectioninfluence;

			direction.x = Math2.mix(direction.x, direction.x + newdirection.x, newdirectioninfluence);
			direction.y = Math2.mix(direction.y, direction.y + newdirection.y, newdirectioninfluence);
			direction.z = Math2.mix(direction.z, direction.z + newdirection.z, newdirectioninfluence);
			direction.normalize();

			Vector2i posDelta = new Vector2i(pos.x, pos.z);
			posDelta.sub(chunkpos);

			// System.out.println(posDelta.x + ":"+posDelta.y);
			if (posDelta.length() < 64) {
				// System.out.println("touching terrain we want to gen!");
				sliceData.getStructures().add(new StructureToPaste(CaveBuilderStructure.caveSegment(pos, oldPosition, size, oldsize), pos, 0));
			}

			if (nextFork > 0)
				nextFork--;
			if (nextFork == 0) {
				// System.out.println("fork snake");
				propagateSnake(cx, cz, pos, length - rnd.nextInt(j), forkdepth + 1, sliceData);
				nextFork = 2 + rnd.nextInt(1 + (int) (length / 1.5));
			}
		}
	}
}

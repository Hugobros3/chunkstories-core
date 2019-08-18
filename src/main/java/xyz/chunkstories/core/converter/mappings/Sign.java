//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.converter.mappings;

import xyz.chunkstories.api.converter.mappings.NonTrivialMapper;
import xyz.chunkstories.api.exceptions.world.WorldException;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.components.VoxelComponent;
import xyz.chunkstories.api.world.World;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.Chunk;
import xyz.chunkstories.core.voxel.VoxelSign;
import xyz.chunkstories.core.voxel.components.VoxelComponentSignText;
import io.xol.enklume.MinecraftChunk;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.nbt.*;
import io.xol.enklume.util.SignParseUtil;

public class Sign extends NonTrivialMapper {

	public Sign(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion, int minecraftCuurrentChunkZinsideRegion,
			int x, int y, int z) {

		Chunk chunk = csWorld.getChunksManager().getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;

		if (voxel instanceof VoxelSign) {

			if (!voxel.getName().endsWith("_post")) {
				if (minecraftMetaData == 2)
					minecraftMetaData = 8;
				else if (minecraftMetaData == 3)
					minecraftMetaData = 0;
				else if (minecraftMetaData == 4)
					minecraftMetaData = 4;
				else if (minecraftMetaData == 5)
					minecraftMetaData = 12;
			}

			FutureCell future = new FutureCell(csWorld, csX, csY, csZ, voxel);
			future.setMetaData(minecraftMetaData);
			csWorld.pokeSimple(future);

			try {
				translateSignText(csWorld.tryPeek(csX, csY, csZ).getComponents().getVoxelComponent("signData"),
						region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion), x, y,
						z);
			} catch (WorldException e) {
				e.printStackTrace();
			}
			// TODO Move Sign text getting here ?

		} else
			System.out.println("fuck you 666");
	}

	private void translateSignText(VoxelComponent target, MinecraftChunk minecraftChunk, int x, int y, int z) {
		NBTCompound root = minecraftChunk.getRootTag();

		if (root == null)
			return;

		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");

		// Check it exists and is of the right kind
		if (target instanceof VoxelComponentSignText) {
			VoxelComponentSignText signTextComponent = (VoxelComponentSignText) target;
			signTextComponent.setSignText("<corresponding sign not found :(>");

			for (NBTag element : entitiesList.elements) {
				NBTCompound entity = (NBTCompound) element;
				NBTString entityId = (NBTString) entity.getTag("id");

				int tileX = ((NBTInt) entity.getTag("x")).data;
				int tileY = ((NBTInt) entity.getTag("y")).data;
				int tileZ = ((NBTInt) entity.getTag("z")).data;

				if (entityId.data.toLowerCase().equals("sign")
						|| entityId.data.toLowerCase().equals("minecraft:sign")) {
					if ((tileX & 0xF) != x || tileY != y || (tileZ & 0xF) != z) {
						continue;
					}

					String text1 = SignParseUtil.parseSignData(((NBTString) entity.getTag("Text1")).data);
					String text2 = SignParseUtil.parseSignData(((NBTString) entity.getTag("Text2")).data);
					String text3 = SignParseUtil.parseSignData(((NBTString) entity.getTag("Text3")).data);
					String text4 = SignParseUtil.parseSignData(((NBTString) entity.getTag("Text4")).data);

					String textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4;
					signTextComponent.setSignText(textComplete);
					break;

				}
			}
		}
	}
}
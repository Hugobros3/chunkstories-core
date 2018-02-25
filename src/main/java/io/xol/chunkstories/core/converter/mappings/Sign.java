//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.converter.mappings;

import io.xol.chunkstories.api.converter.mappings.NonTrivialMapper;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.VoxelSign;
import io.xol.chunkstories.core.voxel.components.VoxelComponentSignText;
import io.xol.enklume.MinecraftChunk;
import io.xol.enklume.MinecraftRegion;
import io.xol.enklume.nbt.NBTCompound;
import io.xol.enklume.nbt.NBTInt;
import io.xol.enklume.nbt.NBTList;
import io.xol.enklume.nbt.NBTString;
import io.xol.enklume.nbt.NBTag;
import io.xol.enklume.util.SignParseUtil;

public class Sign extends NonTrivialMapper {

	public Sign(Voxel voxel) {
		super(voxel);
	}

	@Override
	public void output(World csWorld, int csX, int csY, int csZ, int minecraftBlockId, int minecraftMetaData,
			MinecraftRegion region, int minecraftCuurrentChunkXinsideRegion,
			int minecraftCuurrentChunkZinsideRegion, int x, int y, int z) {

		Chunk chunk = csWorld.getChunkWorldCoordinates(csX, csY, csZ);
		assert chunk != null;
		
		if (voxel instanceof VoxelSign) {
			
			if(!voxel.getName().endsWith("_post")) {
				if (minecraftMetaData == 2)
					minecraftMetaData = 8;
				else if (minecraftMetaData == 3)
					minecraftMetaData = 0;
				else if (minecraftMetaData == 4)
					minecraftMetaData = 4;
				else if (minecraftMetaData == 5)
					minecraftMetaData = 12;
			}
			
			/*baked = VoxelFormat.changeMeta(baked, minecraftMetaData);
			
			try {
				baked = ((VoxelSign) voxel).onPlace(chunk.peek(csX, csY, csZ), baked, null);
			} catch (WorldException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			csWorld.pokeRawSilently(csX, csY, csZ, baked);*/
			csWorld.pokeSimple(new FutureCell(csWorld, csX, csY, csZ, voxel));
			
			try {
				translateSignText(csWorld.peek(csX, csY, csZ).components().get("signData"), region.getChunk(minecraftCuurrentChunkXinsideRegion, minecraftCuurrentChunkZinsideRegion), x, y, z);
			} catch (WorldException e) {
				e.printStackTrace();
			}
			//TODO Move Sign text getting here ?
			
		}
		else
			System.out.println("fuck you 666");
	}
	
	private void translateSignText(VoxelComponent target, MinecraftChunk minecraftChunk, int x, int y, int z) {
		NBTCompound root = minecraftChunk.getRootTag();
		
		if(root == null)
			return;
		
		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");
		
		for (NBTag element : entitiesList.elements)
		{
			NBTCompound entity = (NBTCompound) element;
			NBTString entityId = (NBTString) entity.getTag("id");
			
			int tileX = ((NBTInt)entity.getTag("x")).data;
			int tileY = ((NBTInt)entity.getTag("y")).data;
			int tileZ = ((NBTInt)entity.getTag("z")).data;

			//System.out.println("Looking up sign data for A:"+tileX+":"+tileY+":"+tileZ);
			//System.out.println("Looking up sign data for B:"+x+":"+y+":"+z);
			if(tileX % 16 != x || tileY != y || tileZ % 16 != z) {
				continue;
			}
			
			//System.out.println("Found sign data for "+x+":"+y+":"+z);
			
			if (entityId.data.toLowerCase().equals("chest"))
			{
				//Actually we don't bother converting the items
			}
			else if (entityId.data.toLowerCase().equals("sign") || entityId.data.toLowerCase().equals("minecraft:sign"))
			{
				String text1 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text1")).data);
				String text2 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text2")).data);
				String text3 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text3")).data);
				String text4 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text4")).data);
				
				//Check it exists and is of the right kind
				if(target != null && target instanceof VoxelComponentSignText) {
					VoxelComponentSignText signTextComponent = (VoxelComponentSignText)target;

					String textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4;
					signTextComponent.setSignText(textComplete);
					
					//System.out.println("OK: "+textComplete);
					break;
				}
				else
					assert false; // or die
				
				//((EntitySign) ((VoxelSign) voxel).getEntity(exported.peekSafely(csCoordinatesX, csCoordinatesY, csCoordinatesZ))).setText(textComplete);
				
			}
			//else
			//	System.out.println("Found "+entityId.data);
		}
	}
	
	/*public static void processAdditionalStuff(MinecraftChunk minecraftChunk) {
		
		NBTCompound root = minecraftChunk.getRootTag();
		
		if(root == null)
			return;
		NBTList entitiesList = (NBTList) root.getTag("Level.TileEntities");
		if (entitiesList != null)
		{
			for (NBTag element : entitiesList.elements)
			{
				NBTCompound entity = (NBTCompound) element;
				NBTString entityId = (NBTString) entity.getTag("id");
				
				int tileX = ((NBTInt)entity.getTag("x")).data;
				int tileY = ((NBTInt)entity.getTag("y")).data;
				int tileZ = ((NBTInt)entity.getTag("z")).data;
				
				int csCoordinatesX = (tileX % 16 + 16) % 16 + csBaseX;
				int csCoordinatesY = tileY;
				int csCoordinatesZ = (tileZ % 16 + 16) % 16 + csBaseZ;
				
				if (entityId.data.toLowerCase().equals("chest"))
				{
					//Actually we don't bother converting the items
				}
				else if (entityId.data.toLowerCase().equals("sign") || entityId.data.toLowerCase().equals("minecraft:sign"))
				{
					String text1 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text1")).data);
					String text2 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text2")).data);
					String text3 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text3")).data);
					String text4 = SignParseUtil.parseSignData(((NBTString)entity.getTag("Text4")).data);
					
					// Chunkstories use a single string instead of 4.
					String textComplete = text1 + "\n" + text2 + "\n" + text3 + "\n" + text4;
					
					Chunk chunk = exported.getChunkWorldCoordinates(csCoordinatesX, csCoordinatesY, csCoordinatesZ);
					
					assert chunk != null; // Chunk should really be loaded at this point !!!
					
					ChunkCell context = chunk.peek(csCoordinatesX, csCoordinatesY, csCoordinatesZ);
					
					assert context.getVoxel() instanceof VoxelSign; // We expect this too
					
					//We grab the component that should have been created while placing the block
					VoxelComponent component = context.components().get("signData");
					
					//Check it exists and is of the right kind
					if(component != null && component instanceof VoxelComponentSignText) {
						VoxelComponentSignText signTextComponent = (VoxelComponentSignText)component;
						signTextComponent.setSignText(textComplete);
					}
					else
						assert false; // or die
					
					//((EntitySign) ((VoxelSign) voxel).getEntity(exported.peekSafely(csCoordinatesX, csCoordinatesY, csCoordinatesZ))).setText(textComplete);
					
				}
				//else
				//	System.out.println("Found "+entityId.data);
			}
		}
	}*/
}
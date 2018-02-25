//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelMeta16 extends Voxel
{
	VoxelTexture colors[] = new VoxelTexture[16];

	public VoxelMeta16(VoxelDefinition type)
	{
		super(type);
		for (int i = 0; i < 16; i++)
			colors[i] = store.textures().getVoxelTextureByName(getName() + "." + i);
	}

	@Override
	public VoxelTexture getVoxelTexture(VoxelSides side, CellData info) // 0 for top, 1 bot,
	// 2,3,4,5
	// north/south/east/west
	{
		// System.out.println("swag");
		return colors[info.getMetaData()];
	}
	
	@Override
	public ItemPile[] getItems()
	{
		ItemPile[] items = new ItemPile[16];
		for(int i = 0; i < 16; i++)
		{
			ItemVoxel itemVoxel = (ItemVoxel)store.parent().items().getItemTypeByName("item_voxel").newItem();
			itemVoxel.voxel = this;
			itemVoxel.voxelMeta = i;			
			
			items[i] = new ItemPile(itemVoxel);
		};
		return items;
	}
}

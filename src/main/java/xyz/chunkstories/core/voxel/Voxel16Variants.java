//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.item.ItemVoxel;
import xyz.chunkstories.api.item.inventory.ItemPile;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.voxel.textures.VoxelTexture;
import xyz.chunkstories.api.world.cell.CellData;

import java.util.LinkedList;
import java.util.List;

public class Voxel16Variants extends Voxel {
	private final String variants[] = new String[16];
	private final VoxelTexture textures[] = new VoxelTexture[16];

	public Voxel16Variants(VoxelDefinition definition) {
		super(definition);

		String variantsString = definition.resolveProperty("variants", "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15");

		String split[] = variantsString.split(",");
		assert split.length == 16;

		for (int i = 0; i < 16; i++) {
			variants[i] = split[i].replaceAll(" ", "");
			textures[i] = store().textures().get(definition.resolveProperty("texture", definition.getName()) + "." + variants[i]);
		}
	}

	@Override
	public VoxelTexture getVoxelTexture(CellData info, VoxelSide side) {
		return textures[info.getMetaData()];
	}

	@Override
	public List<ItemPile> enumerateItemsForBuilding() {
		List<ItemPile> items = new LinkedList<>();
		for (int i = 0; i < 16; i++) {

			ItemVoxel itemVoxel = store().parent().items().getItemDefinition("item_voxel").newItem();
			itemVoxel.setVoxel(this);
			itemVoxel.setVoxelMeta(i);

			items.add(new ItemPile(itemVoxel));
		}
		
		return items;
	}
}

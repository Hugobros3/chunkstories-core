//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import org.jetbrains.annotations.NotNull;
import xyz.chunkstories.api.Location;
import xyz.chunkstories.api.entity.Controller;
import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.entity.traits.serializable.TraitControllable;
import xyz.chunkstories.api.events.voxel.WorldModificationCause;
import xyz.chunkstories.api.exceptions.world.WorldException;
import xyz.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.item.inventory.Inventory;
import xyz.chunkstories.api.player.Player;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.voxel.VoxelSide;
import xyz.chunkstories.api.voxel.components.VoxelComponent;
import xyz.chunkstories.api.voxel.components.VoxelInventoryComponent;
import xyz.chunkstories.api.voxel.textures.VoxelTexture;
import xyz.chunkstories.api.world.WorldMaster;
import xyz.chunkstories.api.world.cell.CellData;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.Chunk.ChunkCell;
import xyz.chunkstories.api.world.chunk.Chunk.FreshChunkCell;

import static xyz.chunkstories.api.util.compatibility.MinecraftSidesKt.getSideMcStairsChestFurnace;

public class VoxelChest extends Voxel {
	VoxelTexture frontTexture;
	VoxelTexture sideTexture;
	VoxelTexture topTexture;

	public VoxelChest(VoxelDefinition type) {
		super(type);

		frontTexture = store().textures().get(getName() + "_front");
		sideTexture = store().textures().get(getName() + "_side");
		topTexture = store().textures().get(getName() + "_top");
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input) {
		if (input.getName().equals("mouse.right") && voxelContext.getWorld() instanceof WorldMaster) {

			Controller controller = entity.traits.tryWith(TraitControllable.class, TraitControllable::getController);
			if (controller instanceof Player) {
				Player player = (Player) controller;
				Entity playerEntity = player.getControlledEntity();

				if(playerEntity != null) {
					if (playerEntity.getLocation().distance(voxelContext.getLocation()) <= 5) {
						player.openInventory(getInventory(voxelContext));
					}
				}
			}
		}
		return false;
	}

	private Inventory getInventory(ChunkCell context) {
		VoxelComponent comp = context.components().getVoxelComponent("chestInventory");
		VoxelInventoryComponent component = (VoxelInventoryComponent) comp;
		return component.getInventory();
	}

	@Override
	public void whenPlaced(FreshChunkCell cell) {
		// Create a new component and insert it into the chunk
		VoxelInventoryComponent component = new VoxelInventoryComponent(cell.components(), 10, 6);
		cell.registerComponent("chestInventory", component);
	}

	@Override
	public VoxelTexture getVoxelTexture(@NotNull CellData info, @NotNull VoxelSide side) {
		VoxelSide actualSide = getSideMcStairsChestFurnace(info.getMetaData());

		if (side.equals(VoxelSide.TOP))
			return topTexture;

		if (side.equals(actualSide))
			return frontTexture;

		return sideTexture;
	}

	@Override
	// Chunk stories chests use Minecraft format to ease porting of maps
	public void onPlace(@NotNull FutureCell cell, WorldModificationCause cause) throws IllegalBlockModificationException {
		// Can't access the components of a non-yet placed FutureCell
		// getInventory(context);

		int stairsSide = 0;
		// See:
		// http://minecraft.gamepedia.com/Data_values#Ladders.2C_Furnaces.2C_Chests.2C_Trapped_Chests
		if (cause instanceof Entity) {
			Location loc = ((Entity) cause).getLocation();
			double dx = loc.x() - (cell.getX() + 0.5);
			double dz = loc.z() - (cell.getZ() + 0.5);
			if (Math.abs(dx) > Math.abs(dz)) {
				if (dx > 0)
					stairsSide = 4;
				else
					stairsSide = 5;
			} else {
				if (dz > 0)
					stairsSide = 2;
				else
					stairsSide = 3;
			}
			cell.setMetaData(stairsSide);
		}
	}

	@Override
	public void onRemove(ChunkCell context, WorldModificationCause cause) throws WorldException {

		// Delete the components as to not pollute the chunk's components space
		// context.components().erase();
	}
}

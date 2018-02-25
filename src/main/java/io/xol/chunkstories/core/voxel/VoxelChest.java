//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.voxel.components.VoxelInventoryComponent;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;
import io.xol.chunkstories.api.world.chunk.Chunk.FreshChunkCell;

public class VoxelChest extends Voxel
{
	VoxelTexture frontTexture;
	VoxelTexture sideTexture;
	VoxelTexture topTexture;
	
	public VoxelChest(VoxelDefinition type)
	{
		super(type);
		
		frontTexture = store.textures().getVoxelTextureByName(getName() + "_front");
		sideTexture = store.textures().getVoxelTextureByName(getName() + "_side");
		topTexture = store.textures().getVoxelTextureByName(getName() + "_top");
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input)
	{
		if(input.getName().equals("mouse.right") && voxelContext.getWorld() instanceof WorldMaster) {
			//Only actual players can open that kind of stuff
			if(entity instanceof EntityControllable) {
				EntityControllable e = (EntityControllable)entity;
				Controller c = e.getController();
				
				if(c instanceof Player && ((Player) c).getLocation().distance(voxelContext.getLocation()) <= 5) {
					Player p = (Player)c;
					
					p.openInventory(getInventory(voxelContext));
				}
				
			}
		}
		return false;
	}
	
	private Inventory getInventory(ChunkCell context) {
		VoxelComponent comp = context.components().get("chestInventory");
		VoxelInventoryComponent component = (VoxelInventoryComponent)comp;
		return component.getInventory();
	}
	
	@Override
	public void whenPlaced(FreshChunkCell cell) {
		// Create a new component and insert it into the chunk
		VoxelInventoryComponent component = new VoxelInventoryComponent(cell.components(), 10, 6);
		cell.registerComponent("chestInventory", component);
	}

	@Override
	public VoxelTexture getVoxelTexture(VoxelSides side, CellData info)
	{
		VoxelSides actualSide = VoxelSides.getSideMcStairsChestFurnace(info.getMetaData());
		
		if(side.equals(VoxelSides.TOP))
			return topTexture;
		
		if(side.equals(actualSide))
			return frontTexture;
		
		return sideTexture;
	}
	
	@Override
	//Chunk stories chests use Minecraft format to ease porting of maps
	public void onPlace(FutureCell cell, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		//Can't access the components of a non-yet placed FutureCell
		//getInventory(context);
		
		int stairsSide = 0;
		//See: 
		//http://minecraft.gamepedia.com/Data_values#Ladders.2C_Furnaces.2C_Chests.2C_Trapped_Chests
		if (cause != null && cause instanceof Entity)
		{
			Location loc = ((Entity) cause).getLocation();
			double dx = loc.x() - (cell.getX() + 0.5);
			double dz = loc.z() - (cell.getZ() + 0.5);
			if (Math.abs(dx) > Math.abs(dz))
			{
				if(dx > 0)
					stairsSide = 4;
				else
					stairsSide = 5;
			}
			else
			{
				if(dz > 0)
					stairsSide = 2;
				else
					stairsSide = 3;
			}
			cell.setMetaData(stairsSide);
		}
	}

	@Override
	public void onRemove(ChunkCell context, WorldModificationCause cause) throws WorldException {
		
		//Delete the components as to not pollute the chunk's components space
		//context.components().erase();
	}
}

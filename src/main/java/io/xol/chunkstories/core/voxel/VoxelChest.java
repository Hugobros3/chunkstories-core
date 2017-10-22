package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Controller;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityControllable;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.inventory.BasicInventory;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;
import io.xol.chunkstories.core.voxel.components.VoxelInventoryComponent;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelChest extends Voxel implements VoxelInteractive, VoxelLogic
{
	VoxelTexture frontTexture;
	VoxelTexture sideTexture;
	VoxelTexture topTexture;
	
	public VoxelChest(VoxelType type)
	{
		super(type);
		
		frontTexture = store.textures().getVoxelTextureByName(getName() + "_front");
		sideTexture = store.textures().getVoxelTextureByName(getName() + "_side");
		topTexture = store.textures().getVoxelTextureByName(getName() + "_top");
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkVoxelContext voxelContext, Input input)
	{
		//Open GUI
		if(input.getName().equals("mouse.right") && voxelContext.getWorld() instanceof WorldMaster) {
			//Only actual players can open that kind of stuff
			if(entity instanceof EntityControllable) {
				EntityControllable e = (EntityControllable)entity;
				Controller c = e.getController();
				
				if(c instanceof Player) {
					Player p = (Player)c;
					
					//System.out.println(getInventory(voxelContext).getWidth() + " : " + getInventory(voxelContext).getHeight());
					p.openInventory(getInventory(voxelContext));
					//p.openInventory(((EntityChest)this.getEntity(voxelContext)).getInventory());
				}
				
			}
		}
		return false;
	}
	
	private Inventory getInventory(ChunkVoxelContext context) {
		
		// Try to grab the existing chest inventory
		VoxelComponent comp = context.components().get("chestInventory");
		if(comp != null) {
			VoxelInventoryComponent component = (VoxelInventoryComponent)comp;
			return component.getInventory();
		}

		// Create a new component and insert it into the chunk
		VoxelInventoryComponent component = new VoxelInventoryComponent(context.components(), new BasicInventory(10, 6));
		context.components().put("chestInventory", component);
		return component.getInventory();
	}

	@Override
	public VoxelTexture getVoxelTexture(int data, VoxelSides side, VoxelContext info)
	{
		VoxelSides actualSide = VoxelSides.getSideMcStairsChestFurnace(VoxelFormat.meta(data));
		
		if(side.equals(VoxelSides.TOP))
			return topTexture;
		
		if(side.equals(actualSide))
			return frontTexture;
		
		return sideTexture;
	}
	
	@Override
	//Chunk stories chests use Minecraft format to ease porting of maps
	public int onPlace(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		getInventory(context);
		//super.onPlace(context, voxelData, cause);
		
		int stairsSide = 0;
		//See: 
		//http://minecraft.gamepedia.com/Data_values#Ladders.2C_Furnaces.2C_Chests.2C_Trapped_Chests
		if (cause != null && cause instanceof Entity)
		{
			Location loc = ((Entity) cause).getLocation();
			double dx = loc.x() - (context.getX() + 0.5);
			double dz = loc.z() - (context.getZ() + 0.5);
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
			voxelData = VoxelFormat.changeMeta(voxelData, stairsSide);
		}
		return voxelData;
	}

	@Override
	public void onRemove(ChunkVoxelContext context, WorldModificationCause cause) throws WorldException {
		
		//Delete the components as to not pollute the chunk's components space
		context.components().erase();
	}

	@Override
	public int onModification(ChunkVoxelContext context, int voxelData, WorldModificationCause cause)
			throws WorldException {
		return voxelData;
	}
	
	
}

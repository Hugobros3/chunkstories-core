//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.item.ItemVoxel;
import io.xol.chunkstories.api.item.inventory.ItemPile;
import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.sound.SoundSource.Mode;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.EditableCell;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;

/**
 * 2-blocks tall door Requires two consecutive voxel ids, x being lower, x+1 top, the top part should be suffixed of _top
 */
public class VoxelDoor extends Voxel implements VoxelCustomIcon
{
	VoxelTexture doorTexture;

	VoxelModel[] models = new VoxelModel[8];

	boolean top;

	public VoxelDoor(VoxelDefinition type)
	{
		super(type);

		top = getName().endsWith("_top");

		if (top)
			doorTexture = store.textures().getVoxelTextureByName(getName().replace("_top", "") + "_upper");
		else
			doorTexture = store.textures().getVoxelTextureByName(getName() + "_lower");

		for (int i = 0; i < 8; i++)
			models[i] = store.models().getVoxelModelByName("door.m" + i);
	}

	public Voxel getUpperPart() {
		if(top)
			return this;
		else
			return store().getVoxelByName(getName()+"_top");
	}
	
	public Voxel getLowerPart() {
		if(top) 
			return store.getVoxelByName(getName().substring(0, getName().length() - 4));
		else
			return this;
	}
	
	@Override
	public VoxelTexture getVoxelTexture(VoxelSides side, CellData info)
	{
		return doorTexture;
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		int facingPassed = (info.getMetaData() >> 2) & 0x3;
		boolean isOpen = ((info.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((info.getMetaData() >> 1) & 0x1) == 1;

		int i = 0;

		if (hingeSide)
			facingPassed += 4;

		switch (facingPassed)
		{
		case 0:
			i = isOpen ? 3 : 0;
			break;
		case 1:
			i = isOpen ? 4 : 1;
			break;
		case 2:
			i = isOpen ? 5 : 6;
			break;
		case 3:
			i = isOpen ? 2 : 7;
			break;

		case 4:
			i = isOpen ? 1 : 4;
			break;
		case 5:
			i = isOpen ? 6 : 5;
			break;
		case 6:
			i = isOpen ? 7 : 2;
			break;
		case 7:
			i = isOpen ? 0 : 3;
			break;
		}

		return models[i];
	}

	//Meta
	//0x0 -> open/close
	//0x1 -> left/right hinge || left = 0 right = 1 (left is default)
	//0x2-0x4 -> side ( VoxelSide << 2 )

	@Override
	public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input)
	{
		if (!input.getName().equals("mouse.right"))
			return false;
		if (!(entity.getWorld() instanceof WorldMaster))
			return true;
		
		boolean isOpen = ((voxelContext.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((voxelContext.getMetaData() >> 1) & 0x1) == 1;
		int facingPassed = (voxelContext.getMetaData() >> 2) & 0x3;

		boolean newState = !isOpen;

		int newData = computeMeta(newState, hingeSide, facingPassed);

		Location otherPartLocation = voxelContext.getLocation();
		if (top)
			otherPartLocation.add(0.0, -1.0, 0.0);
		else
			otherPartLocation.add(0.0, 1.0, 0.0);

		EditableCell otherLocationPeek = voxelContext.getWorld().peekSafely(otherPartLocation);
		if (otherLocationPeek.getVoxel() instanceof VoxelDoor)
		{
			System.out.println("new door status : " + newState);
			voxelContext.getWorld().getSoundManager().playSoundEffect("sounds/voxels/door.ogg", Mode.NORMAL, voxelContext.getLocation(), 1.0f, 1.0f);

			voxelContext.setMetaData(newData);
			otherLocationPeek.setMetaData(newData);
			
			//otherPartLocation.setVoxelDataAtLocation(VoxelFormat.changeMeta(otherPartLocation.getVoxelDataAtLocation(), newData));
		}
		else
		{
			store.parent().logger().error("Incomplete door @ " + otherPartLocation);
		}

		return true;
	}

	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		CollisionBox[] boxes = new CollisionBox[1];

		int facingPassed = (info.getMetaData() >> 2) & 0x3;
		boolean isOpen = ((info.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((info.getMetaData() >> 1) & 0x1) == 1;

		boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);

		if (isOpen)
		{
			switch (facingPassed + (hingeSide ? 4 : 0))
			{
			case 0:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			case 1:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			case 2:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 3:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 4:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 5:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 6:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			case 7:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			}
		}
		else
		{
			switch (facingPassed)
			{
			case 0:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(0.125 / 2, 0, 0.5);
				break;
			case 1:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 1.0 - 0.125 / 2);
				break;
			case 2:
				boxes[0] = new CollisionBox(0.125, 1.0, 1.0).translate(1.0 - 0.125 / 2, 0, 0.5);
				break;
			case 3:
				boxes[0] = new CollisionBox(1.0, 1.0, 0.125).translate(0.5, 0, 0.125 / 2);
				break;
			}
		}
		
		boxes[0].translate(-boxes[0].xw / 2, 0, -boxes[0].zw / 2);

		return boxes;
	}

	@Override
	public void onPlace(FutureCell cell, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		//Ignore all that crap on a slave world
		if (!(cell.getWorld() instanceof WorldMaster))
			return;

		//We should only place the lower part, prevent entities from doing so !
		if (top && cause != null && cause instanceof Entity)
			throw new IllegalBlockModificationException(cell, "Entities can't place upper doors parts");

		//If the system adds the upper part, no modifications to be done on it
		if (top)
			return;
		
		World world = cell.getWorld();
		int x = cell.getX();
		int y = cell.getY();
		int z = cell.getZ();

		//Check top is free
		int topData = world.peekRaw(x, y + 1, z);
		if (VoxelFormat.id(topData) != 0)
			throw new IllegalBlockModificationException(cell, "Top part isn't free");

		//grab our attributes
		boolean isOpen = ((cell.getMetaData() >> 0) & 0x1) == 1;
		boolean hingeSide = ((cell.getMetaData() >> 1) & 0x1) == 1;
		int facingPassed = (cell.getMetaData() >> 2) & 0x3;

		//Default face is given by passed metadata
		VoxelSides doorSideFacing = VoxelSides.values()[facingPassed];

		//Determine side if placed by an entity and not internal code
		if (cause != null && cause instanceof Entity)
		{
			Location loc = ((Entity) cause).getLocation();
			double dx = loc.x() - (x + 0.5);
			double dz = loc.z() - (z + 0.5);
			if (Math.abs(dx) > Math.abs(dz))
			{
				if (dx > 0)
					doorSideFacing = VoxelSides.RIGHT;
				else
					doorSideFacing = VoxelSides.LEFT;
			}
			else
			{
				if (dz > 0)
					doorSideFacing = VoxelSides.FRONT;
				else
					doorSideFacing = VoxelSides.BACK;
			}

			//If there is an adjacent one, set the hinge to right
			Voxel adjacent = null;
			switch (doorSideFacing)
			{
			case LEFT:
				adjacent = world.peekSimple(x, y, z - 1);
				break;
			case RIGHT:
				adjacent = world.peekSimple(x, y, z + 1);
				break;
			case FRONT:
				adjacent = world.peekSimple(x - 1, y, z);
				break;
			case BACK:
				adjacent = world.peekSimple(x + 1, y, z);
				break;
			default:
				break;
			}
			if (adjacent instanceof VoxelDoor)
			{
				hingeSide = true;
			}

			cell.setMetaData(computeMeta(isOpen, hingeSide, doorSideFacing));
		}

		//Place the upper part and we're good to go
		world.pokeSimple(x, y + 1, z, this.getUpperPart(), -1, -1, cell.getMetaData());
	}

	public static int computeMeta(boolean isOpen, boolean hingeSide, VoxelSides doorFacingSide)
	{
		return computeMeta(isOpen, hingeSide, doorFacingSide.ordinal());
	}

	public static int computeMeta(boolean isOpen, boolean hingeSide, int doorFacingsSide)
	{
		//System.out.println(doorFacingsSide + " open: " + isOpen + " hinge:" + hingeSide);
		return (doorFacingsSide << 2) | (((hingeSide ? 1 : 0) & 0x1) << 1) | (isOpen ? 1 : 0) & 0x1;
	}

	@Override
	public void onRemove(ChunkCell context, WorldModificationCause cause)
	{
		//Don't interfere with system pokes, else we get stuck in a loop
		if(cause == null || !(cause instanceof Entity))
			return;
			
		World world = context.getWorld();
		int x = context.getX();
		int y = context.getY();
		int z = context.getZ();
		
		//Ignore all that crap on a slave world
		if (!(world instanceof WorldMaster))
			return;

		int otherPartOfTheDoorY = y;
		
		if (top)
			otherPartOfTheDoorY--;
		else
			otherPartOfTheDoorY++;

		Voxel restOfTheDoorVoxel = world.peekSimple(x, otherPartOfTheDoorY, z);
		//Remove the other part as well, if it still exists
		if (restOfTheDoorVoxel instanceof VoxelDoor)
			world.pokeSimple(x, otherPartOfTheDoorY, z, store().air(), -1, -1, 0);
		
	}

	@Override
	public ItemPile[] getItems()
	{
		//Top part shouldn't be placed
		if (top)
			return new ItemPile[] {};


		ItemVoxel itemVoxel = (ItemVoxel)store.parent().items().getItemTypeByName("item_voxel_1x2").newItem();
		itemVoxel.voxel = this;		
		
		return new ItemPile[] { new ItemPile(itemVoxel) };
	}
}

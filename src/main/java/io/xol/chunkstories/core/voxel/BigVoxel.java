package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;

public class BigVoxel extends Voxel implements VoxelLogic {

	final int xWidth, yWidth, zWidth;
	
	final int xBits, yBits, zBits;
	final int xMask, yMask, zMask;
	final int xShift, yShift, zShift;
	
	public BigVoxel(VoxelType type) {
		super(type);
		
		this.xWidth = Integer.parseInt(type.resolveProperty("xWidth", "1"));
		this.yWidth = Integer.parseInt(type.resolveProperty("yWidth", "1"));
		this.zWidth = Integer.parseInt(type.resolveProperty("zWidth", "1"));
		
		xBits = (int)Math.ceil(Math.log(xWidth) / Math.log(2.0));
		yBits = (int)Math.ceil(Math.log(yWidth) / Math.log(2.0));
		zBits = (int)Math.ceil(Math.log(zWidth) / Math.log(2.0));
		
		xMask = (int) Math.pow(2, xBits) - 1;
		yMask = (int) Math.pow(2, yBits) - 1;
		zMask = (int) Math.pow(2, zBits) - 1;
		
		xShift = 0;
		yShift = xBits;
		zShift = yShift + yBits;
		
		if(xBits + yBits + zBits > 8) {
			throw new RuntimeException("Metadata requirements can't allow you to have more than a total of 8 bits to describe the length of those");
		}
	}

	@Override
	public int onPlace(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws IllegalBlockModificationException {
		//Be cool with the system doing it's thing
		if(cause == null)
			return voxelData;
		
		int x = context.getX();
		int y = context.getY();
		int z = context.getZ();
		
		//Check if there is space for it ...
		for(int a = x; a < x + xWidth; a++) {
			for(int b = y; b < y + yWidth; b++) {
				for(int c = z; c < z + zWidth; c++) {
					Chunk chunk = context.getWorld().getChunkWorldCoordinates(a, b, c);
					
					if(chunk == null)
						throw new IllegalBlockModificationException(context, "All chunks upon wich this block places itself must be fully loaded !");
					
					VoxelContext stuff = context.getWorld().peekSafely(a, b, c);
					if(stuff.getVoxel() == null || stuff.getVoxel().getId() == 0 || !stuff.getVoxel().getType().isSolid())
					{
						//These blocks are replaceable
						continue;
					}
					else throw new IllegalBlockModificationException(context, "Can't overwrite block at "+a+": "+b+": "+c);
				}
			}
		}
		
		//Actually build the thing then
		for(int a = 0; a < 0 + xWidth; a++) {
			for(int b = 0; b < 0 + yWidth; b++) {
				for(int c = 0; c < 0 + zWidth; c++) {
					int metadata = (byte) (((a & xMask ) << xShift) | ((b & yMask) << yShift) | ((c & zMask) << zShift));
					
					context.getWorld().pokeSimple(a + x, b + y, c + z, VoxelFormat.changeMeta(this.getId(), metadata));
					//world.setVoxelData(a + x, b + y, c + z, VoxelFormat.changeMeta(this.getId(), metadata));
				}
			}
		}
		
		//Return okay for the user
		return voxelData;
	}

	@Override
	public void onRemove(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws IllegalBlockModificationException {
		//Don't mess with machine removal
		if(cause == null)
			return;

		int x = context.getX();
		int y = context.getY();
		int z = context.getZ();
		
		//Backpedal to find the root block
		int meta = VoxelFormat.meta(voxelData);
		
		int ap = (meta >> xShift) & xMask;
		int bp = (meta >> yShift) & yMask;
		int cp = (meta >> zShift) & zMask;
		
		System.out.println("Removing "+ap+": "+bp+": "+cp);
		
		int startX = x - ap;
		int startY = y - bp;
		int startZ = z - cp;
		
		for(int a = startX; a < startX + xWidth; a++) {
			for(int b = startY; b < startY + yWidth; b++) {
				for(int c = startZ; c < startZ + zWidth; c++) {
					
					//Safely poke zero into the relevant locations
					context.getWorld().pokeSimple(a, b, c, 0);
					//world.setVoxelData(a, b, c, 0);
				}
			}
		}
	}

	@Override
	/** Big voxels manage themselves using their 8 bits of metadata. They don't let themselves being touched ! */
	public int onModification(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws IllegalBlockModificationException {
		if(cause != null && cause instanceof Entity)
			throw new IllegalBlockModificationException(context, "Big Voxels aren't modifiable by anyone !");
		return voxelData;
	}

	/** Test out the auto-partitionning logic for the 8 bits of metadata */
	public static void main(String args[]) {
		
		int xWidth = 16;
		int height = 4;
		int zWidth = 4;
		
		int xBits = (int)Math.ceil(Math.log(xWidth) / Math.log(2.0));
		int yBits = (int)Math.ceil(Math.log(height) / Math.log(2.0));
		int zBits = (int)Math.ceil(Math.log(zWidth) / Math.log(2.0));
		
		System.out.println(xBits + " : " + yBits + "  " + zBits);
		
		int xMask = (int) Math.pow(2, xBits) - 1;
		int yMask = (int) Math.pow(2, yBits) - 1;
		int zMask = (int) Math.pow(2, zBits) - 1;

		System.out.println(xMask + " : " + yMask + "  " + zMask);
		
		int xShift = 0;
		int yShift = xBits;
		int zShift = yShift + yBits;
		
		int xMask_shifted = xMask << xShift;
		int yMask_shifted = yMask << yShift;
		int zMask_shifted = zMask << zShift;
		
		System.out.println(xMask_shifted + " : " + yMask_shifted + "  " + zMask_shifted);
		
		for(int a = 0; a < xWidth; a++) {
			for(int b = 0; b < height; b++) {
				for(int c = 0; c < zWidth; c++) {
					byte test = (byte) (((a & xMask ) << xShift) | ((b & yMask) << yShift) | ((c & zMask) << zShift));
					
					int ap = (test >> xShift) & xMask;
					int bp = (test >> yShift) & yMask;
					int cp = (test >> zShift) & zMask;
					
					if(a == ap && b == bp && c == cp) {
						//System.out.println("All is good with the world rn");
					} else {
						System.out.println("test: "+test);
						System.out.println("a: "+ a + " ap: " + ap);
						System.out.println("b: "+ b + " bp: " + bp);
						System.out.println("c: "+ c + " cp: " + cp);
					}
				}
			}
		}
	}
}

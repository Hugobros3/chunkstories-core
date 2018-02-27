//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.voxel.VoxelDynamicRenderer;

import org.joml.Vector2f;
import org.joml.Vector3d;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelDefinition;

import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;
import io.xol.chunkstories.api.world.chunk.Chunk.FreshChunkCell;
import io.xol.chunkstories.core.voxel.components.VoxelComponentSignText;
import io.xol.chunkstories.core.voxel.renderers.SignRenderer;

/** Signs are voxels you can write stuff on */
//TODO implement a gui when placing a sign to actually set the text
//currently only the map converter can make signs have non-default text
//TODO expose the gui to the api to enable this
public class VoxelSign extends Voxel implements VoxelCustomIcon
{
	final SignRenderer signRenderer;
	
	public VoxelSign(VoxelDefinition type)
	{
		super(type);
		
		signRenderer = new SignRenderer(voxelRenderer);
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input)
	{
		return false;
	}
	
	@Override
	public VoxelDynamicRenderer getVoxelRenderer(CellData info)
	{
		return signRenderer;
	}
		
	@Override
	public void onPlace(FutureCell cell, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		//We don't create the components here, as the cell isn't actually changed yet!
		int x = cell.getX();
		int y = cell.getY();
		int z = cell.getZ();
		
		if(cause != null && cause instanceof Entity)
		{
			Vector3d blockLocation = new Vector3d(x + 0.5, y, z + 0.5);
			blockLocation.sub(((Entity) cause).getLocation());
			blockLocation.negate();
			
			Vector2f direction = new Vector2f((float)(double)blockLocation.x(), (float)(double)blockLocation.z());
			direction.normalize();
			//System.out.println("x:"+direction.x+"y:"+direction.y);
			
			double asAngle = Math.acos(direction.y()) / Math.PI * 180;
			asAngle *= -1;
			if(direction.x() < 0)
				asAngle *= -1;
			
			//asAngle += 180.0;
			
			asAngle %= 360.0;
			asAngle += 360.0;
			asAngle %= 360.0;
			
			//System.out.println(asAngle);
			
			int meta = (int)(16 * asAngle / 360);
			cell.setMetaData(meta);
		}
	}

	@Override
	public void whenPlaced(FreshChunkCell cell) {
		VoxelComponentSignText signTextComponent = new VoxelComponentSignText(cell.components());
		cell.registerComponent("signData", signTextComponent);
	}

	/** Gets the sign component from a chunkcell, assuming it is indeed a sign cell */
	public VoxelComponentSignText getSignData(ChunkCell context) {
		VoxelComponentSignText signTextComponent = (VoxelComponentSignText) context.components().get("signData");
		return signTextComponent;
	}
	
}
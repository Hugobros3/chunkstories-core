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
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;

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

public class VoxelSign extends Voxel implements VoxelCustomIcon
{
	public VoxelSign(VoxelDefinition type)
	{
		super(type);
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input)
	{
		return false;
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(CellData info)
	{
		return super.getVoxelRenderer(info);
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
	
	/*@Override
	public VoxelComponentDynamicRenderer getDynamicRendererComponent(ChunkCell context) {
		return getSignData(context);
	}*/

	public VoxelComponentSignText getSignData(ChunkCell context) {
		VoxelComponentSignText signTextComponent = (VoxelComponentSignText) context.components().get("signData");
		return signTextComponent;
	}
	
}
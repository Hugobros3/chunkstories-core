package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.events.voxel.WorldModificationCause;
import io.xol.chunkstories.api.exceptions.world.WorldException;
import io.xol.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import io.xol.chunkstories.api.input.Input;

import org.joml.Vector2f;
import org.joml.Vector3d;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelCustomIcon;
import io.xol.chunkstories.api.voxel.VoxelDynamicallyRendered;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.components.VoxelComponentDynamicRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.cell.FutureCell;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkCell;
import io.xol.chunkstories.core.voxel.components.VoxelComponentSignText;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelSign extends Voxel implements VoxelInteractive, VoxelLogic, VoxelCustomIcon, VoxelDynamicallyRendered
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
	public void onRemove(ChunkCell context, WorldModificationCause cause) throws WorldException {
		context.components().erase();
	}

	@Override
	public void onModification(ChunkCell context, FutureCell voxelData, WorldModificationCause cause) throws WorldException {
		
	}

	@Override
	public VoxelComponentDynamicRenderer getDynamicRendererComponent(ChunkCell context) {
		return getSignData(context);
	}

	private VoxelComponentSignText getSignData(ChunkCell context) {
		VoxelComponentSignText signTextComponent = (VoxelComponentSignText) context.components().get("signData");
		
		if(signTextComponent == null) {
			signTextComponent = new VoxelComponentSignText(context.components());
			context.components().put("signData", signTextComponent);
		}
		
		return signTextComponent;
	}
	
}
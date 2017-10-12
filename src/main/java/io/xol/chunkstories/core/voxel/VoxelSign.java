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
import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.voxel.VoxelInteractive;
import io.xol.chunkstories.api.voxel.VoxelLogic;
import io.xol.chunkstories.api.voxel.VoxelType;
import io.xol.chunkstories.api.voxel.components.VoxelComponentDynamicRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.world.VoxelContext;
import io.xol.chunkstories.api.world.chunk.Chunk.ChunkVoxelContext;
import io.xol.chunkstories.core.voxel.components.VoxelComponentSignText;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelSign extends Voxel implements VoxelInteractive, VoxelLogic, VoxelCustomIcon, VoxelDynamicallyRendered
{
	public VoxelSign(VoxelType type)
	{
		super(type);
	}

	@Override
	public boolean handleInteraction(Entity entity, ChunkVoxelContext voxelContext, Input input)
	{
		return false;
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(VoxelContext info)
	{
		return super.getVoxelRenderer(info);
	}
		
	@Override
	public int onPlace(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws IllegalBlockModificationException
	{
		context.components().put("signData", new VoxelComponentSignText(context.components()));
		
		//super.onPlace(context, voxelData, cause);
		//World world = context.getWorld();
		int x = context.getX();
		int y = context.getY();
		int z = context.getZ();
		
		//super.onPlace(world, x, y, z, voxelData, entity);
		
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
			voxelData = VoxelFormat.changeMeta(voxelData, meta);
		}
		
		return voxelData;
	}

	@Override
	public void onRemove(ChunkVoxelContext context, int voxelData, WorldModificationCause cause) throws WorldException {
		context.components().erase();
	}

	@Override
	public int onModification(ChunkVoxelContext context, int voxelData, WorldModificationCause cause)
			throws WorldException {
		return voxelData;
	}

	@Override
	public VoxelComponentDynamicRenderer getDynamicRendererComponent(ChunkVoxelContext context) {
		return getSignData(context);
	}

	private VoxelComponentSignText getSignData(ChunkVoxelContext context) {
		return (VoxelComponentSignText) context.components().get("signData");
	}
	
}
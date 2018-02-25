//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.client.net.ClientPacketsProcessor;
import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import org.joml.Vector3d;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketSendingContext;
import io.xol.chunkstories.api.net.PacketWorld;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.util.WorldEffects;

public class PacketExplosionEffect extends PacketWorld
{
	Vector3d center;
	double radius;
	double debrisSpeed;
	float f;

	public PacketExplosionEffect(World world)
	{
		super(world);
	}
	
	public PacketExplosionEffect(World world, Vector3d center, double radius, double debrisSpeed, float f)
	{
		super(world);
		this.center = center;
		this.radius = radius;
		this.debrisSpeed = debrisSpeed;
		this.f = f;
	}

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out, PacketSendingContext ctx) throws IOException
	{
		out.writeDouble(center.x());
		out.writeDouble(center.y());
		out.writeDouble(center.z());

		out.writeDouble(radius);
		out.writeDouble(debrisSpeed);
		
		out.writeFloat(f);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException, PacketProcessingException
	{
		center = new Vector3d(in.readDouble(), in.readDouble(), in.readDouble());
		radius = in.readDouble();
		debrisSpeed = in.readDouble();
		f = in.readFloat();
		
		if(processor instanceof ClientPacketsProcessor)
		{
			ClientPacketsProcessor cpp = (ClientPacketsProcessor)processor;
			WorldEffects.createFireballFx(cpp.getWorld(), center, radius, debrisSpeed, f);
		}
	}
	
}

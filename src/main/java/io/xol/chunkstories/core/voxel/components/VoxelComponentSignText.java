//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel.components;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.rendering.text.TextMesh;
import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.cell.CellComponents;
import io.xol.chunkstories.api.world.serialization.StreamSource;
import io.xol.chunkstories.api.world.serialization.StreamTarget;

public class VoxelComponentSignText extends VoxelComponent
{
	public VoxelComponentSignText(CellComponents holder) {
		super(holder);
	}
	
	public String cachedText = null; //set to whatever renderData represents
	public TextMesh renderData = null; //contains a mesh representing the text written on the sign

	String signText =  "In soviet belgium\n"
			+ "#FFFF00Waffles are yellow\n"
			+ "#FFFF00Fries are yellow\n"
			+ "Ketchup is #FF0000red";
	
	public String getSignText()
	{
		return signText;
	}

	public void setSignText(String name)
	{
		this.signText = name;
	}

	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException
	{
		dos.writeUTF(signText);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException
	{
		signText = dis.readUTF();
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel.components;

import io.xol.chunkstories.api.voxel.components.VoxelComponent;
import io.xol.chunkstories.api.world.cell.CellComponents;
import io.xol.chunkstories.api.world.serialization.StreamSource;
import io.xol.chunkstories.api.world.serialization.StreamTarget;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class VoxelComponentSignText extends VoxelComponent {
	public VoxelComponentSignText(CellComponents holder) {
		super(holder);
	}

	private String signText = "";
	public String getSignText() {
		return signText;
	}

	public void setSignText(String signText) {
		this.signText = signText;
		getHolder().getCell().refreshRepresentation();
	}

	@Override
	public void push(StreamTarget destinator, DataOutputStream dos) throws IOException {
		dos.writeUTF(signText);
	}

	@Override
	public void pull(StreamSource from, DataInputStream dis) throws IOException {
		signText = dis.readUTF();
	}
}

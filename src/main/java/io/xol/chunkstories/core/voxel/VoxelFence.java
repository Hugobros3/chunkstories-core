//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.physics.CollisionBox;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;

public class VoxelFence extends Voxel
{
	public VoxelFence(VoxelDefinition type)
	{
		super(type);
	}

	@Override
	public VoxelModel getVoxelRenderer(CellData info)
	{
		Voxel vox;
		vox = info.getNeightborVoxel(0);
		boolean connectLeft = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(1);
		boolean connectFront = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(2);
		boolean connectRight = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(3);
		boolean connectBack = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		
		String type = "default";
		if(connectLeft && connectFront && connectRight && connectBack)
			type = "allDir";
		else if(connectLeft && connectFront && connectRight)
			type = "allButBack";
		else if(connectLeft && connectFront && connectBack)
			type = "allButRight";
		else if(connectLeft && connectBack && connectRight)
			type = "allButFront";
		else if(connectBack && connectFront && connectRight)
			type = "allButLeft";
		else if(connectLeft && connectRight)
			type = "allX";
		else if(connectFront && connectBack)
			type = "allZ";
		else if(connectLeft && connectBack)
			type = "leftBack";
		else if(connectRight && connectBack)
			type = "rightBack";
		else if(connectLeft && connectFront)
			type = "leftFront";
		else if(connectRight && connectFront)
			type = "rightFront";
		else if(connectLeft)
			type = "left";
		else if(connectRight)
			type = "right";
		else if(connectFront)
			type = "front";
		else if(connectBack)
			type = "back";
		
		return store.models().getVoxelModel("wood_fence"+"."+type);
	}
	
	@Override
	public CollisionBox[] getCollisionBoxes(CellData info)
	{
		// System.out.println("kek");
		CollisionBox[] boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.3, 0.4, 1.0, 0.4) };

		Voxel vox;
		vox = info.getNeightborVoxel(0);
		boolean connectLeft = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(1);
		boolean connectFront = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(2);
		boolean connectRight = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);
		vox = info.getNeightborVoxel(3);
		boolean connectBack = (vox.getDefinition().isSolid() && vox.getDefinition().isOpaque()) || vox.equals(this);

		if (connectLeft && connectFront && connectRight && connectBack)
		{
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.0, 0.4, 1, 1.0), new CollisionBox(0.0, 0.0, 0.3, 1.0, 1, 0.4) };
		}
		
		else if (connectLeft && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.3, 1.0, 1, 0.4), new CollisionBox(0.3, 0.0, 0.25, 0.4, 1, 0.5).translate(0, 0, 0.25) };
		else if (connectLeft && connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.0, 0.4, 1, 1.0), new CollisionBox(0.25, 0.0, 0.3, 0.5, 1, 0.4).translate(-0.25, 0, 0) };
		else if (connectLeft && connectBack && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.3, 1.0, 1, 0.4), new CollisionBox(0.3, 0.0, 0.25, 0.4, 1, 0.5).translate(0, 0, -0.25) };
		else if (connectBack && connectFront && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.0, 0.4, 1, 1.0), new CollisionBox(0.25, 0.0, 0.3, 0.5, 1, 0.4).translate(0.25, 0, 0) };
		else if (connectLeft && connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.0, 0.0, 0.3, 1.0, 1, 0.4) };
		else if (connectFront && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.0, 0.4, 1, 1.0) };
		else if (connectLeft && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(-0.15, 0, 0), new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.7).translate(0, 0, -0.15) };
		else if (connectRight && connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(0.15, 0, 0), new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.7).translate(0, 0, -0.15) };
		else if (connectLeft && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(-0.15, 0, 0), new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.7).translate(0, 0, 0.15) };
		else if (connectRight && connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(0.15, 0, 0), new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.70).translate(0, 0, 0.15) };
		else if (connectLeft)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(-0.15, 0, 0) };
		else if (connectRight)
			boxes = new CollisionBox[] { new CollisionBox(0.15, 0.0, 0.3, 0.7, 1, 0.4).translate(0.15, 0, 0) };
		else if (connectFront)
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.7).translate(0, 0, 0.15) };
		else if (connectBack)
			boxes = new CollisionBox[] { new CollisionBox(0.3, 0.0, 0.15, 0.4, 1, 0.7).translate(0.0, 0.0, -0.15) };

		//for (CollisionBox box : boxes)
		//	box.translate(+0.25, -0, +0.25);

		return boxes;
	}
}

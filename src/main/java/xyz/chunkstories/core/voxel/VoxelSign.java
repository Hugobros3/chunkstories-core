//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.entity.Entity;
import xyz.chunkstories.api.events.voxel.WorldModificationCause;
import xyz.chunkstories.api.exceptions.world.voxel.IllegalBlockModificationException;
import xyz.chunkstories.api.input.Input;
import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.world.cell.FutureCell;
import xyz.chunkstories.api.world.chunk.ChunkCell;
import xyz.chunkstories.api.world.chunk.FreshChunkCell;
import xyz.chunkstories.core.voxel.components.VoxelComponentSignText;
import org.joml.Vector2f;
import org.joml.Vector3d;

/**
 * Signs are voxels you can write stuff on
 */
//TODO implement a gui when placing a sign to actually set the text
//currently only the map converter can make signs have non-default text
//TODO expose the gui to the api to enable this
public class VoxelSign extends Voxel// implements VoxelCustomIcon
{
    //final SignRenderer signRenderer;

    public VoxelSign(VoxelDefinition type) {
        super(type);

        //signRenderer = new SignRenderer(voxelRenderer);
    }

    @Override
    public boolean handleInteraction(Entity entity, ChunkCell voxelContext, Input input) {
        return false;
    }

	/*@Override
	public VoxelDynamicRenderer getVoxelRenderer(CellData info) {
		return signRenderer;
	}*/

    @Override
    public void onPlace(FutureCell cell, WorldModificationCause cause) throws IllegalBlockModificationException {
        // We don't create the components here, as the cell isn't actually changed yet!
        int x = cell.getX();
        int y = cell.getY();
        int z = cell.getZ();

        if (cause != null && cause instanceof Entity) {
            Vector3d blockLocation = new Vector3d(x + 0.5, y, z + 0.5);
            blockLocation.sub(((Entity) cause).getLocation());
            blockLocation.negate();

            Vector2f direction = new Vector2f((float) (double) blockLocation.x(), (float) (double) blockLocation.z());
            direction.normalize();
            // System.out.println("x:"+direction.x+"y:"+direction.y);

            double asAngle = Math.acos(direction.y()) / Math.PI * 180;
            asAngle *= -1;
            if (direction.x() < 0)
                asAngle *= -1;

            // asAngle += 180.0;

            asAngle %= 360.0;
            asAngle += 360.0;
            asAngle %= 360.0;

            // System.out.println(asAngle);

            int meta = (int) (16 * asAngle / 360);
            cell.setMetaData(meta);
        }
    }

    @Override
    public void whenPlaced(FreshChunkCell cell) {
        VoxelComponentSignText signTextComponent = new VoxelComponentSignText(cell.getComponents());
        cell.registerComponent("signData", signTextComponent);
    }

    /**
     * Gets the sign component from a chunkcell, assuming it is indeed a sign cell
     */
    public VoxelComponentSignText getSignData(ChunkCell context) {
        VoxelComponentSignText signTextComponent = (VoxelComponentSignText) context.getComponents().getVoxelComponent("signData");
        return signTextComponent;
    }

}
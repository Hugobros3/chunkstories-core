//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel.renderers;

import io.xol.chunkstories.api.rendering.voxel.VoxelBakerHighPoly;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;

public class VoxelWaterRenderer implements VoxelRenderer
{
	VoxelModel model;
	
	public VoxelWaterRenderer(VoxelModel model)
	{
		this.model = model;
	}

	@Override
	public int bakeInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData cell)
	{
		VoxelBakerHighPoly renderByteBuffer = chunkRenderer.getHighpolyBakerFor(LodLevel.ANY, ShadingType.LIQUIDS);
		return model.renderInto(renderByteBuffer, bakingContext, cell, chunk, cell.getX() & 0x1F, cell.getY() & 0x1F, cell.getZ() & 0x1F);
	}
}

package io.xol.chunkstories.core.voxel.renderers;

import io.xol.chunkstories.api.voxel.models.ChunkRenderer;
import io.xol.chunkstories.api.voxel.models.VoxelBakerHighPoly;
import io.xol.chunkstories.api.voxel.models.VoxelModel;
import io.xol.chunkstories.api.voxel.models.VoxelRenderer;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.voxel.models.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.voxel.models.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class VoxelWaterRenderer implements VoxelRenderer
{
	VoxelModel model;
	
	public VoxelWaterRenderer(VoxelModel model)
	{
		this.model = model;
	}

	@Override
	public int renderInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData cell)
	{
		VoxelBakerHighPoly renderByteBuffer = chunkRenderer.getHighpolyBakerFor(LodLevel.ANY, ShadingType.LIQUIDS);
		return model.renderInto(renderByteBuffer, bakingContext, cell, chunk, cell.getX() & 0x1F, cell.getY() & 0x1F, cell.getZ() & 0x1F);
	}
}

//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel.renderers;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerCubic;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer.ChunkRenderContext;

import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelSides;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;

/** Renders classic voxels as cubes ( and intelligently culls faces ) */
public class DefaultVoxelRenderer implements VoxelRenderer
{
	final Voxels store;
	
	public DefaultVoxelRenderer(Voxels store) {
		this.store = store;
	}
	
	@Override
	public int bakeInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData cell)
	{
		Voxel vox = cell.getVoxel();
		
		int i = cell.getX() & 0x1F;
		int k = cell.getY() & 0x1F;
		int j = cell.getZ() & 0x1F;
		
		VoxelBakerCubic vbc = chunkRenderer.getLowpolyBakerFor(LodLevel.ANY, ShadingType.OPAQUE);
		byte wavyVegetationFlag = 0;
		
		int vertices = 0;
		
		if (shallBuildWallArround(cell, 5) && (k != 0 || bakingContext.isBottomChunkLoaded()))
		{
			addQuadBottom(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.BOTTOM, cell), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(cell, 4) && (k != 31 || bakingContext.isTopChunkLoaded()))
		{
			addQuadTop(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.TOP, cell), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(cell, 2) && (i != 31 || bakingContext.isRightChunkLoaded()))
		{
			addQuadRight(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.RIGHT, cell), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(cell, 0) && (i != 0 || bakingContext.isLeftChunkLoaded()))
		{
			addQuadLeft(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.LEFT, cell), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(cell, 1) && (j != 31 || bakingContext.isFrontChunkLoaded()))
		{
			addQuadFront(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.FRONT, cell), wavyVegetationFlag);
			vertices += 6;
		}
		if (shallBuildWallArround(cell, 3) && (j != 0 || bakingContext.isBackChunkLoaded()))
		{
			addQuadBack(chunk, bakingContext, vbc, i, k, j, vox.getVoxelTexture(VoxelSides.BACK, cell), wavyVegetationFlag);
			vertices += 6;
		}
		
		vbc.reset();

		return vertices;
	}

	protected void addQuadTop(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 1f, 0f);
		rbbf.setWavyFlag(wavy != 0);
		
		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();
	}

	protected void addQuadBottom(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, -1f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx + 1, sy, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();
	}

	protected void addQuadRight(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(1f, 0f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();
	}

	protected void addQuadLeft(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(-1f, 0f, 0f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

	}

	protected void addQuadFront(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 0f, 1f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz + 1);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_FRONT_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz + 1);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_FRONT_LEFT);
		rbbf.endVertex();

	}

	protected void addQuadBack(Chunk c, ChunkRenderContext bakingContext, VoxelBakerCubic rbbf, int sx, int sy, int sz, VoxelTexture texture, byte wavy)
	{
		rbbf.usingTexture(texture);
		rbbf.setNormal(0f, 0f, -1f);
		rbbf.setWavyFlag(wavy != 0);

		rbbf.beginVertex(sx, sy + 1, sz);
		rbbf.setTextureCoordinates(1f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx, sy - 0, sz);
		rbbf.setTextureCoordinates(1f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_LEFT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy + 1, sz);
		rbbf.setTextureCoordinates(0f, 0f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.TOP_BACK_RIGHT);
		rbbf.endVertex();

		rbbf.beginVertex(sx + 1, sy - 0, sz);
		rbbf.setTextureCoordinates(0f, 1f);
		rbbf.setVoxelLightAuto(bakingContext.getCurrentVoxelLighter(), Corners.BOTTOM_BACK_RIGHT);
		rbbf.endVertex();
	}

	protected boolean shallBuildWallArround(CellData renderInfo, int face)
	{
		Voxel facing = renderInfo.getNeightborVoxel(face);
		Voxel voxel = renderInfo.getVoxel();

		if (voxel.getDefinition().isLiquid() && !facing.getDefinition().isLiquid())
			return true;
		
		//Facing.isSideOpaque
		if (!facing.isFaceOpaque(VoxelSides.values()[face].getOppositeSide(), renderInfo.getNeightborMetadata(face)) && (!voxel.sameKind(facing) || !voxel.getDefinition().isSelfOpaque()))
			return true;
		return false;
	}
}

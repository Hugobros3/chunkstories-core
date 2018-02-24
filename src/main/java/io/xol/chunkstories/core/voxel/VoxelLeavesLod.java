//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.voxel;

import io.xol.chunkstories.api.content.Content.Voxels;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerCubic;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer.ChunkRenderContext;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.voxel.VoxelDefinition;
import io.xol.chunkstories.api.voxel.textures.VoxelTexture;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.core.voxel.renderers.DefaultVoxelRenderer;

public class VoxelLeavesLod extends Voxel
{
	VoxelTexture baseTexture;
	VoxelTexture opaqueTexture;
	
	LodedLeavesBlocksRenderer renderer;
	
	public VoxelLeavesLod(VoxelDefinition type)
	{
		super(type);
		this.baseTexture = store.textures().getVoxelTextureByName(getName());
		this.opaqueTexture = store.textures().getVoxelTextureByName(getName() + "Opaque");
		this.renderer = new LodedLeavesBlocksRenderer(type.store());
	}
	
	@Override
	public VoxelRenderer getVoxelRenderer(CellData info) {
		return renderer;
	}
	
	class LodedLeavesBlocksRenderer extends DefaultVoxelRenderer {
		
		public LodedLeavesBlocksRenderer(Voxels store) {
			super(store);
		}

		@Override
		public int bakeInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData voxelInformations)
		{
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.LOW);
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.HIGH);
			return 0;
		}
		
		protected boolean shallBuildWallArround(CellData renderInfo, int face, LodLevel lodLevel)
		{
			//int baseID = renderInfo.data;
			Voxel facing = renderInfo.getNeightborVoxel(face);
			Voxel voxel = renderInfo.getVoxel();

			if (voxel.getDefinition().isLiquid() && !facing.getDefinition().isLiquid())
				return true;
			if (!facing.getDefinition().isOpaque() && ( (!voxel.sameKind(facing) || (lodLevel == LodLevel.HIGH && !voxel.getDefinition().isSelfOpaque())) ) )
				return true;
			return false;
		}
		
		public void renderLodVersion(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk, CellData voxelInformations, LodLevel lodLevel)
		{
			//Voxel vox = voxelInformations.getVoxel();
			//int src = voxelInformations.getData();
			
			int i = voxelInformations.getX() & 0x1F;
			int k = voxelInformations.getY() & 0x1F;
			int j = voxelInformations.getZ() & 0x1F;
			
			VoxelBakerCubic rawRBBF = chunkRenderer.getLowpolyBakerFor(lodLevel, ShadingType.OPAQUE);
			byte extraByte = 0;
			if (shallBuildWallArround(voxelInformations, 5, lodLevel))
			{
				if (k != 0 || bakingContext.isBottomChunkLoaded())
					addQuadBottom(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 4, lodLevel))
			{
				if (k != 31 || bakingContext.isTopChunkLoaded())
					addQuadTop(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 2, lodLevel))
			{
				if (i != 31 || bakingContext.isRightChunkLoaded())
					addQuadRight(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 0, lodLevel))
			{
				if (i != 0 || bakingContext.isLeftChunkLoaded())
					addQuadLeft(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 1, lodLevel))
			{
				if (j != 31 || bakingContext.isFrontChunkLoaded())
					addQuadFront(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(voxelInformations, 3, lodLevel))
			{
				if (j != 0 || bakingContext.isBackChunkLoaded())
					addQuadBack(chunk, bakingContext, rawRBBF, i, k, j, lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
		}
	}

}

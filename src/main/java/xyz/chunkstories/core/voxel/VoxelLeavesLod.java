//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.core.voxel;

import xyz.chunkstories.api.voxel.Voxel;
import xyz.chunkstories.api.voxel.VoxelDefinition;
import xyz.chunkstories.api.voxel.textures.VoxelTexture;

public class VoxelLeavesLod extends Voxel {
	private VoxelTexture baseTexture;
	private VoxelTexture opaqueTexture;

	//LodedLeavesBlocksRenderer renderer;

	public VoxelLeavesLod(VoxelDefinition definition) {
		super(definition);
		this.baseTexture = store().textures().get(getName());
		this.opaqueTexture = store().textures().get(getName() + "Opaque");
		//this.renderer = new LodedLeavesBlocksRenderer(type.store());
	}

	/*@Override
	public VoxelRenderer getVoxelRenderer(CellData info) {
		return renderer;
	}

	class LodedLeavesBlocksRenderer extends DefaultVoxelRenderer {

		public LodedLeavesBlocksRenderer(Voxels store) {
			super(store);
		}

		@Override
		public int bakeInto(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk,
				CellData voxelInformations) {
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.LOW);
			renderLodVersion(chunkRenderer, bakingContext, chunk, voxelInformations, LodLevel.HIGH);
			return 0;
		}

		protected boolean shallBuildWallArround(CellData renderInfo, int face, LodLevel lodLevel) {
			// int baseID = renderInfo.data;
			Voxel facing = renderInfo.getNeightborVoxel(face);
			Voxel voxel = renderInfo.getVoxel();

			if (voxel.getDefinition().isLiquid() && !facing.getDefinition().isLiquid())
				return true;
			if (!facing.getDefinition().isOpaque() && ((!voxel.sameKind(facing)
					|| (lodLevel == LodLevel.HIGH && !voxel.getDefinition().isSelfOpaque()))))
				return true;
			return false;
		}

		public void renderLodVersion(ChunkRenderer chunkRenderer, ChunkRenderContext bakingContext, Chunk chunk,
				CellData cell, LodLevel lodLevel) {
			Voxel vox = cell.getVoxel();
			// int src = voxelInformations.getData();

			int i = cell.getX() & 0x1F;
			int k = cell.getY() & 0x1F;
			int j = cell.getZ() & 0x1F;

			VoxelBakerCubic rawRBBF = chunkRenderer.getLowpolyBakerFor(lodLevel, ShadingType.OPAQUE);

			byte flags = 1;// Byte.parseByte(cell.getVoxel().getDefinition().resolveProperty("materialFlags",
							// "0"));
			if (lodLevel == LodLevel.LOW)
				flags = 0;
			rawRBBF.setMaterialFlags(flags);

			byte extraByte = (byte) (vox.getDefinition().resolveProperty("affectedByWind", "false").equals("true") ? 3
					: 0);
			if (shallBuildWallArround(cell, 5, lodLevel)) {
				if (k != 0 || bakingContext.isBottomChunkLoaded())
					addQuadBottom(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(cell, 4, lodLevel)) {
				if (k != 31 || bakingContext.isTopChunkLoaded())
					addQuadTop(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(cell, 2, lodLevel)) {
				if (i != 31 || bakingContext.isRightChunkLoaded())
					addQuadRight(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(cell, 0, lodLevel)) {
				if (i != 0 || bakingContext.isLeftChunkLoaded())
					addQuadLeft(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(cell, 1, lodLevel)) {
				if (j != 31 || bakingContext.isFrontChunkLoaded())
					addQuadFront(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
			if (shallBuildWallArround(cell, 3, lodLevel)) {
				if (j != 0 || bakingContext.isBackChunkLoaded())
					addQuadBack(chunk, bakingContext, rawRBBF, i, k, j,
							lodLevel == LodLevel.HIGH ? baseTexture : opaqueTexture, extraByte);
			}
		}
	}
*/
}

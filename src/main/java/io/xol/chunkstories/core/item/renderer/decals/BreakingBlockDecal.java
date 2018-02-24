//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.item.renderer.decals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.client.ClientContent;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerCubic;
import io.xol.chunkstories.api.rendering.voxel.VoxelBakerHighPoly;
import io.xol.chunkstories.api.rendering.voxel.VoxelRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.LodLevel;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkMeshDataSubtypes.ShadingType;
import io.xol.chunkstories.api.rendering.world.chunk.ChunkRenderer;
import io.xol.chunkstories.api.rendering.world.chunk.vertexlayout.BaseLayoutBaker;
import io.xol.chunkstories.api.voxel.VoxelSides.Corners;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.api.world.cell.CellData;
import io.xol.chunkstories.core.item.ItemMiningTool.MiningProgress;
import io.xol.chunkstories.core.voxel.renderers.DefaultVoxelRenderer;

public class BreakingBlockDecal {
	
	public final MiningProgress miningProgress;
	
	private final VertexBuffer vertexBuffer;
	private int size;
	
	public BreakingBlockDecal(MiningProgress miningProgress, RenderingInterface renderingInterface) {
		this.miningProgress = miningProgress;
		CellData ctx = miningProgress.loc.getWorld().peekSafely(miningProgress.loc);
		
		BreakingBlockDecalVoxelBaker bbdvb = new BreakingBlockDecalVoxelBaker(((WorldClient) ctx.getWorld()).getClient().getContent(), miningProgress.loc);
		
		ChunkRenderer chunkRenderer = new ChunkRenderer() {

			@Override
			public VoxelBakerHighPoly getHighpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return bbdvb;
			}

			@Override
			public VoxelBakerCubic getLowpolyBakerFor(LodLevel lodLevel, ShadingType renderPass)
			{
				return bbdvb;
			}
			
		};
		
		ChunkRenderer.ChunkRenderContext o2 = new ChunkRenderer.ChunkRenderContext()
		{
			
			private VoxelLighter voxeLighter = new VoxelLighter() {

				@Override
				public byte getSunlightLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getBlocklightLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getAoLevelForCorner(Corners corner)
				{
					return 0;
				}

				@Override
				public byte getSunlightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}

				@Override
				public byte getBlocklightLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}

				@Override
				public byte getAoLevelInterpolated(float vertX, float vertY, float vertZ)
				{
					return 0;
				}
				
			};

			@Override
			public boolean isTopChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isRightChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isLeftChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isFrontChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isBottomChunkLoaded()
			{
				return false;
			}
			
			@Override
			public boolean isBackChunkLoaded()
			{
				return false;
			}

			@Override
			public int getRenderedVoxelPositionInChunkX()
			{
				return ctx.getX() & 0x1f;
			}

			@Override
			public int getRenderedVoxelPositionInChunkY()
			{
				return ctx.getY() & 0x1f;
			}

			@Override
			public int getRenderedVoxelPositionInChunkZ()
			{
				return ctx.getZ() & 0x1f;
			}

			@Override
			public VoxelLighter getCurrentVoxelLighter()
			{
				return voxeLighter;
			}
		};
		
		//System.out.println(ctx.getVoxel().getVoxelRenderer(ctx));
		VoxelRenderer voxelRenderer = ctx.getVoxelRenderer();
		if(voxelRenderer == null) {
			voxelRenderer = new DefaultVoxelRenderer(ctx.getVoxel().store());
		}
		
		voxelRenderer.bakeInto(chunkRenderer, o2, ctx.getWorld().getChunkWorldCoordinates(miningProgress.loc), ctx);
		
		ByteBuffer buffer = bbdvb.cum();
		
		vertexBuffer = renderingInterface.newVertexBuffer();
		vertexBuffer.uploadData(buffer);
	}
	
	ThreadLocal<ArrayList<Float>> memesAreDreams = new ThreadLocal<ArrayList<Float>>() {
		@Override
		protected ArrayList<Float> initialValue() {
			return new ArrayList<Float>();
		}
	};
	
	class BreakingBlockDecalVoxelBaker extends BaseLayoutBaker implements VoxelBakerCubic, VoxelBakerHighPoly {

		protected BreakingBlockDecalVoxelBaker(ClientContent content, Location loc) {
			super(content);
			
			memesAreMyReality = memesAreDreams.get();
			this.loc = loc;
			
			cx = (int)Math.floor(loc.x / 32);
			cy = (int)Math.floor(loc.y / 32);
			cz = (int)Math.floor(loc.z / 32);
		}

		Location loc;
		ArrayList<Float> memesAreMyReality;
		int cx,cy,cz;

		Vector3f position = new Vector3f();
		Vector3f positionF = new Vector3f();
		Vector3f normal2 = new Vector3f();
		Vector3f scrap = new Vector3f();
		
		Vector3f currentVertex = new Vector3f();
		
		@Override
		public void beginVertex(int i0, int i1, int i2) {
			this.beginVertex((float)i0, (float)i1, (float)i2);
		}
		
		public void beginVertex(float f0, float f1, float f2) {
			currentVertex.set(f0, f1, f2);
		}

		@Override
		public void beginVertex(Vector3fc vertex) {
			currentVertex.set(vertex);
		}

		@Override
		public void beginVertex(Vector3dc vertex) {
			currentVertex.set(vertex);
		}
		
		@Override
		public void endVertex() {
			float f0 = currentVertex.x;
			float f1 = currentVertex.y;
			float f2 = currentVertex.z;
			
			memesAreMyReality.add(f0 + cx * 32);
			memesAreMyReality.add(f1 + cy * 32);
			memesAreMyReality.add(f2 + cz * 32);
			
			position.set(f0 + cx * 32, f1 + cy * 32, f2 + cz * 32);
			
			float fx = f0 - (float)loc.x;
			float fy = f1 - (float)loc.y;
			float fz = f2 - (float)loc.z;
			positionF.set(fx, fy, fz);
			
			//normal2.set(i0 / 512f - 1.0f, i1 / 512f - 1.0f, i2 / 512f - 1.0f);
			//normal2.normalize();
			
			normal2.set(normal);
			
			scrap.set(normal2.z, normal2.x, normal2.y);
			
			float scrapzer = scrap.dot(positionF);
			/*if(scrapzer < -0.89)
				scrapzer += 1f;*/
			memesAreMyReality.add(scrapzer);
			
			//System.out.println(scrapzer);
			
			scrap.set(normal2.y, normal2.z, normal2.x);
			
			scrapzer = scrap.dot(positionF);
			/*if(scrapzer < -0.89)
				scrapzer += 1f;*/
			memesAreMyReality.add(scrapzer);
			
			size++;
		}
		
		public ByteBuffer cum() {
			
			ByteBuffer buffer = ByteBuffer.allocateDirect(4 * memesAreMyReality.size());//MemoryUtil.memAlloc(4 * memesAreMyReality.size());
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			for(float f : memesAreMyReality) {
				buffer.putFloat(f);
			}
			buffer.flip();
			memesAreMyReality.clear();
			
			return buffer;
		}
	}
	
	public void render(RenderingInterface renderingInterface) {
		ShaderInterface decalsShader = renderingInterface.useShader("decal_cracking");

		renderingInterface.getCamera().setupShader(decalsShader);
		
		renderingInterface.setCullingMode(CullingMode.DISABLED);
		renderingInterface.setBlendMode(BlendMode.MIX);
		renderingInterface.getRenderTargetManager().setDepthMask(false);

		int phases = 6;
		int phase = (int) (phases * miningProgress.progress);
		
		decalsShader.setUniform1f("textureScale", 1f / 6);
		decalsShader.setUniform1f("textureStart", 1f / 6 * phase);
		
		Texture2D diffuseTexture = renderingInterface.textures().getTexture("./textures/voxel_cracking.png");//decalType.getTexture();
		diffuseTexture.setTextureWrapping(true);
		diffuseTexture.setLinearFiltering(false);
		
		renderingInterface.bindAlbedoTexture(diffuseTexture);
		
		renderingInterface.bindAttribute("vertexIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 3, 4 * (3 + 2), 0));
		renderingInterface.bindAttribute("texCoordIn", vertexBuffer.asAttributeSource(VertexFormat.FLOAT, 2, 4 * (3 + 2), 4 * 3));
		
		renderingInterface.draw(Primitive.TRIANGLE, 0, size);
		
		renderingInterface.flush();
		
		renderingInterface.getRenderTargetManager().setDepthMask(true);
	}

	public void destroy() {
		vertexBuffer.destroy();
	}
}

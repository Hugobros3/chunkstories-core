package io.xol.chunkstories.core.rendering.passes;

import java.util.Map;

import io.xol.chunkstories.api.rendering.RenderPass;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingPipeline;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class FarTerrainPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;

	Texture2DRenderTarget shadedBuffer = null;
	Texture2DRenderTarget specularBuffer = null;
	Texture2DRenderTarget zBuffer;
	
	RenderTargetAttachementsConfiguration fbo = null;
	
	public FarTerrainPass(RenderingPipeline pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		this.zBuffer = (Texture2DRenderTarget) inputs.get("zBuffer");	
		this.shadedBuffer = (Texture2DRenderTarget) inputs.get("shadedBuffer");
		this.specularBuffer = (Texture2DRenderTarget) inputs.get("specularity");
		
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(zBuffer, shadedBuffer, specularBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		worldRenderer.getFarTerrainRenderer().renderTerrain(renderer, worldRenderer.getChunksRenderer().getRenderedChunksMask(renderer.getCamera()));
	}

	@Override
	public void onScreenResize(int width, int height) {
		
	}
}

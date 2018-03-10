package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class FarTerrainPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;

	Texture2DRenderTarget shadedBuffer = null;
	Texture2DRenderTarget specularBuffer = null;
	Texture2DRenderTarget zBuffer;
	
	RenderTargetsConfiguration fbo = null;
	
	public FarTerrainPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
	}

	@Override
	public void onResolvedInputs() {
		this.zBuffer = (Texture2DRenderTarget) resolvedInputs.get("zBuffer");	
		this.shadedBuffer = (Texture2DRenderTarget) resolvedInputs.get("shadedBuffer");
		this.specularBuffer = (Texture2DRenderTarget) resolvedInputs.get("specularityBuffer");
		
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

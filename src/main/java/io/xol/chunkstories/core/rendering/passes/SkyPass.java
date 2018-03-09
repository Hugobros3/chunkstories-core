package io.xol.chunkstories.core.rendering.passes;

import java.util.Map;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderPass;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingPipeline;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;

public class SkyPass extends RenderPass {

	final SkyRenderer skyRenderer;
	
	public final Texture2DRenderTarget rbShaded;
	public final Texture2DRenderTarget rbZBuffer;
	public final RenderTargetAttachementsConfiguration fbo;
	
	public SkyPass(RenderingPipeline pipeline, String name, SkyRenderer skyRenderer) {
		super(pipeline, name, new String[]{}, new String[] {"shadedBuffer, zBuffer!"});
		
		this.skyRenderer = skyRenderer;
		
		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		this.rbShaded = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
		this.rbZBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.DEPTH_RENDERBUFFER, gameWindow.getWidth(), gameWindow.getHeight());
		
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(rbZBuffer, rbShaded);
		
		this.resolvedOutputs.put("shadedBuffer", rbShaded);
		this.resolvedOutputs.put("zBuffer", rbZBuffer);
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();
		
		skyRenderer.render(renderer);
	}

	@Override
	public void onScreenResize(int w, int h) {
		this.fbo.resizeFBO(w, h);
	}

}

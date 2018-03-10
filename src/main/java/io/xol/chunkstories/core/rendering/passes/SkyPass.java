package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;

public class SkyPass extends RenderPass {

	final SkyRenderer skyRenderer;
	
	public final Texture2DRenderTarget rbShaded;
	public final Texture2DRenderTarget rbZBuffer;
	public final RenderTargetsConfiguration fbo;
	
	public SkyPass(RenderPasses pipeline, String name, SkyRenderer skyRenderer) {
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
	public void onResolvedInputs() {
		
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();
		
		skyRenderer.render(renderer);
	}

	@Override
	public void onScreenResize(int w, int h) {
		this.fbo.resize(w, h);
	}

}

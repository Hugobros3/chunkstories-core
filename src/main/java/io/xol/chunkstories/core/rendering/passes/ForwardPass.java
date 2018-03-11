package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class ForwardPass extends RenderPass {

	RenderTargetsConfiguration fbo = null;
	Texture2DRenderTarget shadedBuffer = null;
	Texture2DRenderTarget zBuffer = null;

	final WorldRenderer worldRenderer;
	final World world;
	
	public ForwardPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
	}

	@Override
	public void onResolvedInputs() {
		this.shadedBuffer = (Texture2DRenderTarget) resolvedInputs.get("shadedBuffer");
		this.zBuffer = (Texture2DRenderTarget) resolvedInputs.get("zBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(zBuffer, shadedBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		worldRenderer.getParticlesRenderer().renderParticles(renderer);
		worldRenderer.getWorldEffectsRenderer().renderEffects(renderer);
	}

	@Override
	public void onScreenResize(int width, int height) {
		
	}

}

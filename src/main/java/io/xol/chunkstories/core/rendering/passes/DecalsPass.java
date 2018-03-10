package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class DecalsPass extends RenderPass {
	
	final WorldRenderer worldRenderer;
	final World world;

	Texture2DRenderTarget albedoBuffer = null;
	Texture2DRenderTarget zBuffer = null;
	RenderTargetsConfiguration fbo = null;
	
	public DecalsPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);

		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
	}

	@Override
	public void onResolvedInputs() {
		this.albedoBuffer = (Texture2DRenderTarget) resolvedInputs.get("albedoBuffer");
		this.zBuffer = (Texture2DRenderTarget) resolvedInputs.get("zBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(zBuffer, albedoBuffer);
		
		this.resolvedOutputs.put("albedoBuffer", albedoBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		
		renderer.setBlendMode(BlendMode.MIX);
		renderer.setCullingMode(CullingMode.DISABLED);
		renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

		Shader decalsShader = renderer.useShader("decals");
		renderer.getCamera().setupShader(decalsShader);
		
		renderer.getRenderTargetManager().setDepthMask(false);
		worldRenderer.getDecalsRenderer().renderDecals(renderer);
		
		renderer.getRenderTargetManager().setDepthMask(true);
	}

	@Override
	public void onScreenResize(int width, int height) {
		
	}
}

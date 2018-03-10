package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;

public class DefferedLightsPass extends RenderPass {

	RenderTargetsConfiguration fbo = null;
	Texture2DRenderTarget shadedBuffer = null;
	
	public DefferedLightsPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
	}

	@Override
	public void onResolvedInputs() {
		this.shadedBuffer = (Texture2DRenderTarget) resolvedInputs.get("shadedBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, shadedBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		renderer.getRenderTargetManager().setConfiguration(fbo);
		
		// Deffered lightning
		// Disable depth read/write
		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.getRenderTargetManager().setDepthMask(false);

		Shader lightShader = renderer.useShader("light");

		//Required info
		/*renderer.bindTexture2D("zBuffer", this.renderBuffers.rbZBuffer);
		renderer.bindTexture2D("albedoBuffer", this.renderBuffers.rbAlbedo);
		renderer.bindTexture2D("normalBuffer", this.renderBuffers.rbNormal);*/

		//System.out.println("wow");
		//Parameters
		lightShader.setUniform1f("powFactor", 5f);
		renderer.getCamera().setupShader(lightShader);
		//Blend parameters

		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.ADD);

		renderer.getLightsRenderer().renderPendingLights(renderer);
		//Cleanup
		renderer.getRenderTargetManager().setDepthMask(true);

		renderer.setBlendMode(BlendMode.MIX);
		renderer.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
	}

	@Override
	public void onScreenResize(int width, int height) {
		// TODO Auto-generated method stub

	}

}

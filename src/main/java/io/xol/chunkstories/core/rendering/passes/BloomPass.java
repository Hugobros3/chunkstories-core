package io.xol.chunkstories.core.rendering.passes;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGB_HDR;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class BloomPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	private Texture2DRenderTarget bloomBuffer, blurBuffer;
	private RenderTargetsConfiguration fboBloom, fboBlur;
	
	public BloomPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		bloomBuffer = pipeline.getRenderingInterface().newTexture2D(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
		fboBloom = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, bloomBuffer);
		
		blurBuffer = pipeline.getRenderingInterface().newTexture2D(RGB_HDR, gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
		fboBlur = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, blurBuffer);
		
		this.resolvedOutputs.put("bloomBuffer", bloomBuffer);
	}

	@Override
	public void onResolvedInputs() {
		
	}

	@Override
	public void render(RenderingInterface renderer) {
		/*worldRenderer.renderBuffers.rbShaded.setLinearFiltering(true);
		worldRenderer.renderBuffers.rbBloom.setLinearFiltering(true);
		worldRenderer.renderBuffers.rbBlurTemp.setLinearFiltering(true);*/

		bloomBuffer.setLinearFiltering(true);
		
		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);
		
		Shader bloomShader = renderer.useShader("bloom");

		bloomShader.setUniform1f("apertureModifier", 1.0f);
		bloomShader.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);

		renderer.getRenderTargetManager().setConfiguration(fboBloom);
		renderer.drawFSQuad();

		// Blur bloom
		
		// Vertical pass
		renderer.getRenderTargetManager().setConfiguration(fboBlur);

		Shader blurV = renderer.useShader("blurV");
		blurV.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);
		blurV.setUniform1f("lookupScale", 1);
		renderer.bindTexture2D("inputTexture", bloomBuffer);
		renderer.drawFSQuad();

		// Horizontal pass
		renderer.getRenderTargetManager().setConfiguration(fboBloom);

		Shader blurH = renderer.useShader("blurH");
		blurH.setUniform2f("screenSize", renderer.getWindow().getWidth() / 2f, renderer.getWindow().getHeight() / 2f);
		renderer.bindTexture2D("inputTexture", blurBuffer);
		renderer.drawFSQuad();

		renderer.getRenderTargetManager().setConfiguration(fboBlur);

		blurV = renderer.useShader("blurV");

		blurV.setUniform2f("screenSize", renderer.getWindow().getWidth() / 4f, renderer.getWindow().getHeight() / 4f);
		blurV.setUniform1f("lookupScale", 1);
		renderer.bindTexture2D("inputTexture", bloomBuffer);
		renderer.drawFSQuad();

		// Horizontal pass
		renderer.getRenderTargetManager().setConfiguration(fboBloom);

		blurH = renderer.useShader("blurH");
		blurH.setUniform2f("screenSize", renderer.getWindow().getWidth() / 4f, renderer.getWindow().getHeight() / 4f);
		renderer.bindTexture2D("inputTexture", blurBuffer);
		renderer.drawFSQuad();
	}

	@Override
	public void onScreenResize(int width, int height) {
		this.fboBloom.resize(width, height);
		this.fboBlur.resize(width, height);
	}

}

package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.WorldClient;

public class ReflectionsPass extends RenderPass {

	WorldRenderer worldRenderer;
	WorldClient world;
	final SkyRenderer skyRenderer;

	RenderTargetsConfiguration fbo = null;
	Texture2DRenderTarget reflectionsBuffer;
	
	public ReflectionsPass(RenderPasses pipeline, String name, String[] requires, String[] exports, SkyRenderer skyRenderer) {
		super(pipeline, name, requires, exports);

		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
		
		this.skyRenderer = skyRenderer;

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		this.reflectionsBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, reflectionsBuffer);
		
		this.resolvedOutputs.put("reflectionsBuffer", reflectionsBuffer);
	}

	@Override
	public void onResolvedInputs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void render(RenderingInterface renderer) {
		Shader reflectionsShader = renderer.useShader("reflections");

		//This isn't a depth-buffered pass.
		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);

		renderer.getRenderTargetManager().setConfiguration(fbo);
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();

		//Required to execute SSR
		/*renderer.bindTexture2D("shadedBuffer", worldRenderer.renderBuffers.rbShaded);
		renderer.bindTexture2D("zBuffer", worldRenderer.renderBuffers.rbZBuffer);
		renderer.bindTexture2D("normalBuffer", worldRenderer.renderBuffers.rbNormal);
		renderer.bindTexture2D("specularityBuffer", worldRenderer.renderBuffers.rbSpecularity);
		renderer.bindTexture2D("voxelLightBuffer", worldRenderer.renderBuffers.rbVoxelLight);*/

		//TODO renderer.bindCubemap("environmentCubemap", worldRenderer.renderBuffers.rbEnvironmentMap);

		// Matrices for screen-space transformations
		renderer.getCamera().setupShader(reflectionsShader);
		skyRenderer.setupShader(reflectionsShader);

		//Disable depth writing and run the deal
		renderer.getRenderTargetManager().setDepthMask(false);
		renderer.drawFSQuad();
		renderer.getRenderTargetManager().setDepthMask(true);
	}

	@Override
	public void onScreenResize(int width, int height) {
		this.fbo.resize(width, height);
	}

}

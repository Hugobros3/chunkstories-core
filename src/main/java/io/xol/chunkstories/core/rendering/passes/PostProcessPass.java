package io.xol.chunkstories.core.rendering.passes;

import java.util.Map;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderPass;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingPipeline;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class PostProcessPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	
	RenderTargetAttachementsConfiguration fbo = null;
	Texture2D shadedBuffer = null;
	Texture2D zBuffer = null;
	
	Texture2DRenderTarget postProcessed;
	
	final ShadowPass shadowPass;
	
	public PostProcessPass(RenderingPipeline pipeline, String name, String[] requires, ShadowPass shadowPass) {
		super(pipeline, name, requires, new String[] {"finalBuffer"});
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		this.shadowPass = shadowPass;
		
		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		postProcessed = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_8, gameWindow.getWidth(), gameWindow.getHeight());
		fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, postProcessed);
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		
		//We need the shadedBuffer buffer from the previous passes
		this.shadedBuffer = (Texture2D) inputs.get("shadedBuffer");
		this.zBuffer = (Texture2D) inputs.get("zBuffer");
	}

	@Override
	public void render(RenderingInterface renderer) {
		if(shadedBuffer != null) {
			renderer.getRenderTargetManager().setConfiguration(fbo);
			renderer.getRenderTargetManager().clearBoundRenderTargetAll();
			
			//TODO mix in the reflections earlier ?
			//Texture2D bloomRendered = RenderingConfig.doBloom ? bloomRenderer.renderBloom(renderingContext) : null;
			
			Layer layer = renderer.getWindow().getLayer();
			
			float pauseFade = 0.0f; //(layer instanceof Ingame) ? ((Ingame)layer).getPauseOverlayFade() : 0;
			
			pauseFade = layer.getClass().getName().contains("Ingame") ? 0 : 1;
			
			// We render to the screen.
			renderer.getRenderTargetManager().setConfiguration(null);

			renderer.setDepthTestMode(DepthTestMode.DISABLED);
			renderer.setBlendMode(BlendMode.DISABLED);

			ShaderInterface postProcess = renderer.useShader("postprocess");
			
			renderer.bindTexture2D("shadedBuffer", shadedBuffer);
			renderer.bindTexture2D("zBuffer", zBuffer);
			
			/*renderingContext.bindTexture2D("albedoBuffer", renderBuffers.rbAlbedo);
			renderingContext.bindTexture2D("depthBuffer", renderBuffers.rbZBuffer);
			renderingContext.bindTexture2D("normalBuffer", renderBuffers.rbNormal);
			renderingContext.bindTexture2D("voxelLightBuffer", renderBuffers.rbVoxelLight);
			renderingContext.bindTexture2D("specularityBuffer", renderBuffers.rbSpecularity);
			renderingContext.bindTexture2D("materialBuffer", renderBuffers.rbMaterial);
			renderingContext.bindTexture2D("shadowMap", renderBuffers.rbShadowMap);
			renderingContext.bindTexture2D("reflectionsBuffer", renderBuffers.rbReflections);
			renderingContext.bindCubemap("environmentMap", renderBuffers.rbEnvironmentMap);
			//If we enable bloom
			if(bloomRendered != null)
				renderingContext.bindTexture2D("bloomBuffer", bloomRendered);
			renderingContext.bindTexture2D("ssaoBuffer", renderBuffers.rbSSAO);
			renderingContext.bindTexture2D("debugBuffer", renderBuffers.rbReflections);*/
			
			renderer.bindTexture2D("pauseOverlayTexture", renderer.textures().getTexture("./textures/gui/darker.png"));

			//TODO make an underwater pass
			//Voxel vox = world.peekSafely(renderingContext.getCamera().getCameraPosition()).getVoxel();
			//postProcess.setUniform1f("underwater", vox.getDefinition().isLiquid() ? 1 : 0);
			postProcess.setUniform1f("underwater", 0.0f);
			
			postProcess.setUniform1f("animationTimer", worldRenderer.getAnimationTimer()); //TODO
			postProcess.setUniform1f("pauseOverlayFade", pauseFade);

			
			if(renderer.renderingConfig().isDoShadows()) {
				renderer.bindTexture2D("shadowMap", (Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));
				//System.out.println((Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));
				
				postProcess.setUniform1f("shadowMapResolution", ((Texture2D) shadowPass.resolvedOutputs.get("shadowMap")).getWidth());
				postProcess.setUniform1f("shadowVisiblity", shadowPass.getShadowVisibility());
				
				postProcess.setUniformMatrix4f("shadowMatrix", shadowPass.getShadowMatrix());
			}
				
			renderer.getCamera().setupShader(postProcess);
			//skyRenderer.setupShader(postProcess);

			postProcess.setUniform1f("apertureModifier", 1.0f);

			renderer.drawFSQuad();
			renderer.flush();
		}
	}

	@Override
	public void onScreenResize(int w, int h) {
		
	}

}

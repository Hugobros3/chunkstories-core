package io.xol.chunkstories.core.rendering.passes;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.DEPTH_RENDERBUFFER;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGB_HDR;

import java.util.Map;
import java.util.Map.Entry;

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
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class ApplySunlightPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	RenderTargetAttachementsConfiguration fbo = null;
	Texture2DRenderTarget shadedBuffer = null;
	
	Texture2D albedoBuffer, normalBuffer, voxelLightBuffer, zBuffer;
	
	public ApplySunlightPass(RenderingPipeline pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		for(Entry<String, Texture> e : inputs.entrySet())
			System.out.println(e.getKey() + ":" + e.getValue());
		
		this.shadedBuffer = (Texture2DRenderTarget) inputs.get("shadedBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, shadedBuffer);
		
		this.albedoBuffer = (Texture2DRenderTarget) inputs.get("albedo");	
		this.normalBuffer = (Texture2DRenderTarget) inputs.get("normals");	
		this.voxelLightBuffer = (Texture2DRenderTarget) inputs.get("voxelLight");
		
		this.zBuffer = (Texture2DRenderTarget) inputs.get("zBuffer");	
	}

	@Override
	public void render(RenderingInterface renderer) {
		ShaderInterface applyShadowsShader = renderer.useShader("shadows_apply");

		world.getGenerator().getEnvironment().setupShadowColors(renderer, applyShadowsShader);

		//renderingContext.bindTexture2D("giBuffer", this.giRenderer.giTexture());
		//applyShadowsShader.setUniform1f("accumulatedSamples", giRenderer.accumulatedSamples);

		applyShadowsShader.setUniform1f("animationTimer", worldRenderer.getAnimationTimer());
		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderer.getCamera().getCameraPosition()));

		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);

		renderer.getRenderTargetManager().setConfiguration(fbo);

		float lightMultiplier = 1.0f;

		applyShadowsShader.setUniform1f("brightnessMultiplier", lightMultiplier);

		if(albedoBuffer == normalBuffer || albedoBuffer == zBuffer)
			System.out.println("well fuck"+normalBuffer);
		
		renderer.bindTexture2D("albedoBuffer", albedoBuffer);
		renderer.bindTexture2D("depthBuffer", zBuffer);
		renderer.bindTexture2D("normalBuffer", normalBuffer);
		//TODO materials
		//renderingContext.bindTexture2D("specularityBuffer", renderBuffers.rbSpecularity);
		renderer.bindTexture2D("voxelLightBuffer", voxelLightBuffer);


		renderer.bindTexture2D("blockLightmap", renderer.textures().getTexture("./textures/environement/light.png"));

		Texture2D lightColors = renderer.textures().getTexture("./textures/environement/lightcolors.png");
		renderer.bindTexture2D("lightColors", lightColors);

		//renderingContext.bindCubemap("environmentCubemap", renderBuffers.rbEnvironmentMap);

		/*if(sun_shadowMap != null)
		{
			renderingContext.bindTexture2D("shadowMap", rbShadowMap);
			
			applyShadowsShader.setUniform1f("shadowMapResolution", RenderingConfig.shadowMapResolutions);
			applyShadowsShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());
			
			applyShadowsShader.setUniformMatrix4f("shadowMatrix", sun_shadowMap.getShadowTransformationMatrix());
		}*/

		// Matrices for screen-space transformations
		renderer.getCamera().setupShader(applyShadowsShader);
		worldRenderer.getSkyRenderer().setupShader(applyShadowsShader);

		renderer.getRenderTargetManager().setDepthMask(false);
		renderer.drawFSQuad();
		renderer.getRenderTargetManager().setDepthMask(true);
	}

	@Override
	public void onScreenResize(int width, int height) {
	}
}

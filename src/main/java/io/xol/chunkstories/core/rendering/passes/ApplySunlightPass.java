package io.xol.chunkstories.core.rendering.passes;

import java.util.Map.Entry;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.core.rendering.passes.gi.GiPass;

public class ApplySunlightPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	RenderTargetsConfiguration fbo = null;
	Texture2DRenderTarget shadedBuffer = null;
	
	//Texture2D albedoBuffer, normalBuffer, voxelLightBuffer, zBuffer;
	
	final ShadowPass shadowPass;
	
	public ApplySunlightPass(RenderPasses pipeline, String name, String[] requires, String[] exports, ShadowPass shadowPass) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
		
		this.shadowPass = shadowPass;
	}

	@Override
	public void onResolvedInputs() {
		this.shadedBuffer = (Texture2DRenderTarget) resolvedInputs.get("shadedBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, shadedBuffer);
		
		/*this.albedoBuffer = (Texture2DRenderTarget) resolvedInputs.get("albedoBuffer");	
		this.normalBuffer = (Texture2DRenderTarget) resolvedInputs.get("normals");	
		this.voxelLightBuffer = (Texture2DRenderTarget) resolvedInputs.get("voxelLight");
		
		this.zBuffer = (Texture2DRenderTarget) resolvedInputs.get("zBuffer");*/	
	}

	@Override
	public void render(RenderingInterface renderer) {
		Shader applyShadowsShader = renderer.useShader("shadows_apply");

		world.getGenerator().getEnvironment().setupShadowColors(renderer, applyShadowsShader);
		
		applyShadowsShader.setUniform1f("animationTimer", worldRenderer.getAnimationTimer());
		applyShadowsShader.setUniform1f("overcastFactor", world.getWeather());
		applyShadowsShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderer.getCamera().getCameraPosition()));

		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);

		renderer.getRenderTargetManager().setConfiguration(fbo);

		float lightMultiplier = 1.0f;

		applyShadowsShader.setUniform1f("brightnessMultiplier", lightMultiplier);

		RenderPass giPass = this.pipeline.getRenderPass("gi");
		if(giPass != null && giPass instanceof GiPass) {
			
			GiPass gi = (GiPass)giPass;
			renderer.bindTexture2D("giBuffer", gi.giTexture());
			renderer.bindTexture2D("giConfidence", gi.confidenceTexture());
			applyShadowsShader.setUniform1f("accumulatedSamples", gi.accumulatedSamples);
			//System.out.println("samples:"+gi.accumulatedSamples );
		}
		
		/*if(albedoBuffer == normalBuffer || albedoBuffer == zBuffer)
			System.out.println("well fuck"+normalBuffer);
		
		renderer.bindTexture2D("albedoBuffer", albedoBuffer);
		renderer.bindTexture2D("depthBuffer", zBuffer);
		renderer.bindTexture2D("normalBuffer", normalBuffer);
		//TODO materials
		//renderingContext.bindTexture2D("specularityBuffer", renderBuffers.rbSpecularity);
		renderer.bindTexture2D("voxelLightBuffer", voxelLightBuffer);*/


		renderer.textures().getTexture("./textures/environement/light.png").setTextureWrapping(false);
		renderer.bindTexture2D("blockLightmap", renderer.textures().getTexture("./textures/environement/light.png"));

		Texture2D lightColors = renderer.textures().getTexture("./textures/environement/lightcolors.png");
		renderer.textures().getTexture("./textures/environement/lightcolors.png").setTextureWrapping(false);
		renderer.bindTexture2D("lightColors", lightColors);

		//renderingContext.bindCubemap("environmentCubemap", renderBuffers.rbEnvironmentMap);

		if(renderer.renderingConfig().isDoShadows()) {
			renderer.bindTexture2D("shadowMap", (Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));
			
			applyShadowsShader.setUniform1f("shadowMapResolution", ((Texture2D) shadowPass.resolvedOutputs.get("shadowMap")).getWidth());
			applyShadowsShader.setUniform1f("shadowVisiblity", shadowPass.getShadowVisibility());
			
			applyShadowsShader.setUniformMatrix4f("shadowMatrix", shadowPass.getShadowMatrix());
		}

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

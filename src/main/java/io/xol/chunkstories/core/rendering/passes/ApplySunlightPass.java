//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
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

	final ShadowPass shadowPass;

	public ApplySunlightPass(RenderPasses pipeline, String name, String[] requires, String[] exports,
			ShadowPass shadowPass) {
		super(pipeline, name, requires, exports);

		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		this.shadowPass = shadowPass;
	}

	@Override
	public void onResolvedInputs() {
		this.shadedBuffer = (Texture2DRenderTarget) resolvedInputs.get("shadedBuffer");
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, shadedBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		Shader shadows_apply = renderer.useShader("shadows_apply");
		world.getGenerator().getEnvironment().setupShadowColors(renderer, shadows_apply);

		shadows_apply.setUniform1f("animationTimer", worldRenderer.getAnimationTimer());
		shadows_apply.setUniform1f("overcastFactor", world.getWeather());
		shadows_apply.setUniform1f("wetness",
				world.getGenerator().getEnvironment().getWorldWetness(renderer.getCamera().getCameraPosition()));

		renderer.bindCubemap("irradianceMap",
				worldRenderer.getRenderingInterface().textures().getCubemap("./textures/pbr_test/irradiance"));
		renderer.bindCubemap("unfiltered",
				worldRenderer.getRenderingInterface().textures().getCubemap("./textures/pbr_test/unfiltered"));
		renderer.bindTexture2D("brdfLUT",
				worldRenderer.getRenderingInterface().textures().getTexture("./textures/pbr_test/brdf.png"));

		worldRenderer.getRenderingInterface().textures().getTexture("./textures/pbr_test/brdf.png")
				.setTextureWrapping(false);
		worldRenderer.getRenderingInterface().textures().getTexture("./textures/pbr_test/brdf.png")
				.setLinearFiltering(true);

		renderer.setDepthTestMode(DepthTestMode.DISABLED);
		renderer.setBlendMode(BlendMode.DISABLED);
		renderer.getRenderTargetManager().setConfiguration(fbo);

		float lightMultiplier = 1.0f;
		shadows_apply.setUniform1f("brightnessMultiplier", lightMultiplier);

		RenderPass giPass = this.pipeline.getRenderPass("gi");
		if (giPass != null && giPass instanceof GiPass) {
			GiPass gi = (GiPass) giPass;
			renderer.bindTexture2D("giBuffer", gi.giTexture());
			renderer.bindTexture2D("giConfidence", gi.confidenceTexture());
			shadows_apply.setUniform1f("accumulatedSamples", gi.accumulatedSamples);
		}

		renderer.textures().getTexture("./textures/environement/light.png").setTextureWrapping(false);
		renderer.bindTexture2D("blockLightmap", renderer.textures().getTexture("./textures/environement/light.png"));

		Texture2D lightColors = renderer.textures().getTexture("./textures/environement/lightcolors.png");
		renderer.textures().getTexture("./textures/environement/lightcolors.png").setTextureWrapping(false);
		renderer.bindTexture2D("lightColors", lightColors);

		if (shadowPass != null) {
			renderer.bindTexture2D("shadowMap", (Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));

			shadows_apply.setUniform1f("shadowMapResolution",
					((Texture2D) shadowPass.resolvedOutputs.get("shadowMap")).getWidth());
			shadows_apply.setUniform1f("shadowVisiblity", shadowPass.getShadowVisibility());

			shadows_apply.setUniformMatrix4f("shadowMatrix", shadowPass.getShadowMatrix());
		}

		// Matrices for screen-space transformations
		renderer.getCamera().setupShader(shadows_apply);
		worldRenderer.getSkyRenderer().setupShader(shadows_apply);

		renderer.getRenderTargetManager().setDepthMask(false);
		renderer.drawFSQuad();
		renderer.getRenderTargetManager().setDepthMask(true);
	}

	@Override
	public void onScreenResize(int width, int height) {
	}
}

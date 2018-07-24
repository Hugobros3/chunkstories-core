//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.passes;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class PostProcessPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;

	RenderTargetsConfiguration fbo = null;
	Texture2D shadedBuffer = null;
	Texture2D zBuffer = null;

	Texture2DRenderTarget postProcessed;

	final ShadowPass shadowPass;

	public PostProcessPass(RenderPasses pipeline, String name, String[] requires, ShadowPass shadowPass) {
		super(pipeline, name, requires, new String[] { "finalBuffer" });
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		this.shadowPass = shadowPass;

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		postProcessed = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_8, gameWindow.getWidth(),
				gameWindow.getHeight());
		fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, postProcessed);
	}

	@Override
	public void onResolvedInputs() {

		// We need the shadedBuffer buffer from the previous passes
		this.shadedBuffer = (Texture2D) resolvedInputs.get("shadedBuffer");
		this.zBuffer = (Texture2D) resolvedInputs.get("zBuffer");

		this.resolvedOutputs.put("finalBuffer", postProcessed);
	}

	@Override
	public void render(RenderingInterface renderer) {
		if (shadedBuffer != null) {
			renderer.getRenderTargetManager().setConfiguration(fbo);
			// renderer.getRenderTargetManager().clearBoundRenderTargetAll();

			// TODO mix in the reflections earlier ?
			// Texture2D bloomRendered = RenderingConfig.doBloom ?
			// bloomRenderer.renderBloom(renderingContext) : null;

			Layer layer = renderer.getWindow().getLayer();

			float pauseFade = 0.0f; // (layer instanceof Ingame) ? ((Ingame)layer).getPauseOverlayFade() : 0;

			pauseFade = layer.getClass().getName().contains("Ingame") ? 0 : 1;

			// We render to the screen.
			// renderer.getRenderTargetManager().setConfiguration(null);

			renderer.setDepthTestMode(DepthTestMode.DISABLED);
			renderer.setBlendMode(BlendMode.DISABLED);

			Shader postProcess = renderer.useShader("postprocess");

			renderer.bindTexture2D("shadedBuffer", shadedBuffer);
			renderer.bindTexture2D("zBuffer", zBuffer);

			renderer.bindTexture2D("pauseOverlayTexture", renderer.textures().getTexture("./textures/gui/darker.png"));

			renderer.textures().getTexture("./textures/post/colour_map.png").setTextureWrapping(false);
			renderer.textures().getTexture("./textures/post/colour_map.png").setLinearFiltering(true);
			renderer.bindTexture2D("colourMap", renderer.textures().getTexture("./textures/post/colour_map.png"));

			// TODO make an underwater pass
			// Voxel vox =
			// world.peekSafely(renderingContext.getCamera().getCameraPosition()).getVoxel();
			// postProcess.setUniform1f("underwater", vox.getDefinition().isLiquid() ? 1 :
			// 0);
			postProcess.setUniform1f("underwater", 0.0f);

			postProcess.setUniform1f("animationTimer", worldRenderer.getAnimationTimer()); // TODO
			postProcess.setUniform1f("pauseOverlayFade", pauseFade);

			if (shadowPass != null
					&& renderer.getClient().getConfiguration().getBooleanOption("client.rendering.shadows")) {
				renderer.bindTexture2D("shadowMap", (Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));
				// System.out.println((Texture2D) shadowPass.resolvedOutputs.get("shadowMap"));

				postProcess.setUniform1f("shadowMapResolution",
						((Texture2D) shadowPass.resolvedOutputs.get("shadowMap")).getWidth());
				postProcess.setUniform1f("shadowVisiblity", shadowPass.getShadowVisibility());

				postProcess.setUniformMatrix4f("shadowMatrix", shadowPass.getShadowMatrix());
			}

			renderer.getCamera().setupShader(postProcess);
			worldRenderer.getSkyRenderer().setupShader(postProcess);

			renderer.drawFSQuad();
		}
	}

	@Override
	public void onScreenResize(int w, int h) {
		fbo.resize(w, h);
	}

}

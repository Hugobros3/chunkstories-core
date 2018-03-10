package io.xol.chunkstories.core.rendering.passes;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.voxel.Voxel;
import io.xol.chunkstories.api.world.WorldClient;

public class WaterPass extends RenderPass {
	
	WorldRenderer worldRenderer;
	WorldClient world;
	final SkyRenderer skyRenderer;
	private RenderTargetsConfiguration waterRefractionFbo, fboGBuffers;
	private Texture2DRenderTarget waterTempTexture;
	private Texture2DRenderTarget zBuffer, albedoBuffer, normalBuffer, voxelLightBuffer, specularityBuffer, materialsBuffer;
	
	public WaterPass(RenderPasses pipeline, String name, String[] requires, String[] exports, SkyRenderer skyRenderer) {
		super(pipeline, name, requires, exports);
		
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
		
		this.skyRenderer = skyRenderer;

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		this.waterTempTexture = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_HDR, gameWindow.getWidth(), gameWindow.getHeight());
		
		this.waterRefractionFbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(null, waterTempTexture);
	}

	@Override
	public void onResolvedInputs() {
		zBuffer = (Texture2DRenderTarget) resolvedInputs.get("zBuffer");
		albedoBuffer = (Texture2DRenderTarget) resolvedInputs.get("albedoBuffer");
		normalBuffer = (Texture2DRenderTarget) resolvedInputs.get("normalBuffer");
		voxelLightBuffer = (Texture2DRenderTarget) resolvedInputs.get("voxelLightBuffer");
		specularityBuffer = (Texture2DRenderTarget) resolvedInputs.get("specularityBuffer");
		materialsBuffer = (Texture2DRenderTarget) resolvedInputs.get("materialsBuffer");
		
		fboGBuffers = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(
				zBuffer, albedoBuffer, normalBuffer, voxelLightBuffer, specularityBuffer, materialsBuffer);	
		
		this.resolvedOutputs.put("zBuffer", zBuffer);
		this.resolvedOutputs.put("albedoBuffer", albedoBuffer);
		this.resolvedOutputs.put("normalBuffer", normalBuffer);
		this.resolvedOutputs.put("voxelLightBuffer", voxelLightBuffer);
		this.resolvedOutputs.put("zBuspecularityBufferffer", specularityBuffer);
		this.resolvedOutputs.put("materialsBuffer", materialsBuffer);
	}

	@Override
	public void render(RenderingInterface renderer) {
		//if(true)
		//	return;
		
		renderer.setBlendMode(BlendMode.MIX);
		renderer.setCullingMode(CullingMode.DISABLED);

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		// We do water in two passes : 
		// one for computing the refracted color and putting it in shaded buffer, and another one to read it back and blend it
		for (int pass = 1; pass <= 2; pass++)
		{
			Shader liquidBlocksShader = renderer.useShader("blocks_liquid_pass" + pass);

			liquidBlocksShader.setUniform1f("viewDistance", renderer.renderingConfig().getViewDistance());

			renderer.bindTexture2D("normalTextureDeep", renderer.textures().getTexture("./textures/water/deep.png"));
			renderer.bindTexture2D("normalTextureShallow", renderer.textures().getTexture("./textures/water/shallow.png"));

			renderer.bindTexture2D("lightColors", renderer.textures().getTexture("./textures/environement/light.png"));

			Texture2D blocksAlbedoTexture = gameWindow.getClient().getContent().voxels().textures().getDiffuseAtlasTexture();
			renderer.bindAlbedoTexture(blocksAlbedoTexture);
			
			liquidBlocksShader.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
			skyRenderer.setupShader(liquidBlocksShader);
			liquidBlocksShader.setUniform1f("animationTimer", worldRenderer.getAnimationTimer());

			renderer.getCamera().setupShader(liquidBlocksShader);

			//Underwater flag
			Voxel vox = world.peekSafely(renderer.getCamera().getCameraPosition()).getVoxel();
			liquidBlocksShader.setUniform1f("underwater", vox.getDefinition().isLiquid() ? 1 : 0);

			if (pass == 1) {
				renderer.getRenderTargetManager().setConfiguration(waterRefractionFbo);
				renderer.getRenderTargetManager().clearBoundRenderTargetAll();
				
				renderer.bindTexture2D("readbackAlbedoBufferTemp", albedoBuffer);
				renderer.bindTexture2D("readbackVoxelLightBufferTemp", voxelLightBuffer);
				renderer.bindTexture2D("readbackDepthBufferTemp", zBuffer);

				renderer.getRenderTargetManager().setDepthMask(false);
			} else if (pass == 2) {
				renderer.getRenderTargetManager().setConfiguration(fboGBuffers);
				
				renderer.setBlendMode(BlendMode.DISABLED);
				renderer.bindTexture2D("readbackShadedBufferTemp", waterTempTexture);

				renderer.getRenderTargetManager().setDepthMask(true);
			}

			renderer.setObjectMatrix(new Matrix4f());
			worldRenderer.getChunksRenderer().renderChunks(renderer);
		}
	}

	@Override
	public void onScreenResize(int width, int height) {
		waterRefractionFbo.resize(width, height);
	}

}

package io.xol.chunkstories.core.rendering.passes;

import java.util.Map;

import org.joml.Matrix4f;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderPass;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.RenderingPipeline;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class GBuffersOpaquePass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	
	public final Texture2DRenderTarget albedoBuffer, normalBuffer, voxelLightBuffer, speculairtyBuffer, materialsBuffer;
	
	private RenderTargetAttachementsConfiguration fbo;
	private Texture2DRenderTarget rbZBuffer;

	public GBuffersOpaquePass(RenderingPipeline pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		albedoBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
		normalBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGB_8, gameWindow.getWidth(), gameWindow.getHeight());
		voxelLightBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RG_8, gameWindow.getWidth(), gameWindow.getHeight());
		speculairtyBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RED_8, gameWindow.getWidth(), gameWindow.getHeight());
		materialsBuffer = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RED_8UI, gameWindow.getWidth(), gameWindow.getHeight());
		
		this.resolvedOutputs.put("albedo", albedoBuffer);
		this.resolvedOutputs.put("normals", normalBuffer);
		this.resolvedOutputs.put("voxelLight", voxelLightBuffer);
		this.resolvedOutputs.put("specularity", speculairtyBuffer);
		this.resolvedOutputs.put("material", materialsBuffer);
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		rbZBuffer = (Texture2DRenderTarget) inputs.get("zBuffer");
		
		fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(rbZBuffer, albedoBuffer, normalBuffer, voxelLightBuffer, speculairtyBuffer, materialsBuffer);
	}

	@Override
	public void render(RenderingInterface renderingInterface) {
		if(fbo != null) {
			//System.out.println("bah go");
			GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
			
			renderingInterface.getRenderTargetManager().setConfiguration(fbo);
			renderingInterface.getRenderTargetManager().clearBoundRenderTargetAll();
			
			// Set fixed-function parameters
			renderingInterface.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
			renderingInterface.setBlendMode(BlendMode.DISABLED);
			renderingInterface.setCullingMode(CullingMode.COUNTERCLOCKWISE);
			
			ShaderInterface opaqueBlocksShader = renderingInterface.useShader("blocks_opaque");
			
			Texture2D blocksAlbedoTexture;
			Texture2D blocksNormalTexture;
			Texture2D blocksMaterialTexture;
			
			blocksAlbedoTexture = gameWindow.getClient().getContent().voxels().textures().getDiffuseAtlasTexture();
			blocksNormalTexture = gameWindow.getClient().getContent().voxels().textures().getNormalAtlasTexture();
			blocksMaterialTexture = gameWindow.getClient().getContent().voxels().textures().getMaterialAtlasTexture();
			
			renderingInterface.bindAlbedoTexture(blocksAlbedoTexture);
			renderingInterface.bindNormalTexture(blocksNormalTexture);
			renderingInterface.bindMaterialTexture(blocksMaterialTexture);

			renderingInterface.bindTexture2D("lightColors", renderingInterface.textures().getTexture("./textures/environement/light.png"));
			renderingInterface.bindTexture2D("vegetationColorTexture", worldRenderer.getWorld().getGenerator().getEnvironment().getGrassTexture(renderingInterface));
			//renderingInterface.bindTexture2D("vegetationColorTexture", getGrassTexture());

			//Set texturing arguments
			blocksAlbedoTexture.setTextureWrapping(false);
			blocksAlbedoTexture.setLinearFiltering(false);
			blocksAlbedoTexture.setMipMapping(false);
			blocksAlbedoTexture.setMipmapLevelsRange(0, 4);

			blocksNormalTexture.setTextureWrapping(false);
			blocksNormalTexture.setLinearFiltering(false);
			blocksNormalTexture.setMipMapping(false);
			blocksNormalTexture.setMipmapLevelsRange(0, 4);

			blocksMaterialTexture.setTextureWrapping(false);
			blocksMaterialTexture.setLinearFiltering(false);
			blocksMaterialTexture.setMipMapping(false);
			blocksMaterialTexture.setMipmapLevelsRange(0, 4);

			//World stuff
			opaqueBlocksShader.setUniform1f("mapSize", world.getSizeInChunks() * 32);
			opaqueBlocksShader.setUniform1f("overcastFactor", world.getWeather());
			opaqueBlocksShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderingInterface.getCamera().getCameraPosition()));
			opaqueBlocksShader.setUniform1f("time", worldRenderer.getAnimationTimer());

			opaqueBlocksShader.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
			renderingInterface.getCamera().setupShader(opaqueBlocksShader);

			renderingInterface.setObjectMatrix(new Matrix4f());
			
			worldRenderer.getChunksRenderer().renderChunks(renderingInterface);
			
			// Select shader
			ShaderInterface entitiesShader = renderingInterface.useShader("entities");

			entitiesShader.setUniform1f("viewDistance", renderingInterface.getClient().renderingConfig().getViewDistance());
			//entitiesShader.setUniform1f("shadowVisiblity", shadower.getShadowVisibility());

			renderingInterface.bindTexture2D("lightColors", renderingInterface.textures().getTexture("./textures/environement/light.png"));
			entitiesShader.setUniform3f("blockColor", 1f, 1f, 1f);
			entitiesShader.setUniform1f("time", worldRenderer.getAnimationTimer());

			entitiesShader.setUniform1f("overcastFactor", world.getWeather());
			entitiesShader.setUniform1f("wetness", world.getGenerator().getEnvironment().getWorldWetness(renderingInterface.getCamera().getCameraPosition()));

			renderingInterface.currentShader().setUniform1f("useColorIn", 0.0f);
			renderingInterface.currentShader().setUniform1f("useNormalIn", 1.0f);

			renderingInterface.getCamera().setupShader(entitiesShader);

			worldRenderer.getChunksRenderer().renderChunksExtras(renderingInterface);
			worldRenderer.getEntitiesRenderer().renderEntities(renderingInterface);
			
			worldRenderer.getParticlesRenderer().renderParticles(renderingInterface);
		}
	}

	@Override
	public void onScreenResize(int width, int height) {
		fbo.resizeFBO(width, height);
	}
}

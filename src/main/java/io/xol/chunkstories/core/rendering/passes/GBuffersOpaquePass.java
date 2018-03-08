package io.xol.chunkstories.core.rendering.passes;

import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RED_8;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RED_8UI;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGBA_8BPP;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RGB_8;
import static io.xol.chunkstories.api.rendering.textures.TextureFormat.RG_8;

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
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class GBuffersOpaquePass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	
	public final Texture2DRenderTarget rbAlbedo, rbNormal, rbVoxelLight, rbSpecularity, rbMaterial;
	
	private RenderTargetAttachementsConfiguration fbo;
	private Texture2DRenderTarget rbZBuffer;

	public GBuffersOpaquePass(RenderingPipeline pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();

		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();
		rbAlbedo = pipeline.getRenderingInterface().newTexture2D(RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
		rbNormal = pipeline.getRenderingInterface().newTexture2D(RGB_8, gameWindow.getWidth(), gameWindow.getHeight());
		rbVoxelLight = pipeline.getRenderingInterface().newTexture2D(RG_8, gameWindow.getWidth(), gameWindow.getHeight());
		rbSpecularity = pipeline.getRenderingInterface().newTexture2D(RED_8, gameWindow.getWidth(), gameWindow.getHeight());
		rbMaterial = pipeline.getRenderingInterface().newTexture2D(RED_8UI, gameWindow.getWidth(), gameWindow.getHeight());
		
		this.resolvedOutputs.put("albedo", rbAlbedo);
		this.resolvedOutputs.put("normals", rbNormal);
		this.resolvedOutputs.put("voxelLight", rbVoxelLight);
		this.resolvedOutputs.put("specularity", rbSpecularity);
		this.resolvedOutputs.put("material", rbMaterial);
	}

	@Override
	public void resolvedInputs(Map<String, Texture> inputs) {
		rbZBuffer = (Texture2DRenderTarget) inputs.get("zBuffer");
		
		fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(rbZBuffer, rbAlbedo, rbNormal, rbVoxelLight, rbSpecularity, rbMaterial);
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
		}
	}

	@Override
	public void onScreenResize(int width, int height) {
		fbo.resizeFBO(width, height);
	}
}

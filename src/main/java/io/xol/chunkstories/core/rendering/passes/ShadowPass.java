//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.passes;

import org.joml.Matrix4f;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.Texture2DRenderTarget;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.WorldClient;

public class ShadowPass extends RenderPass
{
	WorldRenderer worldRenderer;
	WorldClient world;
	
	final SkyRenderer skyRenderer;
	
	RenderTargetsConfiguration fbo = null;
	Texture2DRenderTarget shadowDepthTexture;

	private Matrix4f shadowMatrix;
	
	public ShadowPass(RenderPasses pipeline, String name, String[] requires, String[] exports, SkyRenderer skyRenderer)
	{
		super(pipeline, name, requires, exports);
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
		
		this.skyRenderer = skyRenderer;

		this.shadowDepthTexture = pipeline.getRenderingInterface().newTexture2D(TextureFormat.DEPTH_SHADOWMAP, 512, 512);
		this.fbo = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(shadowDepthTexture);
		
		this.resolvedOutputs.put("shadowMap", shadowDepthTexture);
	}

	public void render(RenderingInterface renderingContext)
	{
		if (this.getShadowVisibility() == 0f)
			return; // No shadows at night :)
		
		GameWindow gameWindow = pipeline.getRenderingInterface().getWindow();

		//Resize the texture if needed
		int shadowMapTextureSize = renderingContext.renderingConfig().getShadowMapResolutions();
		if(shadowDepthTexture.getWidth() != shadowMapTextureSize) {
			fbo.resize(shadowMapTextureSize, shadowMapTextureSize);
		}
		
		//The size of the shadow range depends on the shadowmap resolution
		int shadowRange = 128;
		if (shadowMapTextureSize > 1024)
			shadowRange = 192;
		else if (shadowMapTextureSize > 2048)
			shadowRange = 256;
		
		int shadowDepthRange = 200;
		
		//Builds the shadow matrix
		Matrix4f depthProjectionMatrix = new Matrix4f().ortho(-shadowRange, shadowRange, -shadowRange, shadowRange, -shadowDepthRange, shadowDepthRange);//MatrixHelper.getOrthographicMatrix(-shadowRange, shadowRange, -shadowRange, shadowRange, -shadowDepthRange, shadowDepthRange);
		Matrix4f depthViewMatrix = new Matrix4f().lookAt(skyRenderer.getSunPosition(), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
		Matrix4f shadowMVP = new Matrix4f();
		
		depthProjectionMatrix.mul(depthViewMatrix, shadowMVP);
		//Matrix4f.mul(depthProjectionMatrix, depthViewMatrix, shadowMVP);
		
		shadowMatrix = new Matrix4f(shadowMVP);
		Vector3dc posd = renderingContext.getCamera().getCameraPosition();
		Vector3f pos = new Vector3f((float)posd.x(), (float)posd.y(), (float)posd.z());
		pos.negate();
		
		shadowMVP.translate(pos);

		//Set appropriate fixed function stuff
		renderingContext.setCullingMode(CullingMode.COUNTERCLOCKWISE);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);

		//Bind relevant FBO and clear it
		renderingContext.getRenderTargetManager().setConfiguration(fbo);
		renderingContext.getRenderTargetManager().clearBoundRenderTargetZ(1.0f);

		Shader shadowsPassShader = renderingContext.useShader("shadows");
		
		shadowsPassShader.setUniform1f("animationTimer", worldRenderer.getAnimationTimer());
		shadowsPassShader.setUniformMatrix4f("depthMVP", shadowMVP);
		shadowsPassShader.setUniform1f("isUsingInstancedData", 0f);
		shadowsPassShader.setUniform1f("useVoxelCoordinates", 1f);

		Texture2D blocksAlbedoTexture = gameWindow.getClient().getContent().voxels().textures().getDiffuseAtlasTexture();
		renderingContext.bindAlbedoTexture(blocksAlbedoTexture);
		renderingContext.setObjectMatrix(null);
		
		//We render the world from that perspective
		shadowsPassShader.setUniform1f("allowForWavyStuff", 1); //Hackish way of enabling the shader input for the fake "wind" effect vegetation can have
		worldRenderer.getChunksRenderer().renderChunks(renderingContext);
		
		shadowsPassShader.setUniform1f("allowForWavyStuff", 0); //In turn, disabling it while we do the entities
		worldRenderer.getEntitiesRenderer().renderEntities(renderingContext);
	}

	public float getShadowVisibility()
	{
		float worldTime = (world.getTime() % 10000 + 10000) % 10000;
		int start = 2500;
		int end = 7500;
		if (worldTime < start || worldTime > end + 500)
			return 0;
		else if (worldTime < start + 500)
			return (worldTime - start) / 500f;
		else if (worldTime > end)
			return 1 - (worldTime - end) / 500f;
		else
			return 1;
	}
	
	public Matrix4f getShadowMatrix() {
		return shadowMatrix;
	}

	@Override
	public void onResolvedInputs() {
		
	}

	@Override
	public void onScreenResize(int width, int height) {
		
	}
}

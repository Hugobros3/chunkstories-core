//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.passes.gi;

import org.joml.Matrix4f;
import org.joml.Vector3d;

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

public class GiPass extends RenderPass {

	final WorldRenderer worldRenderer;
	final World world;
	
	private Texture2DRenderTarget accumulationA, accumulationB;
	private Texture2DRenderTarget zBufferA, zBufferB;
	private Texture2DRenderTarget confidenceA, confidenceB;
	private RenderTargetsConfiguration fboAccumulationA, fboAccumulationB;
	
	private NearbyVoxelsVolumeTexture voxels4gi;
	
	public int accumulatedSamples = 0;

	public GiPass(RenderPasses pipeline, String name, String[] requires, String[] exports) {
		super(pipeline, name, requires, exports);
		this.worldRenderer = pipeline.getWorldRenderer();
		this.world = worldRenderer.getWorld();
		
		this.voxels4gi = new NearbyVoxelsVolumeTexture(worldRenderer);

		float giScale = 2.0f;
		accumulationA = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGBA_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		accumulationB = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RGBA_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		
		confidenceA = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RED_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		confidenceB = pipeline.getRenderingInterface().newTexture2D(TextureFormat.RED_16F, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		
		zBufferA = pipeline.getRenderingInterface().newTexture2D(TextureFormat.DEPTH_RENDERBUFFER, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		zBufferB = pipeline.getRenderingInterface().newTexture2D(TextureFormat.DEPTH_RENDERBUFFER, (int) (worldRenderer.getWindow().getWidth() / giScale), (int) (worldRenderer.getWindow().getHeight() / giScale));
		
		fboAccumulationA = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(zBufferA, accumulationA, confidenceA);
		fboAccumulationB = pipeline.getRenderingInterface().getRenderTargetManager().newConfiguration(zBufferB, accumulationB, confidenceB);
		
		this.resolvedOutputs.put("giBuffer", accumulationA);
	}
	
	Vector3d cameraPosition = new Vector3d();
	Vector3d cameraDirection = new Vector3d();
	
	Vector3d oldCameraPosition = new Vector3d();
	Vector3d oldCameraDirection = new Vector3d();

	Matrix4f previousProjectionMatrix = new Matrix4f();
	Matrix4f previousModelViewMatrix = new Matrix4f();
	Matrix4f previousProjectionMatrixInv = new Matrix4f();
	Matrix4f previousModelViewMatrixInv = new Matrix4f();
	
	boolean renderingToA = false;
	
	public Texture2DRenderTarget giTexture() {
		return !renderingToA ? accumulationA : accumulationB;
	}

	public Texture2D confidenceTexture() {
		return !renderingToA ? confidenceA : confidenceB;
	}
	
	public void render(RenderingInterface renderer) {
		cameraPosition.set(renderer.getCamera().getCameraPosition());
		cameraDirection.set(renderer.getCamera().getViewDirection());

		Shader giShader = renderer.useShader("gi");
		
		renderer.getRenderTargetManager().setConfiguration(renderingToA ? fboAccumulationA : fboAccumulationB);
		renderer.setDepthTestMode(DepthTestMode.ALWAYS);
		renderer.setBlendMode(BlendMode.DISABLED);

		/*renderer.bindTexture2D("albedoBuffer", worldRenderer.renderBuffers.rbAlbedo);
		renderer.bindTexture2D("depthBuffer", worldRenderer.renderBuffers.rbZBuffer);
		renderer.bindTexture2D("normalBuffer", worldRenderer.renderBuffers.rbNormal);*/
		
		renderer.getRenderTargetManager().clearBoundRenderTargetAll();
		renderer.getRenderTargetManager().setDepthMask(true);
		
		renderer.bindTexture2D("previousBuffer", !renderingToA ? accumulationA : accumulationB);
		renderer.bindTexture2D("previousConfidence", !renderingToA ? confidenceA : confidenceB);
		renderer.bindTexture2D("previousZ", !renderingToA ? zBufferA : zBufferB);
		
		giShader.setUniformMatrix4f("previousProjectionMatrix", previousProjectionMatrix);
		giShader.setUniformMatrix4f("previousModelViewMatrix", previousModelViewMatrix);
		giShader.setUniformMatrix4f("previousProjectionMatrixInv", previousProjectionMatrixInv);
		giShader.setUniformMatrix4f("previousModelViewMatrixInv", previousModelViewMatrixInv);
		
		if(cameraPosition.distance(oldCameraPosition) != 0.0f || cameraDirection.distance(oldCameraDirection) != 0.0f) {
			//System.out.println("moved! : " + cameraPosition.distance(oldCameraPosition) + "or " + cameraDirection.distance(oldCameraDirection));
			giShader.setUniform1i("keepPreviousData", 0);
			accumulatedSamples = 0;
		} else {
			giShader.setUniform1i("keepPreviousData", 1);
		}
		
		accumulatedSamples++;

		voxels4gi.update(renderer);
		voxels4gi.setupForRendering(renderer);
		
		boolean lf = true;
		accumulationA.setLinearFiltering(lf);
		accumulationB.setLinearFiltering(lf);
		confidenceA.setLinearFiltering(lf);
		confidenceB.setLinearFiltering(lf);
		
		giShader.setUniform1f("animationTimer", worldRenderer.getAnimationTimer() + accumulatedSamples);
		giShader.setUniform1f("overcastFactor", worldRenderer.getWorld().getWeather());
		giShader.setUniform1f("wetness", worldRenderer.getWorld().getGenerator().getEnvironment().getWorldWetness(cameraPosition));

		worldRenderer.getSkyRenderer().setupShader(giShader);
		renderer.getCamera().setupShader(giShader);
		
		renderer.drawFSQuad();
		
		previousProjectionMatrix.set(renderer.getCamera().getProjectionMatrix4f());
		previousModelViewMatrix.set(renderer.getCamera().getModelViewMatrix4f());
		previousProjectionMatrixInv.set(renderer.getCamera().getProjectionMatrix4fInverted());
		previousModelViewMatrixInv.set(renderer.getCamera().getModelViewMatrix4fInverted());
		
		renderingToA = !renderingToA;
		
		oldCameraPosition.set(cameraPosition);
		oldCameraDirection.set(cameraDirection);
	}

	@Override
	public void onResolvedInputs() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onScreenResize(int width, int height) {

		float giScale = 2.0f;
		fboAccumulationA.resize((int) (width / giScale), (int) (height / giScale));
		fboAccumulationB.resize((int) (width / giScale), (int) (height / giScale));
	}
}

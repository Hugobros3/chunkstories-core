//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.sky;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.joml.Vector3f;

import io.xol.chunkstories.api.math.Math2;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.StateMachine.DepthTestMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.world.SkyRenderer;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

public class DefaultSkyRenderer implements SkyRenderer
{
	float dayTime = 0;
	
	WorldRenderer worldRenderer;
	World world;
	CloudsRenderer cloudsRenderer;
	VertexBuffer starsVertexBuffer;
	
	public DefaultSkyRenderer(WorldRenderer worldRenderer) {
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
		this.cloudsRenderer = new CloudsRenderer(worldRenderer, this);
	}

	public Vector3f getSunPosition() {
		float sunAzimuth = (float) ((dayTime + 0.75) * 360.0f * (Math.PI / 180.0));
		float sunElevation = (float) (0 + 90 * (Math.PI / 180.0));

		Vector3f lookAt = new Vector3f(0.5f + (float) (Math.sin(sunElevation) * Math.cos(sunAzimuth)), (float) (Math.sin(sunAzimuth)),
				0.3f + (float) (Math.cos(sunElevation) * Math.cos(sunAzimuth)));
		return lookAt.normalize();
	}
	
	public void render(RenderingInterface renderingContext)
	{
		try {
			long worldTime = renderingContext.getWorldRenderer().getWorld().getTime();
			this.dayTime = (worldTime % 10000) / 10000f;
		} catch(NullPointerException e) {
			
		}
		
		renderingContext.setDepthTestMode(DepthTestMode.DISABLED);
		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderingContext.setCullingMode(CullingMode.DISABLED);
		
		renderingContext.getRenderTargetManager().setDepthMask(false);

		Vector3f sunPosVector = getSunPosition();

		Shader skyShader = renderingContext.useShader("sky");
		
		skyShader.setUniform1f("overcastFactor", world.getWeather());

		skyShader.setUniform3f("sunPos", sunPosVector.x(), sunPosVector.y(), sunPosVector.z());
		skyShader.setUniform1f("time", dayTime);
		renderingContext.getCamera().setupShader(skyShader);

		renderingContext.drawFSQuad();

		renderingContext.setBlendMode(BlendMode.MIX);
		
		Shader starsShader = renderingContext.useShader("stars");
		
		starsShader.setUniform3f("sunPos", sunPosVector.x(), sunPosVector.y(), sunPosVector.z());
		starsShader.setUniform3f("color", 1f, 1f, 1f);
		renderingContext.getCamera().setupShader(starsShader);
		int NB_STARS = 500;
		
		//starsVertexBuffer = null;
		if (starsVertexBuffer == null)
		{
			starsVertexBuffer = renderingContext.newVertexBuffer();
			ByteBuffer bb = ByteBuffer.allocateDirect( 4 * 3 * NB_STARS);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			
			//FloatBuffer stars = bb.asFloatBuffer();
			//System.out.println(stars.order());
			for (int i = 0; i < NB_STARS; i++)
			{
				Vector3f star = new Vector3f((float) Math.random() * 2f - 1f, (float) Math.random(), (float) Math.random() * 2f - 1f);
				star.normalize();
				star.mul(100f);
				
				bb.putFloat(star.x());
				bb.putFloat(star.y());
				bb.putFloat(star.z());
				//stars.put(new float[] { star.x(), star.y(), star.z() });
			}
			bb.flip();
			starsVertexBuffer.uploadData(bb);
		}
		
		renderingContext.bindAttribute("vertexIn", starsVertexBuffer.asAttributeSource(VertexFormat.FLOAT, 3));
		renderingContext.draw(Primitive.POINT, 0, NB_STARS);
		
		renderingContext.getRenderTargetManager().setDepthMask(true);
		
		renderingContext.setBlendMode(BlendMode.DISABLED);
		renderingContext.setDepthTestMode(DepthTestMode.LESS_OR_EQUAL);
		
		cloudsRenderer.renderClouds(renderingContext);
	}

	public void setupShader(Shader shaderInterface)
	{
		float fogFactor = Math.min(Math.max(0.0f, world.getWeather() - 0.4f) / 0.1f, 1.0f);
		
		shaderInterface.setUniform1f("fogStartDistance", Math2.mix(192, 32, fogFactor));
		shaderInterface.setUniform1f("fogEndDistance", Math2.mix(1024, 384, fogFactor));
		
		Vector3f sunPos = this.getSunPosition();
		
		shaderInterface.setUniform3f("sunPos", sunPos.x(), sunPos.y(), sunPos.z());
		
		shaderInterface.setUniform1f("dayTime", this.dayTime);
		shaderInterface.setUniform1f("overcastFactor", world.getWeather());
	}
	
	public void destroy()
	{
		cloudsRenderer.destroy();
	}

	@Override
	public float getDayTime() {
		return dayTime;
	}
}

//
// This file is a part of the Chunk Stories Core codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.core.rendering.sky;

import io.xol.chunkstories.api.math.random.SeededSimplexNoiseGenerator;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;
import io.xol.chunkstories.api.world.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CloudsRenderer {
	final WorldRenderer worldRenderer;
	final World world;

	DefaultSkyRenderer skyRenderer;

	int worldSize;
	SeededSimplexNoiseGenerator generator;

	VertexBuffer cloudsMesh;
	int baked = 0;

	public CloudsRenderer(WorldRenderer worldRenderer, DefaultSkyRenderer skyRenderer) {
		this.worldRenderer = worldRenderer;
		this.world = worldRenderer.getWorld();
		this.skyRenderer = skyRenderer;

		this.cloudsMesh = worldRenderer.getRenderingInterface().newVertexBuffer();

		generator = new SeededSimplexNoiseGenerator(world.getWorldInfo().getSeed() + "cloudsKek");
		worldSize = world.getSizeInChunks();
	}

	int lastChunkX = -1;
	int lastChunkZ = -1;

	long lastBaked = 0;

	public void bake(RenderingInterface renderingContext) {
		int width = 128;

		int camChunkX = (int) (double) renderingContext.getCamera().getCameraPosition().x() / 32;
		int camChunkZ = (int) (double) renderingContext.getCamera().getCameraPosition().z() / 32;

		if (System.currentTimeMillis() - lastBaked < 5000 || (camChunkX == lastChunkX && camChunkZ == lastChunkZ))
			return;

		lastChunkX = camChunkX;
		lastChunkZ = camChunkZ;

		lastBaked = System.currentTimeMillis();

		// 64 patches of 2 triangles of 3 points of 3 coordinates of 4 byte floats + 3x4
		// bytes floats + float
		ByteBuffer bbuf = ByteBuffer.allocateDirect(4 * width * width * 2 * 3 * (4 * 3 + 4 * 3 + 4));
		bbuf.order(ByteOrder.LITTLE_ENDIAN);

		int octaves = 6;

		float max = 0;

		baked = 0;

		float time = 0.1f * ((System.currentTimeMillis() / 1000L) % (60 * 60 * 24));
		// time = System.currentTimeMillis();

		for (int x = camChunkX - width / 2; x < camChunkX + width / 2; x++)
			for (int z = camChunkZ - width / 2; z < camChunkZ + width / 2; z++) {
				// float intensity = generator.looped_noise(x, z, worldSize);

				float total = 0.0f;
				float maxAmplitude = 0.0f;
				float persistence = 0.5f;
				float amplitude = 1.0f;
				float freq = 1.0f;
				for (int i = 0; i < octaves; i++) {
					total += generator.looped_noise((x + time) * freq, z * freq, worldSize) * amplitude;
					// System.out.println(i+" "+generator.looped_noise(x * freq, z * freq,
					// worldSize) * amplitude);
					freq *= 2.0f;
					maxAmplitude += amplitude;
					amplitude *= persistence;
				}
				// return total / maxAmplitude;
				float intensity = total / maxAmplitude;

				if (intensity > max)
					max = intensity;

				intensity *= 10.0f * world.getWeather();

				if (intensity > 0.25) {
					// intensity = 1.0f;

					baked += 6;

					int xp = x * 32 + 32;
					int zp = z * 32 + 32;

					int xx = x * 32;
					int zz = z * 32;

					// Pos
					bbuf.putFloat(xp);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zp);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);

					// Pos
					bbuf.putFloat(xx);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zp);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);

					// Pos
					bbuf.putFloat(xx);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zz);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);

					// Pos
					bbuf.putFloat(xp);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zp);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);

					// Pos
					bbuf.putFloat(xx);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zz);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);

					// Pos
					bbuf.putFloat(xp);
					bbuf.putFloat(256.0f);
					bbuf.putFloat(zz);

					// Normal
					bbuf.putFloat(0.0f);
					bbuf.putFloat(-1.0f);
					bbuf.putFloat(0.0f);

					// Alpha
					bbuf.putFloat(intensity);
				}
			}

		if (baked <= 0)
			return;

		// System.out.println(max);

		bbuf.flip();
		cloudsMesh.uploadData(bbuf);
	}

	public void renderClouds(RenderingInterface renderingContext) {
		bake(renderingContext);

		if (baked <= 0)
			return;

		// ShaderProgram cloudsShader = ShadersLibrary.getShaderProgram("clouds");
		renderingContext.useShader("clouds");
		// renderingContext.setCurrentShader(cloudsShader);
		renderingContext.getCamera().setupShader(renderingContext.currentShader());
		skyRenderer.setupShader(renderingContext.currentShader());
		renderingContext.currentShader().setUniform3f("sunPos", skyRenderer.getSunPosition());
		Shader cloudsShader = renderingContext.currentShader();

		cloudsShader.setUniform1f("time", (world.getTime() % 10000) / 10000f);

		cloudsShader.setUniform1f("overcastFactor", world.getWeather());

		renderingContext.setCullingMode(CullingMode.DISABLED);
		// glDisable(GL_CULL_FACE);

		// cloudsMesh.bind();

		renderingContext.bindAttribute("vertexIn",
				cloudsMesh.asAttributeSource(VertexFormat.FLOAT, 3, (4 * 3 + 4 * 3 + 4), 0));
		renderingContext.bindAttribute("normalIn",
				cloudsMesh.asAttributeSource(VertexFormat.FLOAT, 3, (4 * 3 + 4 * 3 + 4), 4 * 3));
		renderingContext.bindAttribute("alphaIn",
				cloudsMesh.asAttributeSource(VertexFormat.FLOAT, 1, (4 * 3 + 4 * 3 + 4), 4 * 6));

		// renderingContext.setVertexAttributePointerLocation("vertexIn", 3, GL_FLOAT,
		// false, (4 * 3 + 4 * 3 + 4), 0);
		// renderingContext.setVertexAttributePointerLocation("normalIn", 3, GL_FLOAT,
		// false, (4 * 3 + 4 * 3 + 4), (4 * 3));
		// renderingContext.setVertexAttributePointerLocation("alphaIn", 1, GL_FLOAT,
		// false, (4 * 3 + 4 * 3 + 4), (4 * 3 + 4 * 3));

		renderingContext.draw(Primitive.TRIANGLE, 0, baked);
		// cloudsMesh.drawElementsTriangles(baked);
	}

	public void destroy() {
		cloudsMesh.destroy();
	}
}

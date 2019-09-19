#version 450

in vec3 vertexIn;
in vec3 colorIn;
in vec3 normalIn;
in vec2 texCoordIn;
in uint textureIdIn;

out vec3 vertex;
out vec4 color;
out vec3 normal;
out vec2 texCoord;
flat out int textureId;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.graphics.common.world.ChunkRenderInfo
//uniform ChunkRenderInfo chunkInfo;
instanced ChunkRenderInfo chunkInfo; // look mom, no uniforms !

#define PI 3.14159265359
#define pi PI
#include struct xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
uniform PrecomputedSimplexSeed simplexSeed;

#include simplex.glsl
#include noise.glsl

float heightAt(vec2 pos) {
	float height = 0.0;

	float baseHeight = ridgedNoise(pos, 5, 1.0, 0.5);
	height += baseHeight * 64.0;

	float mountainFactor = fractalNoise(pos + vec2(548.0, 330.0), 3, 0.5, 0.5);
	mountainFactor *= (1.0 + 0.125 * ridgedNoise(pos + vec2(14, 9977), 2, 4.0, 0.7));
	mountainFactor -= 0.3;
	mountainFactor /= (1.0 - 0.3);
	mountainFactor = clamp(mountainFactor, 0.0, 1.0);

	height += mountainFactor * 128;

	float plateaHeight = clamp(fractalNoise(pos+vec2(225.0, 321.0), 3, 1.0, 0.5) * 32.0 - 8.0, 0.0, 1.0);
	plateaHeight *= clamp(fractalNoise(pos+vec2(3158.0, 9711.0), 3, 0.125, 0.5) * 0.5 + 0.5, 0.0, 1.0);

	if(height > 48)
		height += plateaHeight * 24.0;
	else
		height += plateaHeight * baseHeight * 24.0;

	return height;
}

void main()
{
	vec3 vertexPos = vertexIn.xyz;
	vertexPos += vec3(chunkInfo.chunkX, chunkInfo.chunkY, chunkInfo.chunkZ) * 32.0;

	//if((mod(vertexPos.x , 32.0) < 16.0) == (mod(vertexPos.z , 32.0) < 16.0))
	//	vertexPos.y = 1+floor(heightAt(vec2(floor(vertexPos.x), floor(vertexPos.z))));

	//vec4 viewSpace = camera.viewMatrix * vec4(vertexPos, 1.0);
	//vec4 projected = camera.projectionMatrix * viewSpace;
	vec4 projected = camera.combinedViewProjectionMatrix * vec4(vertexPos, 1.0);


	vertex = vertexPos;

	color = vec4(colorIn.x, colorIn.y, colorIn.z, 1.0);
	//color = vec4(1.0);
	normal = camera.normalMatrix * normalIn;
	texCoord = texCoordIn;
	textureId = int(textureIdIn);

	gl_Position = projected;
}
#version 450

in uvec3 vertexIn;
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
uniform ChunkRenderInfo chunkInfo;

void main()
{
	vec3 vertexPos = vertexIn.xyz;
	vertexPos += vec3(chunkInfo.chunkX, chunkInfo.chunkY, chunkInfo.chunkZ) * 32.0;

	vec4 viewSpace = camera.viewMatrix * vec4(vertexPos, 1.0);
	vec4 projected = camera.projectionMatrix * viewSpace;

	vertex = vertexPos;
	color = vec4(colorIn, 1.0);
	normal = normalIn;
	texCoord = texCoordIn;
	textureId = int(textureIdIn);

	gl_Position = projected;
}
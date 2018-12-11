#version 450

in vec3 vertexIn;
in vec3 colorIn;
in vec3 normalIn;
in vec2 texCoordIn;
in uint textureIdIn;

out vec4 color;
out vec3 normal;
out vec2 texCoord;
flat out int textureId;

out float fogStrength;

#include struct <io.xol.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

void main()
{
	vec4 viewSpace = camera.viewMatrix * vec4(vertexIn.xyz, 1.0);
	vec4 projected = camera.projectionMatrix * viewSpace;

	fogStrength = clamp(1.0 - length(viewSpace) * 0.002, 0.0, 1.0);

	color = vec4(colorIn, 1.0);
	normal = normalIn;
	texCoord = texCoordIn;
	textureId = gl_VertexIndex / 2048;
	textureId = int(textureIdIn);
	/*texCoord = vec2(0.0);
	textureId = 1;*/

	gl_Position = projected;
}
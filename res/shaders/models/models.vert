#version 450

in vec3 vertexIn;
/*in vec3 colorIn;
in vec3 normalIn;
in vec2 texCoordIn;*/

out vec3 vertex;
out vec4 color;
out vec3 normal;
out vec2 texCoord;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.representation.ModelPosition
instanced ModelPosition modelPosition;

void main()
{
	vec3 vertexPos = vertexIn.xyz;

	vec4 viewSpace = camera.viewMatrix * modelPosition.matrix * vec4(vertexPos, 1.0);
	vec4 projected = camera.projectionMatrix * viewSpace;

	vertex = vertexPos;
	/*color = vec4(colorIn, 1.0);
	normal = normalIn;
	texCoord = texCoordIn;*/
	color = vec4(1.0, 0.0, 0.0, 1.0);
	normal = vec3(1.0);
	texCoord = vec2(0.0);

	gl_Position = projected;
}
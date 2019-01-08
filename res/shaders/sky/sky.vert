#version 450

//Vertex inputs
in vec2 vertexIn;

//Passed variables
out vec3 eyeDirection;

//Common camera matrices & uniforms
#include struct <xyz.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

void main()
{
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = normalize(camera.normalMatrixInverted * (camera.projectionMatrixInverted * transformedSS ).xyz);
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}
#version 450

//Vertex inputs
in vec2 vertexIn;

//Passed variables
out vec3 eyeDirection;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

void main()
{
	vec4 screenSpaceCoordinates = vec4(vertexIn.x, vertexIn.y, 0.0, 1.0);
	
	vec4 cameraSpaceCoordinates = (camera.projectionMatrixInverted * screenSpaceCoordinates);

	//eyeDirection = normalize(camera.normalMatrixInverted * (cameraSpaceCoordinates.xyz));
	eyeDirection = ((camera.viewMatrixInverted * vec4(cameraSpaceCoordinates.xyz, 0.0)).xyz);
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}
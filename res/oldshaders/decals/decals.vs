#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in vec2 texCoordIn;

//Passed variables
out vec3 eyeDirection;
out vec2 texCoordPassed;

//Misc
uniform vec3 camPos;
uniform vec3 sunPos;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
	texCoordPassed = texCoordIn;
	
	eyeDirection = normalize(vertexIn.xyz - camPos);
	
	vec4 vertexMod = vertexIn;
	
	//Back up 0.01 unit in world coordinates
	vertexMod.xyz -= normalize(vertexIn.xyz - camPos) * 0.01;
	
    gl_Position = projectionMatrix * modelViewMatrix * vertexMod;
}
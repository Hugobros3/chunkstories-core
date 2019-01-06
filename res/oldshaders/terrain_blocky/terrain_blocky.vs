#version 330

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
//in int voxelDataIn;
in vec4 normalIn;
in vec4 displacementIn;
in ivec4 indexIn;

//Passed variables
out vec3 vertexPassed;
out vec3 normalPassed;
out vec2 lightMapCoords;
out vec2 textureCoord;
out vec3 eyeDirection;
out float fogIntensity;
out float fresnelTerm;
flat out ivec4 indexPassed;
out vec4 displacementPassed;

//Complements vertexIn
uniform vec2 visualOffset;

//Sky
uniform float sunIntensity;
uniform vec3 sunPos;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;

uniform vec3 camPos;
//uniform float viewDistance;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

uniform usampler2DArray heights;
uniform int lodLevel;
uniform float textureLodLevel;
uniform int maskPresence;

//Unused
//flat out int voxelData;

uint access(usampler2DArray tex, vec2 coords) {
	
	if(coords.x <= 1.0) {
		if(coords.y <= 1.0) {
			return textureLod(tex, vec3(coords, indexIn.x), textureLodLevel).r;
		}
		else {
			return textureLod(tex, vec3(coords - vec2(0.0, 1.0), indexIn.y), textureLodLevel).r;
		}
	}
	else {
		if(coords.y <= 1.0) {
			return textureLod(tex, vec3(coords - vec2(1.0, 0.0), indexIn.z), textureLodLevel).r;
		}
		else {
			return textureLod(tex, vec3(coords - vec2(1.0), indexIn.w), textureLodLevel).r;
		}
	}
}

void main()
{
	indexPassed = indexIn;
	displacementPassed = displacementIn;
	
	//Output position
    gl_Position = vertexIn.xyzw;// + vec4(0.0, 0.0, 0.001, 0.0);
}

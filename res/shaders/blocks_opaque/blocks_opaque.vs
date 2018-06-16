#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec4 vertexIn;
in vec2 texCoordIn;
in uvec4 colorAndMaterialIn;
in vec4 normalIn;

//Passed variables
out vec2 texCoordPassed;
out vec2 worldLight;
out float fresnelTerm;
out vec3 normalPassed;
out vec4 vertexPassed;
out vec3 eyeDirection;
out float rainWetness;
flat out uint materialFlagsPassed;

//Lighthing
uniform float sunIntensity;

uniform float wetness;
uniform float time;
uniform float animationTimer;

uniform float overcastFactor;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;
uniform vec3 camPos;

uniform mat4 objectMatrix;
uniform mat3 objectMatrixNormal;

#define stormyness clamp(overcastFactor * 2.0 - 1.0, 0.0, 1.0)

void main(){
	//Usual variable passing
	texCoordPassed = texCoordIn;
	
	vec4 vertex = objectMatrix * vec4(vertexIn.xyz, 1.0);
	
	float movingness = normalIn.w;
	if(movingness > 0)
	{
		vertex.x += sin(animationTimer * (0.25 + stormyness * 0.35) + vertex.z + vertex.y / 2.0) * ( 0.05 + stormyness * 0.1);
		vertex.z += cos(animationTimer * (0.35 + stormyness * 0.25) + vertex.x*1.5 + 0.3) * ( 0.05 + stormyness * 0.1);
	}
	
	vertexPassed = vertex;
	normalPassed =  (normalIn.xyz-0.5)*2.0;
	
	fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertex.xyz - camPos), vec3(normalPassed)), 0.0, 1.0);
	
	texCoordPassed /= 32768.0;
	
	//Compute lightmap coords
	rainWetness = wetness*clamp(float(colorAndMaterialIn.g) - 14.5,0.0,1.0);
	worldLight = clamp(vec2(colorAndMaterialIn.r, colorAndMaterialIn.g) / 15.0 * vec2(1.0, 1.0 - float(colorAndMaterialIn.b) * 0.125), 0.0, 1.0);
	
	gl_Position = modelViewProjectionMatrix * vertex;
	
	uint materialFlags = colorAndMaterialIn.w;
	//if(materialFlags != 0u)
	//	gl_Position = vec4(0.0);
	
	//eyeDirection transform
	materialFlagsPassed = materialFlags;
	
	
	eyeDirection = vertex.xyz - camPos;
}
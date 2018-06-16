#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D diffuseTexture; // Blocks texture atlas

//Passed variables
in vec3 normalPassed;
in vec4 vertexPassed;
in vec2 texCoordPassed; // Coordinate
in vec3 eyeDirection; // eyeDirection-position
in vec2 lightMapCoords; //Computed in vertex shader
in float fresnelTerm;

out vec4 outDiffuseColor;
out vec3 outNormal;
out vec2 outVoxelLight;
out float outRoughness;
out float outMetalness;
out uint outMaterial;

//Block and sun Lightning
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap
uniform vec3 sunPos; // Sun position

//Normal mapping
uniform sampler2D waterNormalShallow;
uniform sampler2D waterNormalDeep;

uniform float shadowVisiblity; // Used for night transitions ( w/o shadows as you know )

//Water
uniform float animationTimer;
// Screen space reflections
uniform vec2 screenSize;

//Fog
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform sampler2D readbackShadedBufferTemp;

uniform float underwater;

//Gamma constants
#include ../lib/gamma.glsl
#include ../lib/transformations.glsl

#include ../lib/normalmapping.glsl

vec3 mixedTextures(vec2 coords)
{
	return texture(waterNormalShallow, coords).rgb * 2.0 - vec3(1.0);
}

#include ../water/surface.glsl

void main(){
	vec3 normal = vec3(0.0, 1.0, 0.0);
	
	vec3 normalMap = normalize(water());
	
	//float i = 1.0;
	
	//normal.x += nt.r*i;
	//normal.y += nt.g*i;
	//normal.z += nt.b*i;
	
	normal = perturb_normal(normalPassed, eyeDirection, texCoordPassed, normalMap);
	//normal = normalPassed;
	normal = normalize(normalMatrix * normal);
	
	
	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	
	float spec = fresnelTerm;
	vec4 worldspaceFragment = convertScreenSpaceToCameraSpace(coords, gl_FragCoord.z);
	
	//#ifdef perPixelFresnel
	float dynamicFresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(worldspaceFragment.xyz), normal), 0.0, 1.0);
	spec = dynamicFresnelTerm;
	//#endif
	
	//vec4 baseColor = texture(readbackShadedBufferTemp, gl_FragCoord.xy / screenSize);
	vec4 baseColor = vec4(texture(diffuseTexture, texCoordPassed));
	baseColor.rgb = pow(vec3(51 / 255.0, 105 / 255.0, 110 / 255.0), vec3(gamma));
	baseColor.rgb *= 1.0;
	baseColor.rgb = vec3(1.0);
	
	spec *= 1-underwater;
	spec = pow(spec, gamma);
	
	//if(mod((gl_FragCoord.x + gl_FragCoord.y), 2) == 0)
	//	discard;
	
	if(baseColor.a < 1.0)
		discard;
	
	
	outDiffuseColor = baseColor;
	outNormal = encodeNormal(normal);
	outVoxelLight = lightMapCoords.xy;
	outVoxelLight = vec2(0.0, 1.0);
	outRoughness = 0.03;
	outMetalness = 1.0;
	outMaterial = 0u;
}

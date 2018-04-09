#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D diffuseTexture; // Blocks texture atlas
uniform sampler2D normalTexture;

//Passed variables
in vec4 vertexPassed;
in vec3 normalPassed;
in vec2 texCoordPassed;
in vec3 eyeDirection;
in float waterFogI;

out vec4 fragColor;

//Block and sun Lightning
uniform sampler2D lightColors; // Sampler to lightmap

//Water
uniform float animationTimer;
// Screen space reflections
uniform vec2 screenSize;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 camPos;

const vec3 shadowColor = vec3(0.20, 0.20, 0.31);

uniform sampler2D readbackAlbedoBufferTemp;
uniform sampler2D readbackVoxelLightBufferTemp;
uniform sampler2D readbackDepthBufferTemp;

uniform vec2 shadedBufferDimensions;
//uniform float viewDistance;

uniform float underwater;

//Gamma constants
#include ../lib/gamma.glsl
#include ../lib/transformations.glsl

const vec4 waterColor = vec4(0.2, 0.4, 0.45, 1.0);

void main(){

	//Basic texture color
	vec2 coords = (gl_FragCoord.xy)/screenSize;
	
	vec4 baseColor = texture(diffuseTexture, texCoordPassed);
	
	vec4 posunderwater = convertScreenSpaceToCameraSpace(coords, readbackDepthBufferTemp);
	vec4 possurface = convertScreenSpaceToCameraSpace(vec3((gl_FragCoord.xy)/screenSize, gl_FragCoord.z));
	
	//Pass 1
	vec2 worldLight = texture(readbackVoxelLightBufferTemp, coords).xy;
	
	vec3 blockLight = textureGammaIn(lightColors,vec2(worldLight.x, 0)).rgb;
	vec3 sunLight = textureGammaIn(lightColors,vec2(0, worldLight.y)).rgb;
	
	vec3 finalLight = blockLight;
	finalLight += sunLight;

	//coords += 15.0 * (1 - length(worldspaceFragment) / viewDistance) * vec2( normal.xz ) / screenSize;
	vec4 refracted = texture(readbackAlbedoBufferTemp, coords);
	
	float waterFogI2 = length(posunderwater.xyz) / 128.0;
	refracted.rgb *= pow(finalLight, vec3(gammaInv));
	
	//baseColor.rgba = vec4(1.0);
	
	baseColor = mix(refracted, baseColor, clamp(waterFogI2*(1.0-underwater), 0.0, 1.0));
	
	//baseColor = mix(baseColor, vec4(waterColor.rgb/* * getSkyColor(dayTime, vec3(0.0, -1.0, 0.0))*/, 1.0), underwater * clamp(length(convertScreenSpaceToCameraSpace((gl_FragCoord.xyz)/vec3(screenSize,1.0))) / 32.0, 0.0, 1.0));
	
	if(baseColor.a == 0.0) {
		discard;
	}
	
	fragColor = vec4(baseColor);
}

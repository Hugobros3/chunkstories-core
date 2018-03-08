#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D depthBuffer;
uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;

uniform sampler2D previousBuffer;
uniform int keepPreviousData;

//Reflections stuff

//Passed variables
in vec2 screenCoord;
in vec3 eyeDirection;

//Sky data
uniform vec3 sunPos;
uniform float overcastFactor;
uniform float dayTime;

uniform sampler2D lightColors;
uniform sampler2D blockLightmap;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;
uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
uniform mat4 untranslatedMV;
uniform mat4 untranslatedMVInv;
uniform vec3 camPos;

//Shadow mapping
uniform float shadowVisiblity; // Used for night transitions, hides shadows
uniform sampler2DShadow shadowMap;
uniform mat4 shadowMatrix;

uniform float time;
uniform float animationTimer;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Gamma constants
<include ../lib/gamma.glsl>

uniform vec3 shadowColor;
uniform vec3 sunColor;

out vec4 fragColor;

<include ../sky/sky.glsl>
//<include ../sky/fog.glsl>
<include ../lib/transformations.glsl>
//<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
<include gi.glsl>
//<include ../lib/ssr.glsl>

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	vec4 worldSpacePosition = modelViewMatrixInv * cameraSpacePosition;
	vec3 normalWorldSpace = normalize(normalMatrixInv * pixelNormal);
	
	vec3 dist = abs(worldSpacePosition.xyz - camPos.xyz);
	if(dist.x >= 48 || dist.y >= 48 || dist.z >= 48) {
		fragColor = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}
		
	
	vec4 gi = giMain(worldSpacePosition, normalWorldSpace, screenCoord);
	gi.a = 1.0 - gi.a;
	
	if(keepPreviousData == 1) {
		vec4 previousGi = texture(previousBuffer, screenCoord);
		float accTime = 0.05;
		
		gi = gi + previousGi;
		//gi = (gi * accTime + previousGi * (1.0 - accTime));
	}
	
	
	fragColor = gi;
}

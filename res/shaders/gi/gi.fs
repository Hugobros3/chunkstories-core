#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D zBuffer;
uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;

uniform sampler2D previousBuffer;
uniform sampler2D previousConfidence;
uniform sampler2D previousZ;

uniform int keepPreviousData;

//Reflections stuff

//Passed variables
in vec2 screenCoord;
in vec3 eyeDirection;

//Sky data
uniform vec3 sunPos;
uniform float overcastFactor;
uniform float dayTime;

//uniform sampler2D lightColors;
//uniform sampler2D blockLightmap;

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

uniform mat4 previousProjectionMatrix;
uniform mat4 previousModelViewMatrix;
uniform mat4 previousProjectionMatrixInv;
uniform mat4 previousModelViewMatrixInv;

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

out float gl_FragDepth;
out vec4 fragColor;
out float outputConfidence;

<include ../sky/sky.glsl>
//<include ../sky/fog.glsl>
<include ../lib/transformations.glsl>
//<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
<include gi.glsl>
//<include ../lib/ssr.glsl>

void main() {
    
	vec4 cameraSpacePosition = projectionMatrixInv * vec4(vec3(screenCoord * 2.0 - vec2(1.0), texture(zBuffer, screenCoord, 0.0).x * 2.0 - 1.0), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
	
	//vec4 previousCSP = previousProjectionMatrixInv * vec4(vec3(screenCoord * 2.0 - vec2(1.0), texture(previousZ, screenCoord, 0.0).x * 2.0 - 1.0), 1.0);
    //previousCSP /= previousCSP.w;
    //vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	vec4 worldSpacePosition = modelViewMatrixInv * cameraSpacePosition;
	//vec4 previousWorldSpacePosition = previousModelViewMatrixInv * previousCSP;
	
	vec3 normalWorldSpace = normalize(normalMatrixInv * pixelNormal);
	
	vec3 dist = abs(worldSpacePosition.xyz - camPos.xyz);
	if(dist.x >= 48 || dist.y >= 48 || dist.z >= 48) {
		fragColor = vec4(0.0, 0.0, 0.0, 0.0);
		outputConfidence = 1.0;
		return;
	}
	
	vec4 gi = giMain(worldSpacePosition, normalWorldSpace, screenCoord);
	gi.a = 1.0 - gi.a;
	
	vec4 oldCameraSpacePosition = previousModelViewMatrix * worldSpacePosition;
	//oldCameraSpacePosition.w = 1.0;
	//oldCameraSpacePosition /= oldCameraSpacePosition.w;
	vec4 reprojectedPosition = previousProjectionMatrix * oldCameraSpacePosition;
    reprojectedPosition /= reprojectedPosition.w;
	
	reprojectedPosition.xyz = reprojectedPosition.xyz * 0.5 + vec3(0.5);
	
	//if(keepPreviousData == 1) {
	vec4 previousGi = texture(previousBuffer, reprojectedPosition.xy);
	float accTime = 0.05;
	
    float linearDepth = linearizeDepth(texture(zBuffer, screenCoord).r) * 3000.0;
    float offsetDepth = linearizeDepth(texture(previousZ, reprojectedPosition.xy).r) * 3000.0;
	
	float confidence = texture(previousConfidence, reprojectedPosition.xy).x;
	
	float depthDiff = (abs(linearDepth - offsetDepth));
	
	float similarity = clamp(1.0 - pow(4 * max(abs(linearDepth - offsetDepth) - 1.0, 0.0), 2.0), 0.0, 1.0);
	similarity *= clamp(1.0 - 0.5 * length(reprojectedPosition.xyz - vec3(screenCoord.xy, texture(zBuffer, screenCoord).r)), 0.0, 1.0);
	
	//similarity = clamp(similarity, 0.0, 0.95 + clamp(confidence / 100, 0.0, 0.025));
	if(reprojectedPosition.x < 0.0 || reprojectedPosition.x > 1.0 || reprojectedPosition.y < 0.0 || reprojectedPosition.y > 1.0) {
		outputConfidence = 1.0;
	} else {
		outputConfidence = 1.0 + confidence * similarity;
		gi = (gi * (1.0) + previousGi * similarity * confidence) / outputConfidence;
	}
	
	fragColor = gi;
	gl_FragDepth = texture(zBuffer, screenCoord).x;
	
	//fragColor = vec4(reprojectedPosition.xy - screenCoord.xy, 0.0, 1.0);
	//outputConfidence = 1u;
	//fragColor = vec4(screenCoord.rg, 1.0, 1.0);
}

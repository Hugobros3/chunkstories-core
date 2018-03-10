#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D zBuffer;

uniform sampler2D shadedBuffer;

uniform sampler2D voxelLightBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D specularityBuffer;

//Reflections stuff
uniform samplerCube environmentCubemap;

//Passed variables
in vec2 screenCoord;

//Sky data
uniform vec3 sunPos;
uniform float overcastFactor;

uniform sampler2D lightColors;
uniform sampler2D blockLightmap;
uniform sampler2D ssaoBuffer;

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

//Sunlight data
uniform vec3 shadowColor;
uniform vec3 sunColor;

uniform float dayTime;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Gamma constants
<include ../lib/gamma.glsl>

<include ../sky/sky.glsl>
<include ../lib/transformations.glsl> 
<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
<include ../lib/ssr.glsl>

out vec4 fragColor;

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	float spec = texture(specularityBuffer, screenCoord).x;
	
	//Discard fragments using alpha
	if(texture(shadedBuffer, screenCoord).a > 0.0 && spec > 0.0)
	{
		fragColor = clamp(computeReflectedPixel(zBuffer, shadedBuffer, environmentCubemap, screenCoord, cameraSpacePosition.xyz, pixelNormal, texture(voxelLightBuffer, screenCoord).y), 0.0, 1000);
	}
	else
		discard;
	
	// Apply fog - unused because this time SSR is ran AFTER deffered shading.
	
	/*vec3 sum = (cameraSpacePosition.xyz);
	float dist = length(sum)-fogStartDistance;
	float fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	float fogIntensity = clamp(fogFactor, 0.0, 1.0);
	
	vec3 fogColor = getSkyColorWOSun(time, normalize(((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz));
	
	fragColor = mix(shadingColor, vec4(fogColor,shadingColor.a), fogIntensity);*/
}

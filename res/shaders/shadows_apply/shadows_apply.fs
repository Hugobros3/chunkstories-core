#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D depthBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D voxelLightBuffer;

//Reflections stuff
uniform samplerCube environmentCubemap;

//Passed variables
in vec2 screenCoord;
in vec3 eyeDirection;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;
uniform float dayTime;

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

uniform float time;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

//Gamma constants
<include ../lib/gamma.glsl>

uniform vec3 shadowColor;
uniform vec3 sunColor;

out vec4 fragColor;

<include ../sky/sky.glsl>
<include ../sky/fog.glsl>
<include ../lib/transformations.glsl>
<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
//<include ../lib/ssr.glsl>

vec4 computeLight(vec4 inputColor2, vec3 normal, vec4 worldSpacePosition, vec2 voxelLight)
{
	vec4 inputColor = vec4(0.0, 0.0, 0.0, 0.0);
	inputColor.rgb = pow(inputColor2.rgb, vec3(gamma));

	float NdotL = clamp(dot(normalize(normal), normalize(normalMatrix * sunPos )), 0.0, 1.0);
	float lDotU = dot(normalize(-sunPos), vec3(0.0, 1.0, 0.0));

	float opacity = 0.0;

	//Declaration here
	vec3 finalLight = vec3(0.0);
	
	//Voxel light input, modified linearly according to time of day
	vec3 voxelSunlight = textureGammaIn(blockLightmap, vec2(0.0, voxelLight.y)).rgb;
	//voxelSunlight *= textureGammaIn(lightColors, vec2(dayTime, 1.0)).rgb;
		
	//vec3 sunAbsorption = getSkyAbsorption(skyColor, zenithDensity(lDotU + multiScatterPhase));

	
	float sunVisibility = clamp(1.0 - overcastFactor * 2.0, 0.0, 1.0);
	float storminess = clamp(-1.0 + overcastFactor * 2.0, 0.0, 1.0);
	
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient(sunPos, upVec) ;//pow(shadowColor, vec3(gamma));
	shadowLight_g *= textureGammaIn(lightColors, vec2(dayTime, 1.0)).rgb;
		
	<ifdef shadows>
	//Shadows sampling
		vec4 coordinatesInShadowmap = accuratizeShadow(shadowMatrix * (untranslatedMVInv * worldSpacePosition));
	
		float clamped = 10 * clamp(NdotL, 0.0, 0.1);
		
		//How much in shadows's brightness the object is
		float shadowIllumination = 0.0;
		
		//How much in shadow influence's zone the object is
		float edgeSmoother = 0.0;
		
		//How much does the pixel is lit by directional light
		float directionalLightning = clamp((NdotL * 1.1 - 0.1), 0.0, 1.0);
		
		if(!(coordinatesInShadowmap.x <= 0.0 || coordinatesInShadowmap.x >= 1.0 || coordinatesInShadowmap.y <= 0.0 || coordinatesInShadowmap.y >= 1.0  || coordinatesInShadowmap.z >= 1.0 || coordinatesInShadowmap.z <= -1.0))
		{
			//Bias to avoid shadow acne
			float bias = clamp(0.0010*pow(1.0 * NdotL, 1.5) - 0.0*0.01075, 0.0010,0.0025 ) * clamp(16.0 * pow(length(coordinatesInShadowmap.xy - vec2(0.5)), 2.0), 1.0, 16.0);
			//Are we inside the shadowmap zone edge ?
			edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
			
			//
			shadowIllumination += clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0) * 1.5 - 0.25), 0.0, 1.0);
		}
		
		float sunlightAmount = ( directionalLightning * ( mix( shadowIllumination, voxelLight.y, 1-edgeSmoother) ) ) * shadowVisiblity;
		
		finalLight += clamp(sunLight_g * sunlightAmount, 0.0, 4096);
		finalLight += clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
		
		//finalLight = mix(sunLight_g, voxelSunlight * shadowLight_g, (1.0 - sunlightAmount));
		
	<endif shadows>
	<ifdef !shadows>
		// Simple lightning for lower end machines
		float flatShading = 0.0;
		vec3 shadingDir = normalize(normalMatrixInv * normal);
		flatShading += 0.35 * clamp(dot(/*vec3(0.0, 0.0, 0.0)*/sunPos, shadingDir), -0.5, 1.0);
		flatShading += 0.25 * clamp(dot(/*vec3(0.0, 0.0, 1.0)*/sunPos, shadingDir), -0.5, 1.0);
		flatShading += 0.5 * clamp(dot(/*vec3(0.0, 1.0, 0.0)*/sunPos, shadingDir), 0.0, 1.0);
		
		flatShading *= clamp(dot(sunPos, vec3(0.0, 1.0, 0.0)), 0.0, 1.0);
		
		finalLight += clamp(sunLight_g * flatShading * voxelSunlight, 0.0, 4096);
		finalLight += clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
	<endif !shadows>
	
	//Adds block light
	finalLight += textureGammaIn(blockLightmap, vec2(voxelLight.x, 0.0)).rgb;
	
	//Multiplies the albedo
	inputColor.rgb *= finalLight;
	
	// Emmissive materials
	//TODO
	//finalLight += inputColor.rgb * 5.0 * meta.z;
	
	return inputColor;
}

void main() {
	
	
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, depthBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 shadingColor = texture(albedoBuffer, screenCoord);
	
	//Discard fragments using alpha
	if(shadingColor.a > 0.0)
		shadingColor = computeLight(shadingColor, pixelNormal, cameraSpacePosition, texture(voxelLightBuffer, screenCoord).xy);
	else
		discard;
	
	// Apply fog
	
	vec4 fogColor = getFogColor(dayTime, ((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz);
	
	fragColor = mix(shadingColor, vec4(fogColor.xyz, 1.0), fogColor.a);
	fragColor.w = 1.0;
}

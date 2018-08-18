#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec4 vertexPassed;
in vec3 normalPassed;
in vec2 textureCoord;
in vec3 eyeDirection;
//in float fogIntensity;

//flat in ivec4 indexPassed;
in vec4 colorPassed;
flat in uint voxelId;

//Framebuffer outputs
out vec4 shadedFramebufferOut;
//out float metalnessOut;
//out float roughnessOut;

//Textures
uniform sampler2D waterNormalShallow; // Water surface
uniform sampler2D waterNormalDeep; // Water surface

uniform usampler2DArray heights; // Heightmap
uniform usampler2DArray topVoxels; // Block ids
uniform int arrayIndex;

uniform sampler1D blocksTexturesSummary; // Atlas ids -> diffuse rgb
uniform sampler2D vegetationColorTexture; //Vegetation

//Reflections
uniform samplerCube environmentCubemap;

//Block lightning
uniform sampler2D lightColors;
uniform sampler2D blockLightmap;
uniform vec3 shadowColor;
uniform vec3 sunColor;
uniform float shadowStrength;
uniform float shadowVisiblity;

//World general information
uniform float mapSize;
uniform float time;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 camPos;
uniform vec3 camUp;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;

//Gamma constants
#include ../lib/gamma.glsl

//World mesh culling
uniform float ignoreWorldCulling;

uniform float fogStartDistance;
uniform float fogEndDistance;

uniform float animationTimer;

#include ../sky/sky.glsl
#include ../sky/fog.glsl
#include ../lib/normalmapping.glsl

vec3 mixedTextures(vec2 coords)
{
	return texture(waterNormalShallow, coords).rgb * 2.0 - vec3(1.0);
	//return mix(texture(normalTextureShallow, coords).rgb, texture(normalTextureDeep, coords * 0.125).rgb, 0) * 2.0 - vec3(1.0);
}

/*uint access(usampler2DArray tex, vec2 coords) {
	if(coords.x <= 1.0) {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords, indexPassed.x)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(0.0, 1.0), indexPassed.y)).r;
		}
	}
	else {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords - vec2(1.0, 0.0), indexPassed.z)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(1.0), indexPassed.w)).r;
		}
	}
	
	return 250u;
}*/

#include ../water/surface.glsl

void main()
{
	//uint voxelId = access(topVoxels, textureCoord);//texture(topVoxels, vec3(textureCoord, indexPassed)).r;
	
	if(voxelId == 250u)
	{
		shadedFramebufferOut = vec4(1.0, 1.0, 0.0, 1.0);
		return;
	}
	
	//512-voxel types summary... not best
	vec4 diffuseColor = texture(blocksTexturesSummary, (float(voxelId))/512.0);
	
	//Apply plants color if alpha is < 1.0
	if(diffuseColor.a < 1.0)
		diffuseColor.rgb *= texture(vegetationColorTexture, vertexPassed.xz / vec2(mapSize)).rgb;
	
	//Apply gamma then
	diffuseColor.rgb = pow(diffuseColor.rgb, vec3(gamma));
	
	float specularity = 0.0;
	vec3 normal = normalPassed;
	
	//Water case
	if(voxelId == 512u)
	{
		diffuseColor.rgb = pow(vec3(51 / 255.0, 105 / 255.0, 110 / 255.0), vec3(gamma));
		
		normal = perturb_normal(normalPassed, eyeDirection, vertexPassed.xz, normalize(water()));
		//normal = normalize(normalMatrix * normal);
		
		//Set wet
		float fresnelTerm = 0.2 + 0.8 * clamp(0.7 + dot(normalize(vertexPassed.xyz - camPos), normal), 0.0, 1.0);
	
		specularity = pow(fresnelTerm, gamma);
	}
	
	//Computes blocky light
	vec3 baseLight = textureGammaIn(blockLightmap, vec2(0.0, 1.0)).rgb;
	baseLight *= textureGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	//Compute side illumination by sun
	float NdotL = clamp(dot(normal, normalize(sunPos)), 0.0, 1.0);
	float sunlightAmount = NdotL * clamp(sunPos.y, 0.0, 1.0);
	
	float sunVisibility = clamp(1.0 - overcastFactor * 2.0, 0.0, 1.0);
	float storminess = clamp(-1.0 + overcastFactor * 2.0, 0.0, 1.0);
	
	//vec3 sunLight_g = pow(sunColor, vec3(gamma));
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();
	
	vec3 finalLight = shadowLight_g;
	finalLight += sunLight_g * sunlightAmount;
	
	finalLight *= (0.1 + 0.2 * sunVisibility + 0.8 * (1.0 - storminess));

	//Merges diffuse and lightning
	vec3 finalColor = diffuseColor.rgb * finalLight;
	
	//Do basic reflections
	vec3 reflectionVector = normalize(reflect(vec3(eyeDirection.x, eyeDirection.y, eyeDirection.z), normal));
	if(specularity > 0.0)
	{	
		//Basic sky colour
		vec3 reflected = getSkyColor(time, normalize(reflect(eyeDirection, normal)));
		
		//Sample cubemap if enabled
		#ifdef doDynamicCubemaps
		reflected = texture(environmentCubemap, vec3(reflectionVector.x, -reflectionVector.y, -reflectionVector.z)).rgb;
		#endif
		
		//Add sunlight reflection
		//float sunSpecularReflection = specularity * 100.0 * pow(clamp(dot(normalize(reflect(normalMatrix * eyeDirection,normalMatrix * normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),750.0);
		//finalColor += vec3(sunSpecularReflection);
		
		//Mix them to obtain final colour
		finalColor = mix(finalColor, reflected, specularity);
	}
	
	//Get per-fragment fog color
	vec4 fogColor = getFogColor(time, vertexPassed.xyz - camPos);
	
	//Mix in fog 
	shadedFramebufferOut = mix(vec4(finalColor, 1.0), vec4(fogColor.xyz, 1.0), fogColor.a * 0.0);
	//metalnessOut = 0.0;
	//roughnessOut = 1.0;
}

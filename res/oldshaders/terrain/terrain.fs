#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec3 vertexPassed;
in vec3 normalPassed;
in vec2 lightMapCoords;
in vec2 textureCoord;
in vec3 eyeDirection;
in float fresnelTerm;
in float fogIntensity;

flat in ivec4 indexPassed;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Textures
uniform sampler2D normalTexture; // Water surface

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

#include ../sky/sky.glsl

uint access(usampler2DArray tex, vec2 coords) {
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
}

void main()
{
	uint voxelId = access(topVoxels, textureCoord);//texture(topVoxels, vec3(textureCoord, indexPassed)).r;
	
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
	
		//Build water texture
		vec3 nt = 1.0*(texture(normalTexture,(vertexPassed.xz/5.0+vec2(0.0,time)/50.0)/15.0).rgb*2.0-1.0);
		nt += 1.0*(texture(normalTexture,(vertexPassed.xz/2.0+vec2(-time,-2.0*time)/150.0)/2.0).rgb*2.0-1.0);
		nt += 0.5*(texture(normalTexture,(vertexPassed.zx*0.8+vec2(400.0, sin(-time/5.0)+time/25.0)/350.0)/10.0).rgb*2.0-1.0);
		nt += 0.25*(texture(normalTexture,(vertexPassed.zx*0.1+vec2(400.0, sin(-time/5.0)-time/25.0)/250.0)/15.0).rgb*2.0-1.0);
		
		nt = normalize(nt);
		
		//Merge it a bit with the usual direction
		float i = 0.5;
		normal.x += nt.r*i;
		normal.z += nt.g*i;
		normal.y += nt.b*i;
		
		normal = normalize(normal);
		
		//Set wet
		specularity = pow(fresnelTerm, gamma);
	}
	
	//Computes blocky light
	vec3 baseLight = textureGammaIn(blockLightmap, vec2(0.0, 1.0)).rgb;
	baseLight *= textureGammaIn(lightColors, vec2(time, 1.0)).rgb;
	
	//Compute side illumination by sun
	float NdotL = clamp(dot(normal, normalize(sunPos)), 0.0, 1.0);
	float sunlightAmount = NdotL * clamp(sunPos.y, 0.0, 1.0);
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();
	
	vec3 finalLight = shadowLight_g;
	finalLight += sunLight_g * sunlightAmount;


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
		float sunSpecularReflection = specularity * 100.0 * pow(clamp(dot(normalize(reflect(normalMatrix * eyeDirection,normalMatrix * normal)),normalize(normalMatrix * sunPos)), 0.0, 1.0),750.0);
		finalColor += vec3(sunSpecularReflection);
		
		//Mix them to obtain final colour
		finalColor = mix(finalColor, reflected , specularity);
	}
	
	//Get per-fragment fog color
	vec3 fogColor = getFogColor(time, eyeDirection);
	
	//Mix in fog
	shadedFramebufferOut = mix(vec4(finalColor, 1.0),vec4(fogColor,1.0), fogIntensity);
	//shadedFramebufferOut.a = 0.5;
}

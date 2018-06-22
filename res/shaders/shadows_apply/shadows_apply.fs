#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D zBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D voxelLightBuffer;
uniform sampler2D roughnessBuffer;
uniform sampler2D metalnessBuffer;
uniform usampler2D materialsBuffer;

uniform sampler2D reflectionsBuffer;

//Passed variables
in vec2 screenCoord;
in vec3 eyeDirection;

//Sky data
uniform vec3 sunPos;
uniform float overcastFactor;
uniform float dayTime;

uniform float accumulatedSamples;

uniform sampler2D giBuffer;
uniform sampler2D giConfidence;

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
uniform vec3 camUp;
uniform vec2 screenViewportSize;

uniform samplerCube irradianceMap;
uniform samplerCube unfiltered;
uniform sampler2D brdfLUT;

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
#include ../lib/gamma.glsl

out vec4 fragColor;

#include ../sky/sky.glsl
#include ../sky/fog.glsl
#include ../lib/transformations.glsl
#include ../lib/shadowTricks.glsl
#include ../lib/normalmapping.glsl
//#include gi.glsl
#include ../lib/pbr.glsl

#define vl_bias 0.01
	
vec4 bilateralTexture(sampler2D sample, vec2 position, vec3 normal, float lod){

    const vec2 offset[4] = vec2[4](
        vec2(1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(-1.0, 0.0),
        vec2(0.0, -1.0)
    );
	
    float totalWeight = 0.0;
    vec4 result = vec4(0.0);

    float linearDepth = linearizeDepth(texture(zBuffer, position).r);
    vec2 offsetMult = 2.0 / vec2(screenViewportSize.x, screenViewportSize.y);

    for (int i = 0; i < 4; i++){
        vec2 coord = (float(i + 1)) * offset[i] * offsetMult + position;

        vec3 offsetNormal = decodeNormal(texture(normalBuffer, coord));// texture(normalBuffer, coord, lod).rgb * 2.0 - 1.0;
        float normalWeight = pow(abs(dot(offsetNormal, normal)), 32);

        float offsetDepth = linearizeDepth(texture(zBuffer, coord).r);
        float depthWeight = 1.0 / (abs(linearDepth - offsetDepth) + 1e-8);

        float weight = (float(i + 1)) * normalWeight * depthWeight;

        result = texture(sample, coord) * weight + result;

        totalWeight += weight;
    }

    result /= totalWeight;
	
	if(totalWeight <= 0.0f)
		return texture(sample, position);
		//return vec4(1.0, 0.0, 1.0, 1.0);

    return max(result, 0.0);
} 

vec3 computeDirectSunlight(vec3 N, vec3 V, vec3 H, vec3 L, vec3 F0, float roughness, float metallic, vec3 radiance, vec3 albedoColor) {
	vec3 lightColor = vec3(0.0);
	
	// cook-torrance brdf
	float NDF = DistributionGGX(N, H, roughness);        
	float G   = GeometrySmith(N, V, L, roughness);      
	vec3 F    = fresnelSchlick(max(dot(H, V), 0.0), F0);       

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - metallic;	  
	
	vec3 numerator    = NDF * G * F;
	float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0);
	vec3 specular     = numerator / max(denominator, 0.001);  
		
	// add to outgoing radiance Lo
	float NdotL = max(dot(N, L), 0.0);
	
	return (kD * albedoColor.xyz / PI + specular) * radiance * NdotL; 
}

vec3 computeAmbientSkylight(vec3 N, vec3 V, vec3 F0, float roughness, float metallic, vec3 irradiance, vec3 prefilteredColor, vec3 albedoColor) {
	
	vec3 F_ibl = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
	vec2 envBRDF  = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
	vec3 specular_ibl = prefilteredColor * (F_ibl * envBRDF.x + envBRDF.y);
	
    vec3 kS_a = fresnelSchlick(max(dot(N, V), 0.0), F0); //F_ibl
    vec3 kD_a = 1.0 - kS_a;
    kD_a *= 1.0 - metallic;	
	
	vec3 diffuse    = irradiance * albedoColor.rgb;
	vec3 ambient    = (kD_a * diffuse + specular_ibl);
	
	return ambient;
}

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	uint materialFlags = texture(materialsBuffer, screenCoord).x;
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	//Place the albedo colour into gamma-correct space
	albedoColor.rgb = pow(albedoColor.rgb, vec3(gamma));
	
	vec4 worldSpacePosition = modelViewMatrixInv * cameraSpacePosition;
	vec3 normalWorldSpace = normalize(normalMatrixInv * pixelNormal);
	vec2 voxelLight = texture(voxelLightBuffer, screenCoord).xy;
	
		vec4 shadowCoord = shadowMatrix * (untranslatedMVInv * cameraSpacePosition);
		float distFactor = getDistordFactor(shadowCoord.xy);
		vec4 coordinatesInShadowmap = accuratizeShadow(shadowCoord);
		
		//Bias to avoid shadow acne
		float bias = distFactor/32768.0 * 64.0;
		//Are we inside the shadowmap zone edge ?
		float edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
		
		float shadowIllumination = clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0)), 0.0, 1.0);
		shadowIllumination *= clamp((dot(normalize(sunPos), vec3(0.0, 1.0, 0.0)) - 0.0) * 100.0, 0.0, 1.0);
		shadowIllumination *= clamp((dot(normalize(sunPos), normalWorldSpace)) * 100.0, 0.0, 1.0);
	
	vec3 N = normalWorldSpace;
	vec3 V = normalize(-eyeDirection);
	
	//The sun is our light in this shader
	vec3 L = normalize(sunPos);
	vec3 H = normalize(V + L);
	
	vec3 R = reflect(-V, N);
	
	vec3 ambientColour = getAtmosphericScatteringAmbient();
	
	float roughness = texture(roughnessBuffer, screenCoord).r;
	float metallic = texture(metalnessBuffer, screenCoord).r;
	
	//Apply water film on wet surfaces
	#define wetInfluence 0.7
	float wet = clamp(overcastFactor * 2.0 - 1.0, 0.0, 1.0);
	float waterRoughtness = 0.03;
	float waterMetallic = 1.0;
	roughness = mix(roughness, waterRoughtness, wet * wetInfluence);
	metallic = mix(metallic, waterMetallic, wet * wetInfluence);
	
	//Clamp those to be sure
	roughness = clamp(roughness, 0.0, 1.0);
	metallic = clamp(metallic, 0.0, 1.0);    
	
	//Accumulation shadedColor
	vec3 shadedColor = vec3(0.0);
	//Fresnel stuff
	vec3 F0 = F0Base;
	F0 = mix(F0, albedoColor.rgb, metallic);
	
	//Direct sunlight contribution
	float sunStrength = clamp(2.0 * (1.0 - overcastFactor * 1.75), 0.0, 1.0);
	shadedColor += sunStrength * shadowIllumination * computeDirectSunlight(N, V, H, L, F0, roughness, metallic, sunLightColor * 10.0 * pi,  albedoColor.rgb);
	      
	#ifdef globalIllumination
	vec4 gi = texture(giBuffer, screenCoord);
	#endif
	
	float ao = pow(voxelLight.y, 5.1) * (0.5 + 0.5 * shadowIllumination); // not physically correct but gives visual feedback on water and fully metallic surfaces
	//ao = pow(voxelLight.y, 5.1);
	
	//Irradiance is the ambient colour with maybe GI added in
	vec3 irradiance = ambientColour;
	#ifdef globalIllumination
	irradiance += gi.rgb * pi * 1.0;
	#endif
	
	//Compute very crude reflections, add in SSR if the surface is smooth enough
	vec3 smoothReflections = texture(reflectionsBuffer, screenCoord).rgb;
	vec3 prefilteredColor = mix(smoothReflections.rgb, ambientColour, clamp(5.0 * (roughness - 0.2), 0.0, 1.0));
	
	//Do the final ambient light stuff
	shadedColor += computeAmbientSkylight(N, V, F0, roughness, metallic, irradiance, prefilteredColor, albedoColor.rgb) * ao;
	
	//Adds block light
	vec3 torchColor = vec3(255.0 / 255.0, 239.0 / 255.0, 43.0 / 140.0);
	float vl_distance = (1.0 - voxelLight.x) + vl_bias;
	float vl_distanceSquared = vl_distance * vl_distance;
	float vl_invSquared = 1.0 / vl_distanceSquared;
	shadedColor += 0.002 * albedoColor.rgb * clamp(vl_invSquared - 1.0 / (1.0 + vl_bias), 0.0, 100.0) * pow(torchColor, vec3(gamma));
	
	fragColor = vec4(shadedColor, 1.0);
}

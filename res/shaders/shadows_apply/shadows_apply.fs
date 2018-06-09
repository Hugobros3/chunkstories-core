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

uniform vec3 shadowColor;
uniform vec3 sunColor;

out vec4 fragColor;

#include ../sky/sky.glsl
#include ../sky/fog.glsl
#include ../lib/transformations.glsl
#include ../lib/shadowTricks.glsl
#include ../lib/normalmapping.glsl
//#include gi.glsl
//#include ../lib/ssr.glsl

#define vl_bias 0.05
	
vec4 bilateralTexture(sampler2D sample, vec2 position, vec3 normal, float lod){

    const vec2 offset[4] = vec2[4](
        vec2(1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(-1.0, 0.0),
        vec2(0.0, -1.0)
    );

	const int NUM_TAPS = 4;
	
	/*vec2 fTaps_Poisson[NUM_TAPS];
	fTaps_Poisson[0]  = vec2(-.326,-.406);
	fTaps_Poisson[1]  = vec2(-.840,-.074);
	fTaps_Poisson[2]  = vec2(-.696, .457);
	fTaps_Poisson[3]  = vec2(-.203, .621);
	fTaps_Poisson[4]  = vec2( .962,-.195);
	fTaps_Poisson[5]  = vec2( .473,-.480);
	fTaps_Poisson[6]  = vec2( .519, .767);
	fTaps_Poisson[7]  = vec2( .185,-.893);
	fTaps_Poisson[8]  = vec2( .507, .064);
	fTaps_Poisson[9]  = vec2( .896, .412);
	fTaps_Poisson[10] = vec2(-.322,-.933);
	fTaps_Poisson[11] = vec2(-.792,-.598);*/
	
    float totalWeight = 0.0;
    vec4 result = vec4(0.0);

    float linearDepth = linearizeDepth(texture(zBuffer, position).r);
    vec2 offsetMult = 2.0 / vec2(screenViewportSize.x, screenViewportSize.y);

    for (int i = 0; i < NUM_TAPS; i++){
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

vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

vec3 fresnelSchlickRoughness(float cosTheta, vec3 F0, float roughness)
{
    return F0 + (max(vec3(1.0 - roughness), F0) - F0) * pow(1.0 - cosTheta, 5.0);
}

float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a      = roughness*roughness;
    float a2     = a*a;
    float NdotH  = max(dot(N, H), 0.0);
    float NdotH2 = NdotH*NdotH;
	
    float num   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
	
    return num / denom;
}

float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r*r) / 8.0;

    float num   = NdotV;
    float denom = NdotV * (1.0 - k) + k;
	
    return num / denom;
}
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2  = GeometrySchlickGGX(NdotV, roughness);
    float ggx1  = GeometrySchlickGGX(NdotL, roughness);
	
    return ggx1 * ggx2;
}

float GeometrySchlickGGXIBL(float NdotV, float roughness)
{
    float a = roughness;
    float k = (a * a) / 2.0;

    float nom   = NdotV;
    float denom = NdotV * (1.0 - k) + k;

    return nom / denom;
}
// ----------------------------------------------------------------------------
float GeometrySmithIBL(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGXIBL(NdotV, roughness);
    float ggx1 = GeometrySchlickGGXIBL(NdotL, roughness);

    return ggx1 * ggx2;
}  

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	uint materialFlags = texture(materialsBuffer, screenCoord).x;
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	albedoColor.rgb = pow(albedoColor.rgb, vec3(gamma));
	
	vec3 sunLight_g = sunLightColor * pi;
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();
	
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
		shadowIllumination*= clamp((dot(normalize(sunPos), vec3(0.0, 1.0, 0.0)) - 0.5) * 10000.0, 0.0, 1.0);
	
	vec3 N = normalWorldSpace;
	vec3 V = normalize(-eyeDirection);
	
	vec3 L = normalize(sunPos);
	vec3 H = normalize(V + L);
	
	float roughness = texture(roughnessBuffer, screenCoord).r;
	float metallic = texture(metalnessBuffer, screenCoord).r;
	
	float wet = overcastFactor*0;
	vec3 watercolor = pow(vec3(51 / 255.0, 105 / 255.0, 110 / 255.0), vec3(gamma));
	watercolor.rgb *= 4.0;
	float waterRoughtness = 0.03;
	float waterMetallic = 1.0;
	
	roughness = mix(roughness, waterRoughtness, wet * 0.25);
	metallic = mix(metallic, waterMetallic, wet);
	//albedoColor.rgb = mix(albedoColor.rgb, watercolor, wet);
	
	roughness = clamp(roughness, 0.0, 1.0);
	metallic = clamp(metallic, 0.0, 1.0);
	
	//sunLight_g = vec3(1.0, 0.0, 0.0);
	
	float distance    = 0.0;//length(lightPositions[i] - WorldPos);
    float attenuation = 1.0;//1.0 / (distance * distance);
    vec3 radiance     = vec3(sunLight_g) * attenuation;
	
	vec3 lightColor = vec3(0.0);
	
	vec3 F0 = vec3(0.04);
	F0      = mix(F0, albedoColor.xyz, metallic);
	
	// cook-torrance brdf
	float NDF = DistributionGGX(N, H, roughness);        
	float G   = GeometrySmith(N, V, L, roughness);      
	vec3 F    = fresnelSchlick(max(dot(H, V), 0.0), F0);       

	vec3 kS = F;
	vec3 kD = vec3(1.0) - kS;
	kD *= 1.0 - vec3(metallic);	  
	
	vec3 numerator    = NDF * G * F;
	float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0);
	vec3 specular     = numerator / max(denominator, 0.001);  
		
	// add to outgoing radiance Lo
	float NdotL = max(dot(N, L), 0.0);        
	
	vec3 shadedColor = vec3(0.0);
	
	shadedColor += shadowIllumination * (kD * albedoColor.xyz / PI + specular) * radiance * NdotL; 
	      
	vec3 R = reflect(-V, N);
	
    /*const float MAX_REFLECTION_LOD = 4.0;
	vec3 prefilteredColor = textureLod(prefilterMap, R,  roughness * MAX_REFLECTION_LOD).rgb;*/
	//vec3 prefilteredColor = mix(pow(texture(unfiltered, R).rgb, vec3(2.1)), pow(texture(irradianceMap, R).rgb, vec3(2.1)), roughness).rgb;
	//vec3 prefilteredColor = mix(getSkyColorNoSun(dayTime, R).rgb, shadowLight_g.rgb, roughness).rgb;
	vec3 prefilteredColor = shadowLight_g.rgb;
	
	/*vec3 prefilteredColor = vec3(0.01, 0.05, 0.1);
	float smoothness = 1.0 - roughness;
	prefilteredColor += pow(max(dot(R, sunPos), 0.0), 1.0+(10 * smoothness) * (10 * smoothness) * (10 * smoothness)) * vec3(1.0);*/
	
	vec3 F_ibl = fresnelSchlickRoughness(max(dot(N, V), 0.0), F0, roughness);
	vec2 envBRDF  = texture(brdfLUT, vec2(max(dot(N, V), 0.0), roughness)).rg;
	vec3 specular_ibl = prefilteredColor * (F_ibl * envBRDF.x + envBRDF.y);
	
	/*float sl_distance = (1.0 - voxelLight.y) + vl_bias;
	float sl_distanceSquared = sl_distance * sl_distance;
	float sl_invSquared = 1.0 / sl_distanceSquared;
	
	//vec4 gi = texture(giBuffer, screenCoord);
	vec4 gi = bilateralTexture(giBuffer, screenCoord, pixelNormal, 0.0) / 1.0;
	float ambientOcclusion = clamp(1.0 - 0.16 - gi.a * 1.0, 0.0, 1.0);
	
	vec3 voxelSunlight = clamp(sl_invSquared - 1.0 / (1.0 + vl_bias), 0.0, 100.0) * vec3(1.0);*/
	
    vec3 kS_a = fresnelSchlick(max(dot(N, V), 0.0), F0);
    vec3 kD_a = 1.0 - kS_a;
    kD_a *= 1.0 - metallic;	 
	
	float ao = pow(voxelLight.y, 5.1) * (0.1 + 0.9 * shadowIllumination);
	//ao = pow(voxelLight.y, 5.1);
	//vec3 irradiance = texture(irradianceMap, N).rgb;
	vec3 irradiance = shadowLight_g.rgb;
	vec3 diffuse    = irradiance * albedoColor.rgb;
	vec3 ambient    = (kD_a * diffuse + specular_ibl * 1.0) * ao; 
	
	shadedColor += 1.0 * ambient;
	//shadedColor += 0.05 * albedoColor.xyz * shadowLight_g;
	
	//lightColor.rgb += 0.25 * gi.rgb / pi;
	
	fragColor = vec4(shadedColor, 1.0);
	//fragColor = vec4(vec3(pow(voxelLight.y, 5.1)), 1.0);
	//fragColor.rgb = albedoColor.rgb;
	
	//fragColor = texture(brdfLUT, screenCoord);
	//fragColor.rgb = vec3(roughness);
}

void main2() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	uint materialFlags = texture(materialsBuffer, screenCoord).x;
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	vec4 worldSpacePosition = modelViewMatrixInv * cameraSpacePosition;
	vec3 normalWorldSpace = normalize(normalMatrixInv * pixelNormal);
	vec2 voxelLight = texture(voxelLightBuffer, screenCoord).xy;
	
	vec3 lightColor = vec3(0.0);
	
	float NdotL = dot(normalize(normalWorldSpace), normalize(sunPos));
	
	
	//Voxel light input, modified linearly according to time of day
	
	float sl_distance = (1.0 - voxelLight.y) + vl_bias;
	float sl_distanceSquared = sl_distance * sl_distance;
	float sl_invSquared = 1.0 / sl_distanceSquared;
	#ifdef shadows
	//vec3 voxelSunlight = textureGammaIn(blockLightmap, vec2(0.0, voxelLight.y)).rgb;
	vec3 voxelSunlight = 0.02 * clamp(sl_invSquared - 1.0 / (1.0 + vl_bias), 0.0, 100.0) * vec3(1.0);// * pow(torchColor, vec3(gamma));
	#else
	vec3 voxelSunlight = 0.005 * clamp(sl_invSquared - 1.0 / (1.0 + vl_bias), 0.0, 100.0) * vec3(1.0);// * pow(torchColor, vec3(gamma));
	#endif
	
	//float sunVisibility = clamp(1.0 - overcastFactor * 2.0, 0.0, 1.0);
	//float storminess = clamp(-1.0 + overcastFactor * 2.0, 0.0, 1.0);
	
	vec4 gi = vec4(0.0, 0.0, 0.0, 0.0);
	float ambientOcclusion = 1.0;
	
	#ifdef globalIllumination
	float confidence = texture(giConfidence, screenCoord).x;
	//gi = texture(giBuffer, screenCoord) / 1.0;
	gi = bilateralTexture(giBuffer, screenCoord, pixelNormal, 0.0) / 1.0;
	ambientOcclusion = clamp(1.0 - 0.16 - gi.a * 1.0, 0.0, 1.0);
	
	lightColor.rgb += gi.rgb;
	#endif
	
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();//pow(shadowColor, vec3(gamma));
		
	#ifdef shadows
	//Shadows sampling
		vec4 shadowCoord = shadowMatrix * (untranslatedMVInv * cameraSpacePosition);
		float distFactor = getDistordFactor(shadowCoord.xy);
		vec4 coordinatesInShadowmap = accuratizeShadow(shadowCoord);
		
		//How much in shadows's brightness the object is
		float shadowIllumination = 0.0;
		
		//How much in shadow influence's zone the object is
		float edgeSmoother = 0.0;
		
		//How much does the pixel is lit by directional light
		float directionalLightning = NdotL * 1.0;
		
		//Bias to avoid shadow acne
		float bias = distFactor/32768.0 * 64.0;
		//Are we inside the shadowmap zone edge ?
		edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
		
		shadowIllumination += clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0)), 0.0, 1.0);
	
		float sunlightAmount = shadowIllumination * ( mix( shadowIllumination, voxelLight.y, 1-edgeSmoother) ) * clamp(sunPos.y, 0.0, 1.0);
		
		//sunlightAmount *= directionalLightning;
		sunlightAmount *= clamp(mix(directionalLightning, abs(directionalLightning), float(materialFlags & 1u) * 1.0), 0.0, 1.0);
		
		lightColor += clamp(sunLight_g * sunlightAmount, 0.0, 4096);
		lightColor += clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
	#else
		// Simple lightning for lower end machines
		float flatShading = 0.0;
		flatShading += 0.35 * clamp(dot(sunPos, normalWorldSpace), -0.5, 1.0);
		flatShading += 0.25 * clamp(dot(sunPos, normalWorldSpace), -0.5, 1.0);
		flatShading += 0.5 * clamp(dot(sunPos, normalWorldSpace), 0.0, 1.0);
		
		lightColor += clamp(sunLight_g * flatShading * voxelSunlight, 0.0, 4096);
		lightColor += clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
	#endif
	
	//Adds block light
	vec3 torchColor = vec3(255.0 / 255.0, 239.0 / 255.0, 43.0 / 140.0);
	
	//lightColor += vec3(0.25 * pow(voxelLight.x, 4.0));//
	//lightColor += 0.25 * textureGammaIn(blockLightmap, vec2(voxelLight.x, 0.0)).rgb;
	//lightColor += 0.25 * voxelLight.x * voxelLight.x * voxelLight.x * voxelLight.x * pow(torchColor, vec3(gamma));
	//lightColor += 0.25 * textureGammaIn(blockLightmap, vec2(voxelLight.x * voxelLight.x, 0.0)).rgb;
	
	float vl_distance = (1.0 - voxelLight.x) + vl_bias;
	float vl_distanceSquared = vl_distance * vl_distance;
	float vl_invSquared = 1.0 / vl_distanceSquared;
	lightColor += 0.005 * clamp(vl_invSquared - 1.0 / (1.0 + vl_bias), 0.0, 100.0) * pow(torchColor, vec3(gamma));
	
	//gamma-correct the albedo color
	albedoColor.rgb = pow(albedoColor.rgb, vec3(gamma));

	//albedoColor.rgb = vec3(1.0);
	
	//Multiplies the albedo by the light color
	fragColor = vec4(albedoColor.rgb * lightColor.rgb, 1.0);
	
	//if(materialFlags == 1u)
	//	fragColor = vec4(0.0, 1.0, 0.0, 1.0);
	
	//fragColor = vec4(gi.rgb, 1.0);
	//fragColor = vec4(vec3(pow(clamp(1.0 - gi.a * 1.0, 0.0, 1.0), 2.1)), 1.0);
	//fragColor = vec4(vec3(clamp(1.0 - 0.16 - gi.a * 1.0, 0.0, 1.0)), 1.0);
}

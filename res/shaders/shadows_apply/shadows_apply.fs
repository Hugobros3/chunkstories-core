#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

uniform sampler2D zBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D voxelLightBuffer;

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
uniform vec2 screenViewportSize;

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
<include ../sky/fog.glsl>
<include ../lib/transformations.glsl>
<include ../lib/shadowTricks.glsl>
<include ../lib/normalmapping.glsl>
//<include gi.glsl>
//<include ../lib/ssr.glsl>

vec4 bilateralTexture(sampler2D sample, vec2 position, vec3 normal, float lod){

    const vec2 offset[4] = vec2[4](
        vec2(1.0, 0.0),
        vec2(0.0, 1.0),
        vec2(-1.0, 0.0),
        vec2(0.0, -1.0)
    );

	const int NUM_TAPS = 12;
	
	vec2 fTaps_Poisson[NUM_TAPS];
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
	fTaps_Poisson[11] = vec2(-.792,-.598);
	
    float totalWeight = 0.0;
    vec4 result = vec4(0.0);

    float linearDepth = linearizeDepth(texture(zBuffer, position).r);
    vec2 offsetMult = 1.0 / vec2(screenViewportSize.x, screenViewportSize.y);

    for (int i = 0; i < NUM_TAPS; i++){

        vec2 coord = (float(i + 1)) * fTaps_Poisson[i] * offsetMult + position;

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

void main() {
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(screenCoord, zBuffer);
	
	vec3 pixelNormal = decodeNormal(texture(normalBuffer, screenCoord));
	vec4 albedoColor = texture(albedoBuffer, screenCoord);
	
	//Discard fragments using alpha
	if(albedoColor.a <= 0.0)
		discard;
	
	vec4 worldSpacePosition = modelViewMatrixInv * cameraSpacePosition;
	vec3 normalWorldSpace = normalize(normalMatrixInv * pixelNormal);
	vec2 voxelLight = texture(voxelLightBuffer, screenCoord).xy;
	
	vec3 lightColor = vec3(0.0);
	
	float NdotL = clamp(dot(normalize(normalWorldSpace), normalize(sunPos)), 0.0, 1.0);
	
	//Voxel light input, modified linearly according to time of day
	vec3 voxelSunlight = textureGammaIn(blockLightmap, vec2(0.0, voxelLight.y)).rgb;
	
	//float sunVisibility = clamp(1.0 - overcastFactor * 2.0, 0.0, 1.0);
	//float storminess = clamp(-1.0 + overcastFactor * 2.0, 0.0, 1.0);
	
	vec4 gi = vec4(0.0, 0.0, 0.0, 0.0);
	
	float confidence = texture(giConfidence, screenCoord).x;
	
	gi = texture(giBuffer, screenCoord) / 1.0;
	//gi.a = 0.0;
	gi = bilateralTexture(giBuffer, screenCoord, pixelNormal, 0.0) / 1.0;
	gi.a = 1.0 - gi.a;
	
	lightColor.rgb += gi.rgb * pi;
	
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();//pow(shadowColor, vec3(gamma));
		
	<ifdef shadows>
	//Shadows sampling
		vec4 shadowCoord = shadowMatrix * (untranslatedMVInv * cameraSpacePosition);
		float distFactor = getDistordFactor(shadowCoord.xy);
		vec4 coordinatesInShadowmap = accuratizeShadow(shadowCoord);
	
		float clamped = 10 * clamp(NdotL, 0.0, 0.1);
		
		//How much in shadows's brightness the object is
		float shadowIllumination = 0.0;
		
		//How much in shadow influence's zone the object is
		float edgeSmoother = 0.0;
		
		//How much does the pixel is lit by directional light
		float directionalLightning = clamp((NdotL * 1.1 - 0.1), 0.0, 1.0);
		
			//Bias to avoid shadow acne
			float bias = distFactor/32768.0 * 64.0;
			//Are we inside the shadowmap zone edge ?
			edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
			
			shadowIllumination += clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0)), 0.0, 1.0);
		
		float sunlightAmount = ( directionalLightning * shadowIllumination * ( mix( shadowIllumination, voxelLight.y, 1-edgeSmoother) )) * clamp(sunPos.y, 0.0, 1.0);
		
		lightColor += clamp(sunLight_g * sunlightAmount, 0.0, 4096);
		lightColor += gi.a * clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
		
	<endif shadows>
	<ifdef !shadows>
		// Simple lightning for lower end machines
		float flatShading = 0.0;
		flatShading += 0.35 * clamp(dot(/*vec3(0.0, 0.0, 0.0)*/sunPos, normalWorldSpace), -0.5, 1.0);
		flatShading += 0.25 * clamp(dot(/*vec3(0.0, 0.0, 1.0)*/sunPos, normalWorldSpace), -0.5, 1.0);
		flatShading += 0.5 * clamp(dot(/*vec3(0.0, 1.0, 0.0)*/sunPos, normalWorldSpace), 0.0, 1.0);
		
		lightColor += clamp(sunLight_g * flatShading * voxelSunlight, 0.0, 4096);
		lightColor += gi.a * clamp(shadowLight_g * voxelSunlight, 0.0, 4096);
	<endif !shadows>
	
	//Adds block light
	lightColor += textureGammaIn(blockLightmap, vec2(voxelLight.x, 0.0)).rgb;
	
	//gamma-correct the albedo color
	albedoColor.rgb = pow(albedoColor.rgb, vec3(gamma));

	//Multiplies the albedo by the light color
	vec4 shadedColor = vec4(albedoColor.rgb * lightColor.rgb, 1.0);
	
	//Apply the fog
	vec4 fogColor = getFogColor(dayTime, ((modelViewMatrixInv * cameraSpacePosition).xyz - camPos).xyz);
	
	fragColor = mix(shadedColor, vec4(fogColor.xyz, 1.0), fogColor.a);
	fragColor.w = 1.0;
	
	//fragColor = vec4(gi.rgb, 1.0);
	//fragColor = vec4(vec3(gi.a), 1.0);
}

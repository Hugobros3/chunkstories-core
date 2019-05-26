#version 450

in vec2 vertexPos;

out vec4 colorOut;

uniform sampler2D colorBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D depthBuffer;

uniform sampler2DShadow shadowBuffers[4];

#include ../gamma.glsl

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.graphics.common.structs.ShadowMappingInfo
uniform ShadowMappingInfo shadowInfo;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer)
{
    vec4 cameraSpacePosition = camera.projectionMatrixInverted * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

void sampleShadowMap(vec4 worldSpacePosition, mat4 cameraMatrix, sampler2DShadow shadowMap, int cascade, float NdL, in out float shadowFactor, in out float outOfBounds)  {
	vec4 coordinatesInShadowmap = cameraMatrix * worldSpacePosition;
	coordinatesInShadowmap.xy *= 0.5;
	coordinatesInShadowmap.xy += vec2(0.5);
	
	if(coordinatesInShadowmap.x > 1.0 || coordinatesInShadowmap.y > 1.0 || coordinatesInShadowmap.x < 0.0 || coordinatesInShadowmap.y < 0.0 || coordinatesInShadowmap.z < 0.0 || coordinatesInShadowmap.z > 1.0) {
		//outOfBounds = 1.0;
		//shadowFactor = 1.0;
		return;
	} else {
		float bias = pow(2.0, 4 - cascade) * 0.0004 * (1.0 - NdL);
		shadowFactor = clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xyz + vec3(0.0, 0.0, -bias)), 0.0)), 0.0, 1.0);
		outOfBounds = 0.0;
	}
}

void sampleShadowMaps(vec4 worldSpacePosition, float NdL, in out float shadowFactor, in out float outOfBounds) {
	#define sampleLvl(i) sampleShadowMap(worldSpacePosition, shadowInfo.cameras[i].viewMatrix, shadowBuffers[i], i, NdL, shadowFactor, outOfBounds);

	// Unrolled for GLSL 330 compatibility
	if(shadowInfo.cascadesCount >= 1) {
		sampleLvl(0)
	}
	if(shadowInfo.cascadesCount >= 2) {
		sampleLvl(1)
	}
	if(shadowInfo.cascadesCount >= 3) {
		sampleLvl(2)
	}
	if(shadowInfo.cascadesCount >= 4) {
		sampleLvl(3)
	}
}

float bayer2(vec2 a){
    a = floor(a);
    return fract(dot(a,vec2(.5, a.y*.75)));
}

float bayer4(vec2 a)   {return bayer2( .5*a)   * .25     + bayer2(a); }
float bayer8(vec2 a)   {return bayer4( .5*a)   * .25     + bayer2(a); }
float bayer16(vec2 a)  {return bayer4( .25*a)  * .0625   + bayer4(a); }

float computeVolumetricLight(vec3 worldSpacePosition, vec3 lightVec, vec3 eyeDirection){
	const int steps = 16;
	const float oneOverSteps = 1.0 / float(steps);

	vec3 startRay = camera.position;//mat3(shadowMatrix) * untranslatedMVInv[3].xyz;
	vec3 endRay = camera.position + eyeDirection * 64.0;//(shadowMatrix * (untranslatedMVInv * worldSpacePosition)).rgb;

	//float oneOverTotalDistance = 1.0 / length(endRay - startRay);

	vec3 increment = (startRay - endRay) * oneOverSteps;
	vec3 rayPosition = increment * bayer16(gl_FragCoord.xy) + endRay;

	float weight = clamp(sqrt(dot(increment, increment)), 0.0, 0.25);

	float ray = 0.0;
	float stop = 1.0;

	for (int i = 0; i < steps; i++){
		//vec3 shadowCoord = accuratizeShadow(vec4(rayPosition, 0.0)).rgb + vec3(0.0, 0.0, 0.0001);
		//ray += texture(shadowMap, shadowCoord, 0.0);
		float shadowFactor = 1.0;
		float dontCare = 0.0;
		sampleShadowMaps(vec4(rayPosition, 1.0), 1.0, shadowFactor, dontCare);
		ray += shadowFactor * clamp(dot(worldSpacePosition - rayPosition, eyeDirection), 0.0, 1.0);

		rayPosition += increment;
	}
	
	//float lDotU = dot(normalize(-lightVec), vec3(0.0, 1.0, 0.0));
	float lDotV = dot(normalize(lightVec), normalize(eyeDirection));
	
	//vec3 sunLight_g = sunLightColor;
	//float sunlightAmount = (ray * clamp(sunPos.y, 0.0, 1.0)) * (oneOverSteps * weight) * (gPhase(lDotV, 0.9) * mCoeff);

	//return sunlightAmount * sunLight_g * pi;
	return ray * (oneOverSteps * weight) * clamp(world.sunPosition.y * 100.0, 0.0, 1.0) * (clamp(lDotV, 0.0, 0.5) + 0.5);
}

void main()
{
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
	vec4 albedo = pow(texture(colorBuffer, texCoord), vec4(2.1));
	//vec3 normal = camera.normalMatrixInverted * (texture(normalBuffer, texCoord).xyz * 2.0 - vec3(1.0));
	vec3 decodedNormal = decodeNormal(texture(normalBuffer, texCoord).rg);
	vec3 normal = camera.normalMatrixInverted * decodedNormal;

	if(albedo.a < 1.0) {
		discard;
	}

	//float torchLight = texture(colorBuffer, texCoord).w;
	//torchLight = pow(torchLight, 2.1);
	float torchLight = texture(normalBuffer, texCoord).w;
	torchLight = pow(torchLight, 2.1);

	float ambientLight = texture(normalBuffer, texCoord).z;
	ambientLight = pow(ambientLight, 2.1);

	vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(texCoord, depthBuffer);
	vec4 worldSpacePosition = camera.viewMatrixInverted * cameraSpacePosition;

	//vec2 color = vec2(ambientLight, torchLight);
	float NdL = clamp(dot(world.sunPosition, normal.xyz), 0.0, 1.0);
	
	//vec3 shadowLight = pow(vec3(52.0 / 255.0, 68.0 / 255.0, 84.0 / 255.0), vec3(gamma));
	//vec3 sunLight = vec3(1.0) - shadowLight;

	float shadowFactor = 1.0;
	float outOfBounds = 1.0;

	sampleShadowMaps(worldSpacePosition, NdL, shadowFactor, outOfBounds);
	
	/*for(int cascade = 0; cascade < shadowInfo.cascadesCount; cascade++) {
		vec4 coordinatesInShadowmap = shadowInfo.cameras[cascade].viewMatrix * worldSpacePosition;
		coordinatesInShadowmap.xy *= 0.5;
		coordinatesInShadowmap.xy += vec2(0.5);
		
		if(coordinatesInShadowmap.x > 1.0 || coordinatesInShadowmap.y > 1.0 || coordinatesInShadowmap.x < 0.0 || coordinatesInShadowmap.y < 0.0 || coordinatesInShadowmap.z < 0.0 || coordinatesInShadowmap.z > 1.0) {
			//outOfBounds = 1.0;
			//shadowFactor = 1.0;
			break;
		} else {
			float bias = pow(2.0, 4 - cascade) * 0.0004 * (1.0 - NdL);
			shadowFactor = clamp((texture(shadowBuffers[cascade], vec3(coordinatesInShadowmap.xyz + vec3(0.0, 0.0, -bias)), 0.0)), 0.0, 1.0);
			outOfBounds = 0.0;
		}
	}*/

	shadowFactor = mix(shadowFactor, 1.0, outOfBounds);

	vec3 sunLightColor = sunAbsorb;
	vec3 shadowLightColor = getAtmosphericScatteringAmbient() / pi;

	vec3 lightColor = (sunLightColor * NdL * shadowFactor + shadowLightColor * ambientLight);
	lightColor += vec3(1.0) * pow(torchLight, 2.0);

	//vec3 cLightPosition = camera.position;
	//vec3 cLightPosition = vec3(1480, 67, 382);
	/*vec3 cLightPosition = vec3(1324, 35, 524);
	vec3 cLightColor = vec3(0.00, 0.384, 0.7) * 20000.0;
	float cDist = length(cLightPosition - worldSpacePosition.xyz);
	vec3 cLightDir = normalize(cLightPosition - worldSpacePosition.xyz);
	float cNdL = clamp(dot(cLightDir, normal), 0.0, 1.0);
	lightColor += cLightColor * cNdL / (cDist * cDist);*/

	vec3 litSurface = albedo.rgb * lightColor;

	// light overflow thingie
	litSurface += vec3(1.0) * length(litSurface) * 0.05;

	// apply fog
	vec4 fogColor = getFogColor(world.time, (worldSpacePosition.xyz - camera.position.xyz).xyz);
	vec3 foggedSurface = mix(litSurface, fogColor.xyz, fogColor.a);

	// apply light haze
	vec3 eyeDirection = normalize((worldSpacePosition.xyz - camera.position.xyz).xyz);
	foggedSurface += vec3(1.0, 0.6, 0.1) * computeVolumetricLight(worldSpacePosition.xyz, world.sunPosition, eyeDirection.xyz);

	// output
	colorOut = vec4(foggedSurface, 1.0);
}
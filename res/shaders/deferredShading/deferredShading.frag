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

#include struct xyz.chunkstories.graphics.vulkan.util.ShadowMappingInfo
uniform ShadowMappingInfo shadowInfo;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer)
{
    vec4 cameraSpacePosition = camera.projectionMatrixInverted * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

void main()
{
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
	vec4 albedo = pow(texture(colorBuffer, texCoord), vec4(2.1));
	vec3 normal = texture(normalBuffer, texCoord).xyz * 2.0 - vec3(1.0);

	if(albedo.a < 1.0) {
		discard;
	}

	//float torchLight = texture(colorBuffer, texCoord).w;
	//torchLight = pow(torchLight, 2.1);
	float torchLight = 0.0;

	float ambientLight = texture(normalBuffer, texCoord).w;
	ambientLight = pow(ambientLight, 2.1);

	vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(texCoord, depthBuffer);
	vec4 worldSpacePosition = camera.viewMatrixInverted * cameraSpacePosition;

	vec2 color = vec2(ambientLight, torchLight);

	float NdL = clamp(dot(world.sunPosition, normal.xyz), 0.0, 1.0);
	
	//vec3 shadowLight = pow(vec3(52.0 / 255.0, 68.0 / 255.0, 84.0 / 255.0), vec3(gamma));
	//vec3 sunLight = vec3(1.0) - shadowLight;

	float shadowFactor = 1.0;
	float outOfBounds = 1.0;
	for(int cascade = 0; cascade < shadowInfo.cascadesCount; cascade++) {
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
	}

	shadowFactor = mix(shadowFactor, 1.0, outOfBounds);

	vec3 sunLightColor = sunAbsorb;
	vec3 shadowLightColor = getAtmosphericScatteringAmbient() / pi;

	vec3 lightColor = (sunLightColor * NdL * shadowFactor + shadowLightColor * ambientLight);
	//lightColor += vec3(1.0) * pow(color.y, 2.0);

	vec3 litSurface = albedo.rgb * lightColor;
	//litSurface = camera.lookingAt.rgb;
	//litSurface = normal.rgb;
	//vec3 E = camera.lookingAt.xyz;
	//E = normalize(vertex - camera.position);
	//vec3 R = reflect(E, normal);
	//litSurface = clamp(dot(R, world.sunPosition), 0.0, 1.0) * vec3(1.0);
	//litSurface = R;
	//litSurface = getSkyColor(0.5, E);

	//vec3 fog = vec3(0.0, 0.5, 1.0);
	//float fogStrength = clamp(length(worldSpacePosition.xyz - camera.position.xyz) * 0.001, 0.0, 1.0);
	//vec3 foggedSurface = mix(litSurface, fog, fogStrength);

	vec4 fogColor = getFogColor(world.time, (worldSpacePosition.xyz - camera.position.xyz).xyz);
	vec3 foggedSurface = mix(litSurface, fogColor.xyz, fogColor.a);

	if(outOfBounds > 0.5f) {
		lightColor = vec3(1.0, 0.0, 0.0);
	}

	vec3 eyeDirection = normalize(worldSpacePosition.xyz - camera.position.xyz);

	colorOut = vec4(foggedSurface, 1.0);
	//colorOut = vec4(eyeDirection, 1.0);
	//colorOut = vec4(vec3(lightColor), 1.0);
	//colorOut = vec4(vec3(pow(ambientLight, 2.1)), 1.0);
	//colorOut = vec4(coordinatesInShadowmap.xyz, 1.0);
}
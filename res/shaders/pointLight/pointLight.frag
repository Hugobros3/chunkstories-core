#version 450

in vec2 vertexPos;

out vec4 colorOut;

uniform sampler2D colorBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D depthBuffer;

#include ../gamma.glsl

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.representation.PointLight
uniform PointLight light;

#include ../normalcompression.glsl

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

	vec3 decodedNormal = decodeNormal(texture(normalBuffer, texCoord).rg);
	vec3 normal = camera.normalMatrixInverted * decodedNormal;

	if(albedo.a < 1.0) {
		discard;
	}

	vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(texCoord, depthBuffer);
	vec4 worldSpacePosition = camera.viewMatrixInverted * cameraSpacePosition;

	//vec2 color = vec2(ambientLight, torchLight);
	//float NdL = clamp(dot(world.sunPosition, normal.xyz), 0.0, 1.0);

	vec3 lightColor = vec3(0.0);

	//vec3 cLightPosition = camera.position;
	//vec3 cLightPosition = vec3(1480, 67, 382);
	
	//vec3 cLightPosition = vec3(1324, 35, 524);
	//vec3 cLightColor = vec3(0.00, 0.384, 0.7) * 20000.0;
	
	vec3 cLightPosition = light.position;
	vec3 cLightColor = light.color;

	float cDist = length(cLightPosition - worldSpacePosition.xyz);
	vec3 cLightDir = normalize(cLightPosition - worldSpacePosition.xyz);
	float cNdL = clamp(dot(cLightDir, normal), 0.0, 1.0);
	lightColor += cLightColor * cNdL / (cDist * cDist);

	vec3 litSurface = albedo.rgb * lightColor;

	// apply fog
	//vec4 fogColor = getFogColor(world.time, (worldSpacePosition.xyz - camera.position.xyz).xyz);
	//vec3 foggedSurface = mix(litSurface, fogColor.xyz, fogColor.a);

	// apply light haze
	//vec3 eyeDirection = normalize((worldSpacePosition.xyz - camera.position.xyz).xyz);
	//foggedSurface += vec3(1.0, 0.6, 0.1) * computeVolumetricLight(worldSpacePosition.xyz, world.sunPosition, eyeDirection.xyz);

	// output
	//colorOut = vec4(foggedSurface, 1.0);
	colorOut = vec4(litSurface, 0.0);
}
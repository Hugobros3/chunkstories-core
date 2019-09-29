#version 450

//layout(set=0, location=0) uniform sampler2D virtualTextures[1024];

in vec3 vertex;
in vec4 color;
in vec3 normal;
in vec2 texCoord;
flat in int textureId;

uniform sampler2DArray albedoTextures;

out vec4 shadedBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl

uniform sampler2D waterNormalShallow;
uniform sampler2D waterNormalDeep;

/*vec4 convertScreenSpaceToCameraSpace(vec3 screenSpaceCoordinates) {

    vec4 fragposition = camera.projectionMatrixInverted * vec4(screenSpaceCoordinates * 2.0 - vec3(1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition;
}*/

vec3 granular(vec3 vec) {
	return floor(vec * 4.0) / 4.0;
}

void main()
{
	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	//vec4 albedo = vtexture2D(textureId, texCoord);
	vec4 albedo = texture(albedoTextures, vec3(texCoord, textureId));

	vec3 floored = granular(vertex.xyz);

	vec3 normal2 = normalize(normal + 0.0 * vec3(sin(vertex.x), 0.0, cos(vertex.y)));

	vec3 shallow = texture(waterNormalShallow, floored.xz + world.time * 0*0.25 * vec2(10.0, -78.0)).xzy * 2.0 - vec3(1.0);
	vec3 deep = texture(waterNormalDeep, floored.xz * 0.125 * 0.125 + world.time * 0.5 * vec2(-40.0, 3.0)).xzy * 2.0 - vec3(1.0);
	normal2 = normalize(shallow + deep * 0.5 + 5.0 * normal2);

	if(albedo.a == 0.0) {
		discard;
	}
	
	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}

	vec3 eye = granular(vertex.xyz) - camera.position;
	vec3 eyeDirection = normalize(eye);

	float shittyFresnel = clamp(pow(dot(eyeDirection, -normal2), 1.0), 0.0, 1.0);

	float sunSpot = 2.1 * clamp(pow(dot(world.sunPosition, reflect(eyeDirection, normal2.xyz)), 512.0), 0.0, 100.0);
	float NdL = clamp(pow(dot(world.sunPosition, normal2.xyz), 1.0), 0.0, 1.0);

	vec4 waterColor = vec4(0.0, 0.0, 0.0, clamp(0.8 + 0.2 * (1.0 - shittyFresnel), 0.0, 1.0));
	waterColor.rgb += vec3(0.2, 0.2, 0.5) * getSkyColor(world.time, reflect(eyeDirection, normal2.xyz));

	vec4 fogColor = getFogColor(world.time, camera.position - vertex);

	shadedBuffer = vec4(mix(waterColor.rgb, fogColor.rgb, fogColor.a), waterColor.a);
}
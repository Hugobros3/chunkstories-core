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

void main()
{
	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	//vec4 albedo = vtexture2D(textureId, texCoord);
	vec4 albedo = texture(albedoTextures, vec3(texCoord, textureId));

	vec3 normal2 = normalize(normal + 0.0 * vec3(sin(vertex.x), 0.0, cos(vertex.y)));

	vec3 shallow = texture(waterNormalShallow, vertex.xz * 0.125).xzy * 2.0 - vec3(1.0);
	vec3 deep = texture(waterNormalDeep, vertex.xz * 0.125 * 0.125).xzy * 2.0 - vec3(1.0);
	normal2 = normalize(shallow + deep * 0.5 + 5.0 * normal2);

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}

	vec3 eye = vertex.xyz - camera.position;
	vec3 eyeDirection = normalize(eye);

	float shittyFresnel = clamp(pow(dot(eyeDirection, -normal2), 1.0), 0.0, 1.0);

	float sunSpot = 2.1 * clamp(pow(dot(world.sunPosition, reflect(eyeDirection, normal2.xyz)), 512.0), 0.0, 100.0);
	float NdL = clamp(pow(dot(world.sunPosition, normal2.xyz), 1.0), 0.0, 1.0);

	vec4 waterColor = vec4(0.0, 0.0, 0.0, 1.0 * (1.0 - shittyFresnel));
	//waterColor += vec4(1.0) * sunSpot;
	waterColor.rgb += vec3(0.2, 0.2, 0.5) * getSkyColor(world.time, reflect(eyeDirection, normal2.xyz));

	shadedBuffer = waterColor;
	//shadedBuffer = vec4(reflect(eye, normal.xyz), 1.0);
	//shadedBuffer = vec4(eye, 1.0);
}
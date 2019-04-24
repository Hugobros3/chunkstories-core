#version 450

in vec3 vertex;
in vec4 color;
in vec3 normal;
in vec2 texCoord;
flat in int textureId;

uniform sampler2DArray albedoTextures;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

void main()
{
	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	//vec4 albedo = vtexture2D(textureId, texCoord);
	vec4 albedo = texture(albedoTextures, vec3(texCoord, textureId));

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}

	colorBuffer = albedo;
	normalBuffer = vec4(encodeNormal(normal), color.xy);
}
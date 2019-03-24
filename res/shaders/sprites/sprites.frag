#version 450

in vec3 position;
in vec2 texCoord;

in vec3 eyeDirection;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl

#material sampler2D albedoTexture;
#material sampler2D normalTexture;

void main()
{
	vec4 albedo = texture(albedoTexture, texCoord).rgba;

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}

	colorBuffer = albedo;
	normalBuffer = vec4(eyeDirection, 1.0);
}
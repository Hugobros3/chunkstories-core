#version 450

//layout(set=0, location=0) uniform sampler2D virtualTextures[1024];

in vec3 vertex;
in vec4 color;
in vec3 normal;
in vec2 texCoord;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl

void main()
{
	vec4 albedo = color;

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}

	colorBuffer = albedo;
	normalBuffer = vec4(normal * 0.5 + vec3(0.5), 1.0);
}
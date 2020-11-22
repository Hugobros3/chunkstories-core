#version 450

in vec3 vertex;
in vec3 normal;
in vec2 texCoord;

#ifdef ENABLE_ANIMATIONS
flat in ivec4 boneId;
in vec4 boneWeight;
#endif

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

#material sampler2D albedoTexture;
#material sampler2D normalTexture;

void main() {
    vec4 albedo = texture(albedoTexture, texCoord).rgba;

    if(albedo.a == 0.0) {
        discard;
    }

    if(albedo.a < 1.0) {
        albedo.rgb *= vec3(0.2, 1.0, 0.5);
        albedo.a = 1.0;
    }

    colorBuffer = albedo;
    normalBuffer = vec4(encodeNormal(normal), 1.0, 0.0);
}

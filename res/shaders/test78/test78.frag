#version 450
 
in vec2 texCoord;

out vec4 fragColor;

#define PI 3.14159265359
#define pi PI
#include struct xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
uniform PrecomputedSimplexSeed simplexSeed;

#include ../blockMeshes/simplex.glsl
#include ../blockMeshes/noise.glsl

float ridgedNoise2(vec2 pos, int octaves, float freq, float persistence) {
    float frequency = freq;
    float total = 0.0f;
    float maxAmplitude = 0.0f;
    float amplitude = 1.0f;

    //frequency *= (WORLD_SIZE / (64 * 32));
    for(int i = 0; i < octaves; i++) {
        total += (1.0f - abs(looped_noise(pos * frequency, 1.0, 0.0))) * amplitude;
        frequency *= 2.0f;
        maxAmplitude += amplitude;
        amplitude *= persistence;
    }
    return total / maxAmplitude;
}

void main()
{
	fragColor = vec4(0.0, 0.0, 0.0, 0.0);

    /*if(texCoord.x < 512 && texCoord.y < 512) {

        float noise = ridgedNoise2(texCoord.xy / vec2(512.0), 4, 1.0, 0.5);
	    fragColor = vec4(vec3(noise),1.0);
    }*/
}
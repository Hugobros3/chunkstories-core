float looped_noise(vec2 pos, vec4 offset, vec4 multiplier) {
    float nx = cos(mod(pos.x * 2 * pi, 2 * pi));
    float ny = cos(mod(pos.y * 2 * pi, 2 * pi));
    float nz = sin(mod(pos.x * 2 * pi, 2 * pi));
    float nw = sin(mod(pos.y * 2 * pi, 2 * pi));

    //TODO figure out wtf is going on here to require this hack
    //PrecomputedSimplexSeed s33d = PrecomputedSimplexSeed(simplexSeed.perm);

    return noise4d( offset + multiplier * vec4(nx, ny, nz, nw));
}

#define WORLD_SIZE 4096.0

float fractalNoise(vec2 pos, int octaves, float freq, float persistence, vec4 offset) {
    float frequency = freq;
    float acc = 0.0;
    float maxAmplitude = 0.0;
    float amplitude = 1.0;

    //frequency *= (WORLD_SIZE / (64 * 32));
    for(int i = 0; i < octaves; i++) {
        acc += looped_noise(pos / WORLD_SIZE, offset, vec4(frequency * WORLD_SIZE / 4096.0)) * amplitude;
        frequency *= 2.0;
        maxAmplitude += amplitude;
        amplitude *= persistence;
    }

    return acc / maxAmplitude;
}

float ridgedNoise(vec2 pos, int octaves, float freq, float persistence, vec4 offset) {
    float frequency = freq;
    float total = 0.0f;
    float maxAmplitude = 0.0f;
    float amplitude = 1.0f;

    //frequency *= (WORLD_SIZE / (64 * 32));
    for(int i = 0; i < octaves; i++) {
        total += (1.0f - abs(looped_noise(pos / WORLD_SIZE, offset, vec4(frequency * WORLD_SIZE / 4096.0)))) * amplitude;
        frequency *= 2.0f;
        maxAmplitude += amplitude;
        amplitude *= persistence;
    }
    return total / maxAmplitude;
}

float heightAt(vec2 pos) {
	float height = 0.0;

    float maxHeight = fractalNoise(pos, 1, 1.0, 0.5, vec4(-47.0, 154.0, 126.0, 148.0));
    height += (32 + 48 * maxHeight + 48 * maxHeight * maxHeight) * ridgedNoise(pos, 2, 1.0, 0.5, vec4(0.0, 154.0, 121.0, -48.0));

    float roughness = fractalNoise(pos, 1, 1.0, 0.5, vec4(245.0, 87.0, -33.0, -88.0));
    roughness = clamp(roughness * 2.5 - 0.33, 0.25, 1.0);

    height += 32 * roughness * fractalNoise(pos, 4, 8.0, 0.5, vec4(0.0, 154.0, 121.0, -48.0));
    
    height += 32 * clamp(
        ridgedNoise(pos, 2, 1.0, 0.5, vec4(-5752.0, -2200.0, -457.0, 948.0)) * 2.0 - 1.0, 0.0, 1.0);

    if(height < 63.0)
        height = 63.0;

	return height;
}
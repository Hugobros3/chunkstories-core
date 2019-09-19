float looped_noise(vec2 pos, float period, float invPeriod) {
    float s = pos.x / period;
    float t = pos.y / period;

    float nx = cos(mod(s * 2 * pi, 2 * pi));
    float ny = cos(mod(t * 2 * pi, 2 * pi));
    float nz = sin(mod(s * 2 * pi, 2 * pi));
    float nw = sin(mod(t * 2 * pi, 2 * pi));

    //TODO figure out wtf is going on here to require this hack
    //PrecomputedSimplexSeed s33d = PrecomputedSimplexSeed(simplexSeed.perm);

    return noise4d(nx, ny, nz, nw);
}

#define WORLD_SIZE 4096.0

float fractalNoise(vec2 pos, int octaves, float freq, float persistence) {
    float frequency = freq;
    float acc = 0.0;
    float maxAmplitude = 0.0;
    float amplitude = 1.0;

    frequency *= (WORLD_SIZE / (64 * 32));
    for(int i = 0; i < octaves; i++) {
        acc += looped_noise(pos * frequency, WORLD_SIZE, 1.0 / WORLD_SIZE) * amplitude;
        frequency *= 2.0;
        maxAmplitude += amplitude;
        amplitude *= persistence;
    }

    return acc / maxAmplitude;
}

float ridgedNoise(vec2 pos, int octaves, float freq, float persistence) {
    float frequency = freq;
    float total = 0.0f;
    float maxAmplitude = 0.0f;
    float amplitude = 1.0f;

    frequency *= (WORLD_SIZE / (64 * 32));
    for(int i = 0; i < octaves; i++) {
        total += (1.0f - abs(looped_noise(pos * frequency, WORLD_SIZE, 0.0))) * amplitude;
        frequency *= 2.0f;
        maxAmplitude += amplitude;
        amplitude *= persistence;
    }
    return total / maxAmplitude;
}   
struct SampledDirection {
    vec3 direction;
    float pdf;
};

/// Generates a cosine-weighted sample on a hemisphere
SampledDirection sample_direction_hemisphere_cosine_weighted(vec2 randomVals) {
    float phi = 2.0*PI*randomVals.x;
    float theta = acos(sqrt(randomVals.y));

    float s = sqrt(1 - randomVals.y);

    //float pdf = cos(theta) * INVPI;
    float pdf = sqrt(randomVals.y) * INVPI;

    vec3 direction =  vec3(cos(phi) * s, sin(phi) * s, sqrt(randomVals.y));
    return SampledDirection(direction, pdf);
}

/// Creates tangent vectors from normal vector
/// from pbrt
void generate_tangents(vec3 v1, out vec3 v2, out vec3 v3) {
    if(abs(v1.x) > abs(v1.y)) {
        float invLen = 1.0 / v1.xz.length();
        v2 = vec3(-v1.z * invLen, 0.0, v1.x * invLen);
    } else {
        float invLen = 1.0 / v1.yz.length();
        v2 = vec3(0.0, v1.z * invLen, -v1.y * invLen);
    }
    v3 = cross(v1, v2);
}

/// Generates a cosine-weighted sample on a hemisphere wrt to the given normal
SampledDirection sample_direction_hemisphere_cosine_weighted_with_normal(vec2 randomVals, vec3 normal) {
    int i = 0;
    SampledDirection sample_ = sample_direction_hemisphere_cosine_weighted(randomVals);

    vec3 t1, t2;
    generate_tangents(normal, t1, t2);

    vec3 mapped_dir = t1 * sample_.direction.x + t2 * sample_.direction.y + normal * sample_.direction.z;
    //return sample;
    return SampledDirection(mapped_dir, sample_.pdf);
}

const float UNIFORM_SAMPLED_SPHERE_PDF = 1.0 / (4.0 * PI);
SampledDirection sample_direction_sphere_uniform(vec2 randomVals) {
    // from https://www.bogotobogo.com/Algorithms/uniform_distribution_sphere.php
    float theta = 2.0*PI*randomVals.x;
    float phi = acos(2.0*randomVals.y-1.0);

    vec3 direction = vec3(cos(theta)*sin(phi), sin(theta)*sin(phi), cos(phi));
    return SampledDirection(direction, UNIFORM_SAMPLED_SPHERE_PDF);
}
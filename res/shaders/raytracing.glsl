struct Ray {
    vec3 origin;
    vec3 direction;
    float tmin, tmax;
};

struct Hit {
    float t;
    ivec3 voxel;
    vec4 data;
    vec3 normal;
    #ifdef COUNT_STEPS
    int steps;
    #endif
};
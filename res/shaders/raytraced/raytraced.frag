#version 450

#extension GL_EXT_control_flow_attributes : require
#extension GL_KHR_shader_subgroup_basic : require
#extension GL_KHR_shader_subgroup_vote : require

#define sunRadiance sunAbsorb
#define skyRadiance getAtmosphericScatteringAmbient() / pi
#define INVPI (1.0 / PI)
#define FLT_MAX 3.402823466e+38

const int voxel_data_size = VOLUME_TEXTURE_SIZE;
const float voxel_data_sizef = float(VOLUME_TEXTURE_SIZE);
const float inverseVoxelDataSize = 1.0 / voxel_data_sizef;
const float goldenRatio = (1.0 + sqrt(5.0)) / 2.0;

//Passed variables
in vec2 vertexPos;
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

uniform sampler2D blueNoise;
uniform sampler3D voxelData;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.graphics.vulkan.systems.drawing.rt.VolumetricTextureMetadata
uniform VolumetricTextureMetadata voxelDataInfo;

#include struct xyz.chunkstories.graphics.common.structs.ViewportSize
uniform ViewportSize viewportSize;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#define COUNT_STEPS
//#define RT_USE_MASK_OPS yes

// define that to x for coherence subgroup ops to not do anything
#define COHERENCE_ANY(x) subgroupAny(x)
//#define COHERENCE_ANY(x) x

#include ../sky/sky.glsl
#include ../bbox.glsl
#include ../raytracing.glsl
#include ../sampling.glsl
#include ../normalcompression.glsl

const vec3[6] cube_normals = vec3[6](
    vec3(1.0, 0.0, 0.0),
    vec3(-1.0, 0.0, 0.0),
    vec3(0.0, 1.0, 0.0),
    vec3(0.0, -1.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(0.0, 0.0, -1.0)
);

vec3 make_normal(int face) {
    /*if(face == 0) {
        return vec3(1.0, 0.0, 0.0);
    } else if(face == 1) {
        return vec3(-1.0, 0.0, 0.0);
    } else if(face == 2) {
        return vec3(0.0, 1.0, 0.0);
    } else if(face == 3) {
        return vec3(0.0, -1.0, 0.0);
    } else if(face == 4) {
        return vec3(0.0, 0.0, 1.0);
    } else if(face == 5) {
        return vec3(0.0, 0.0, -1.0);
    } 
    return vec3(0.0);*/
    return cube_normals[face];
}

const int maxLevel = 5;

Hit raytrace(in Ray ray) {	
    #ifdef COUNT_STEPS
    int steps = 0;
    #endif

    int level = maxLevel;
    int gridx = int(floor(ray.origin.x + 0.0));
    int gridy = int(floor(ray.origin.y + 0.0));
    int gridz = int(floor(ray.origin.z + 0.0));
    #define gridPosition ivec3(gridx, gridy, gridz)

    if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size)))) {
        return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0)
        #ifdef COUNT_STEPS
        , steps
        #endif
        );
    }

    vec3 tSpeed = abs(vec3(1.0) / ray.direction);
    vec3 timeToEdge = vec3(0.0);
    vec3 nextEdge = vec3(gridPosition & ivec3(-1 << level)) + (vec3(greaterThan(ray.direction, vec3(0.0)))) * (1 << level);
    timeToEdge = abs((nextEdge - ray.origin) * tSpeed);
    
    bool raydirsignx = ray.direction.x > 0;
    bool raydirsigny = ray.direction.y > 0;
    bool raydirsignz = ray.direction.z > 0;
     
    float f = -1.0;
    int out_face = 0;

    vec4 data = vec4(0.0);
    
    bool hit = false;
    bool horizontal_move = true;
    [[loop]]
    while(COHERENCE_ANY(horizontal_move)) {
        // Vertical movement (across mip lvls)
        bool vertical_move = !hit;
        [[loop]]
        while(COHERENCE_ANY(vertical_move)) {
            bool non_empty;
            bool above_non_empty;
            
            non_empty = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> (level), clamp(level, 0, maxLevel)).w > 0.0;
            above_non_empty = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> (level + 1), clamp(level + 1, 0, maxLevel)).w > 0.0;
            
            if((non_empty && level == 0) || (!non_empty && (level + 1 > maxLevel || above_non_empty)))
                vertical_move = false;

            // non_empty == true => moving down
            // non_empty == false => moving up
            
            // Uncomment for forcing coherent ray traversal
            /*bool moving_down = vertical_move && non_empty;
            bool moving_up = vertical_move && !non_empty;

            if(subgroupAll(moving_up)) {
                // ok
            } else if(subgroupAny(moving_down)) {
                vertical_move = true;
                non_empty = true;
            } else {
                vertical_move = false;
            }*/

            if(vertical_move) {
                level -= int(non_empty);

                {
                bool qx = ((gridx >> level) & 1) == 1;
                float dx = (((raydirsignx) ^^ qx) ? tSpeed.x * float(1 << level) : 0.0f);
                timeToEdge.x += non_empty ? -dx : dx;
                }

                {
                bool qy = ((gridy >> level) & 1) == 1;
                float dy = (((raydirsigny) ^^ qy) ? tSpeed.y * float(1 << level) : 0.0f);
                timeToEdge.y += non_empty ? -dy : dy;
                }

                {
                bool qz = ((gridz >> level) & 1) == 1;
                float dz = (((raydirsignz) ^^ qz) ? tSpeed.z * float(1 << level) : 0.0f);
                timeToEdge.z += non_empty ? -dz : dz;
                }

                level += int(!non_empty);
            }

            #ifdef COUNT_STEPS
            steps++;
            #endif
        }

        float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

        if(minTime > ray.tmax)
            horizontal_move = false;

        if(level == 0 && minTime > f && f >= ray.tmin) {
            data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> level, level);
            if(data.a != 0.0) {
                hit = hit || horizontal_move;
                horizontal_move = false;
            }
        }

        if(horizontal_move) {
            [[branch]]
            if(minTime == timeToEdge.x) {
                int vstepx = mix(-1, 1 << level, raydirsignx);
                gridx = (gridx & (-1 << level)) + vstepx;

                if(((gridx - voxelDataInfo.baseChunkPos.x * 32) & 0xFFFF) >= voxel_data_size)
                    horizontal_move = false;
                timeToEdge.x += tSpeed.x * float(1 << level);
                if(minTime > f)
                    out_face = 0 + int(raydirsignx);
            } else if(minTime == timeToEdge.y) {
                int vstepy = mix(-1, 1 << level, raydirsigny);
                gridy = (gridy & (-1 << level)) + vstepy;

                if(((gridy - voxelDataInfo.baseChunkPos.y * 32) & 0xFFFF) >= voxel_data_size)
                    horizontal_move = false;
                timeToEdge.y += tSpeed.y * float(1 << level);
                if(minTime > f)
                    out_face = 2 + int(raydirsigny);
            } else {
                int vstepz = mix(-1, 1 << level, raydirsignz);
                gridz = (gridz & (-1 << level)) + vstepz;

                if(((gridz - voxelDataInfo.baseChunkPos.z * 32) & 0xFFFF) >= voxel_data_size)
                    horizontal_move = false;
                timeToEdge.z += tSpeed.z * float(1 << level);
                if(minTime > f)
                    out_face = 4 + int(raydirsignz);
            }
            f = max(f, minTime);
        }

        #ifdef COUNT_STEPS
        steps++;
        #endif
    }

    data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> level, level);
    if(!hit) {
        data = vec4(0.0);
        f = -1;
    }

    return Hit(f, gridPosition, data, make_normal(out_face)
    #ifdef COUNT_STEPS
    , steps
    #endif
    );
}

#undef gridPosition

Hit raytrace_non_mipmapped(Ray ray) {
    #ifdef COUNT_STEPS
    int steps = 0;
    #endif

    ivec3 gridPosition = ivec3(floor(ray.origin + 0.));

    if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size)))) {
        return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0)
        #ifdef COUNT_STEPS
        , steps
        #endif
        );
    }

    vec3 tSpeed = abs(vec3(1.0) / ray.direction);
    ivec3 vstep = ivec3(greaterThan(ray.direction, vec3(0.0))) * 2 - ivec3(1);

    vec3 nextEdge = floor(ray.origin) + vec3(vstep) * 0.5 + vec3(0.5);
    
    vec3 timeToEdge = abs((nextEdge - ray.origin) * tSpeed);
    float f = 0.0;
    vec3 normal = vec3(0.0);

    // Stupid thing to stop drivers from crashing when the normal exit condition is fudged
    int buggy_driver = 0;

    while(true) {
        float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

        if(minTime > ray.tmax)
            break;

        vec4 data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)), 0);
        if(data.a != 0.0 && minTime >= ray.tmin) {
            return Hit(f, gridPosition, data, normal
            #ifdef COUNT_STEPS
            , steps
            #endif
            );
        }

        buggy_driver++;
        if(buggy_driver > 512) {
            discard;
        }
        
        #ifdef COUNT_STEPS
        steps++;
        #endif

        #ifdef RT_USE_MASK_OPS
            bvec3 mask = lessThanEqual(timeToEdge.xyz, min(timeToEdge.yzx, timeToEdge.zxy));
            gridPosition += ivec3(mask) * vstep;
            timeToEdge += vec3(mask) * tSpeed;
            normal = vec3(mask) * -vec3(vstep);
            if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
                break;
        #else
            if(minTime == timeToEdge.x) {
                gridPosition.x += vstep.x;
                if(((gridPosition.x - voxelDataInfo.baseChunkPos.x * 32) & 0xFFFF) >= voxel_data_size)
                    break;
                timeToEdge.x += tSpeed.x;
                normal = vec3(-float(vstep.x), 0.0, 0.0);
            } else if(minTime == timeToEdge.y) {
                gridPosition.y += vstep.y;
                if(((gridPosition.y - voxelDataInfo.baseChunkPos.y * 32) & 0xFFFF) >= voxel_data_size)
                    break;
                timeToEdge.y += tSpeed.y;
                normal = vec3(0.0, -float(vstep.y), 0.0);
            } else {
                gridPosition.z += vstep.z;
                if(((gridPosition.z - voxelDataInfo.baseChunkPos.z * 32) & 0xFFFF) >= voxel_data_size)
                    break;
                timeToEdge.z += tSpeed.z;
                normal = vec3(0.0, 0.0, -float(vstep.z));
            }
        #endif

        f = minTime;
    }

    return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0)
    #ifdef COUNT_STEPS
    , steps
    #endif
    );
}

void main() {
    vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
    vec2 pixelCoordinates = texCoord * vec2(viewportSize.size);
    
    vec4 noise = abs(fract(texture(blueNoise, fract((pixelCoordinates) / vec2(1024.0))) + goldenRatio * int(100 * voxelDataInfo.noise)));

    vec3 prim_org = camera.position;
    vec3 prim_dir = normalize(eyeDirection);

    // COMPLEXITY RENDERER
    /*Ray prim_ray = Ray(prim_org, prim_dir, 0.0, 256.0);
    Hit prim_hit = raytrace(prim_ray);
    colorOut = vec4((vec3(prim_hit.steps * 0.005)) * mix(vec3(0.0, 0.5, 1.0), prim_hit.data.rgb, prim_hit.data.a), 1.0);
    colorOut = vec4(sqrt(vec3(prim_hit.steps) * 0.005), 1.0);*/

    /// PRIMARY RAYS RENDERER
    /*Ray prim_ray = Ray(prim_org, prim_dir, 0.0, 256.0);
    Hit prim_hit = raytrace(prim_ray);
    colorOut = vec4(prim_hit.data) * dot(prim_hit.normal, -eyeDirection);*/

    /// AO RENDERER
    Ray prim_ray = Ray(prim_org, prim_dir, 0.0, 256.0);
    Hit prim_hit = raytrace(prim_ray);
    if(prim_hit.t < 0.0) {
        colorOut = vec4(0.0);
    } else {
        vec3 primary_hit_pos = prim_org + prim_dir * prim_hit.t + prim_hit.normal * 0.001;
        colorOut = prim_hit.data;

        SampledDirection bounce = sample_direction_hemisphere_cosine_weighted_with_normal(noise.xy, prim_hit.normal);
        Ray bounce_ray = Ray(primary_hit_pos, bounce.direction, 0.0, 256.0);
        Hit ao_hit = raytrace(bounce_ray);

        float ao = (ao_hit.t < 0.0 ? 0.2 : 0.0) / bounce.pdf;

        colorOut.xyz = vec3(0.5);
        colorOut.xyz *= ao;
        colorOut.a = 1.0;
    }

    /// PT RENDERER
    /*int rng_seed = 0;
    #define mk_noise (abs(fract(texture(blueNoise, fract((pixelCoordinates + vec2(rng_seed * 73, rng_seed * 69)) / vec2(1024.0))) + goldenRatio * int(100 * voxelDataInfo.noise + rng_seed++))))
    //#define mk_noise noise

    vec3 colorAccumulation = vec3(0);

    const int samples = 1;
    for(int s = 0; s < samples; s++) {
        vec3 color = vec3(0);
        Ray ray = Ray(prim_org, prim_dir, 0.0, 256.0);
        int depth = 0;
        vec3 weight = vec3(1.0);
        bool last_specular = false;
        while(true) {
            if(depth > 2) {
                float rr_death_probability = 1.0;
                if(mk_noise.x >= 1.0 - rr_death_probability) {
                    break;
                } else {
                    weight = weight * (1.0 / rr_death_probability);
                }
                //break;
            }

            if(depth > 15) {
                break;
            }
            
            Hit hit = raytrace(ray);

            if(hit.t >= 0.0) {
                vec3 hitPoint = ray.origin + ray.direction * hit.t + hit.normal * 0.001;
                vec3 hitNormal = hit.normal;

                bool direct_sampling = mk_noise.x > 0.5;
                if(direct_sampling) {
                    //float ndl = dot(hit.normal, world.sunPosition);
                    Ray sun_ray = Ray(hitPoint, normalize(100.0 * world.sunPosition + mk_noise.xyz), 0.0, 256.0);
                    Hit sun_hit = raytrace(sun_ray);
                    color = color + hit.data.rgb * INVPI * 5.0 * sunRadiance * dot(hit.normal, sun_ray.direction) * (sun_hit.t < 0.0 ? 1.0 : 0.0);
                }

                if(last_specular || depth == 0) {
                    vec3 L_e = vec3(1.0) * hit.data.rgb * clamp((hit.data.a - 0.5) * 2.0, 0.0, 1.0);
                    color = color + L_e * weight;
                }

                SampledDirection bsdfSample = sample_direction_hemisphere_cosine_weighted_with_normal(mk_noise.xy, hit.normal);
                vec3 bsdfSample_value = hit.data.rgb * INVPI;

                Ray bounceRay = { hitPoint, bsdfSample.direction, 0.0, 256.0};
                ray = bounceRay;

                weight = weight * bsdfSample_value * dot(hitNormal, ray.direction) / bsdfSample.pdf;
                last_specular = !direct_sampling;
            } else {
                // "sky" color
                if(last_specular || depth == 0) {
                    vec3 L_e = 0.125 * getSkyColor(world.time, normalize(ray.direction));
                    //L_e = 0.25 * getAtmosphericScatteringAmbient();
                    //L_e *= 0.0;
                    color = color + L_e * weight;
                }
                //float sunnyness = clamp(dot(normalize(ray.direction), world.sunPosition), 0.0, 1.0);
                //sunnyness = 1500000000 * pow(sunnyness * 0.9, 150.0);
                //color = color + sunRadiance * sunnyness;
                break;
            }

            depth++;
        }
        colorAccumulation += color;
    }
    colorAccumulation /= samples;
    //colorAccumulation = color;

    colorOut = vec4(sqrt(colorAccumulation), 1.0);*/
}

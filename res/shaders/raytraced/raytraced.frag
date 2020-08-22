#version 450

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

#include ../sky/sky.glsl
#include ../bbox.glsl
#include ../raytracing.glsl
#include ../sampling.glsl
#include ../normalcompression.glsl

//#define RT_USE_MASK_OPS yes

Hit raytrace(Ray ray) {
	ivec3 gridPosition = ivec3(floor(ray.origin + 0.));

	if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
		return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));

	vec3 tSpeed = abs(vec3(1.0) / ray.direction);
	ivec3 vstep = ivec3(greaterThan(ray.direction, vec3(0.0))) * 2 - ivec3(1);

	const int maxLevel = 5;
	int level = maxLevel;

	vec3 nextEdge = vec3(gridPosition & ivec3(-1 << level)) + (vec3(vstep) * 0.5 + vec3(0.5)) * (1 << level);
	vstep = mix(ivec3(-1), ivec3(1 << level), greaterThan(ray.direction, vec3(0.0)));
	
	vec3 timeToEdge = abs((nextEdge - ray.origin) * tSpeed);
	float f = 0.0;
	vec3 normal = vec3(0.0);

	// Stupid thing to stop drivers from crashing when the normal exit condition is fudged
	int buggy_driver = 0;

	while(true) {
		// Vertical movement (across mip lvls)
		while(true) {
			/*bool non_empty = texelFetch(voxelData, 
				(gridPosition & ivec3(voxel_data_size - 1)) >> (level), (level)).w > 0.0;
			bool above_non_empty = texelFetch(voxelData, 
				(gridPosition & ivec3(voxel_data_size - 1)) >> (level + 1), clamp(level + 1, 0, maxLevel)).w > 0.0;
			
			if((non_empty && level == 0) || (!non_empty && (level + 1 > maxLevel)) || (!non_empty && above_non_empty))
				break;*/

			bool non_empty = texelFetch(voxelData, 
				(gridPosition & ivec3(voxel_data_size - 1)) >> (level), (level)).w > 0.0;
			
			if(non_empty && level == 0)
				break;

			if(!non_empty && (level + 1 > maxLevel))
				break;
			
			bool above_non_empty = texelFetch(voxelData, 
				(gridPosition & ivec3(voxel_data_size - 1)) >> (level + 1), (level + 1)).w > 0.0;
		
			if(!non_empty && above_non_empty)
				break;

			level -= mix(0, 1, non_empty);

            /*bool qx = ((gridPosition.x >> level) & 1) == 1;
            bool qy = ((gridPosition.y >> level) & 1) == 1;
            bool qz = ((gridPosition.z >> level) & 1) == 1;

            float dx = (((vstep.x > 0) ^^ qx) ? tSpeed.x * float(1 << level) : 0.0f);
            float dy = (((vstep.y > 0) ^^ qy) ? tSpeed.y * float(1 << level) : 0.0f);
            float dz = (((vstep.z > 0) ^^ qz) ? tSpeed.z * float(1 << level) : 0.0f);

			timeToEdge.x += non_empty ? -dx : dx;
            timeToEdge.y += non_empty ? -dy : dy;
            timeToEdge.z += non_empty ? -dz : dz;*/
			
			ivec3 q = (gridPosition >> ivec3(level)) & ivec3(1);
			vec3 d = mix(vec3(0.0f), tSpeed * vec3(1 << level), ivec3(greaterThan(vstep, ivec3(0))) ^ q);
			d *= float(mix(1, -1, non_empty));
			timeToEdge += d;

            level += mix(1, 0, non_empty);
			//vstep =  ivec3(greaterThan(ray.direction, vec3(0.0))) * 2 - ivec3(1);
			//normal = vec3(0.0);
		}
		vstep = mix(ivec3(-1), ivec3(1 << level), greaterThan(ray.direction, vec3(0.0)));

		float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

		if(minTime > ray.tmax)
			break;

		if(level == 0 && minTime > f && f >= ray.tmin) {
			const int mip = level;
			vec4 data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> mip, mip);
			if(data.a != 0.0) {
				return Hit(f, gridPosition, data, normal);
			}
		}

		buggy_driver++;
		if(buggy_driver > 512) {
			discard;
		}

		#ifdef RT_USE_MASK_OPS
			bvec3 mask = lessThanEqual(timeToEdge.xyz, min(timeToEdge.yzx, timeToEdge.zxy));
			gridPosition = ivec3(not(mask)) * gridPosition + ivec3(mask) * ((gridPosition & ivec3(-1 << level)) + vstep);

			if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
				break;

			timeToEdge += vec3(mask) * tSpeed * float(1 << level);
			if(minTime > f)
				normal = -vec3(mask) * vstep;
		#else
			if(minTime == timeToEdge.x) {
				//gridPosition.x = (gridPosition.x & (-1 << level)) + ((vstep.x > 0) ? (1 << level) : -1);
				gridPosition.x = (gridPosition.x & (-1 << level)) + vstep.x;

				if(((gridPosition.x - voxelDataInfo.baseChunkPos.x * 32) & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.x += tSpeed.x * float(1 << level);
				if(minTime > f)
					normal = vec3(-float(vstep.x), 0.0, 0.0);
			} else if(minTime == timeToEdge.y) {
				//gridPosition.y = (gridPosition.y & (-1 << level)) + ((vstep.y > 0) ? (1 << level) : -1);
				gridPosition.y = (gridPosition.y & (-1 << level)) + vstep.y;

				if(((gridPosition.y - voxelDataInfo.baseChunkPos.y * 32) & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.y += tSpeed.y * float(1 << level);
				if(minTime > f)
					normal = vec3(0.0, -float(vstep.y), 0.0);
			} else {
				//gridPosition.z = (gridPosition.z & (-1 << level)) + ((vstep.z > 0) ? (1 << level) : -1);
				gridPosition.z = (gridPosition.z & (-1 << level)) + vstep.z;

				if(((gridPosition.z - voxelDataInfo.baseChunkPos.z * 32) & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.z += tSpeed.z * float(1 << level);
				if(minTime > f)
					normal = vec3(0.0, 0.0, -float(vstep.z));
			}
		#endif

		f = max(f, minTime);
	}

	return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));
}

Hit raytrace_non_mipmapped(Ray ray) {
	ivec3 gridPosition = ivec3(floor(ray.origin + 0.));

	if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
		return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));

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
		if(data.a != 0.0 && minTime >= ray.tmin)
			return Hit(f, gridPosition, data, normal);

		buggy_driver++;
		if(buggy_driver > 512) {
			discard;
		}

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

	return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));
}

void main() {
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
	vec2 pixelCoordinates = texCoord * vec2(viewportSize.size);
	
	vec4 noise = abs(fract(texture(blueNoise, fract((pixelCoordinates) / vec2(1024.0))) + goldenRatio * int(100 * voxelDataInfo.noise)));

	vec3 prim_org = camera.position;
	vec3 prim_dir = normalize(eyeDirection);

	/// PRIMARY RAYS RENDERER
	Ray prim_ray = Ray(prim_org, prim_dir, 0.0, 256.0);
	Hit prim_hit = raytrace(prim_ray);
	colorOut = vec4(prim_hit.data) * dot(prim_hit.normal, -eyeDirection);

	/// AO RENDERER
	/*Ray prim_ray = Ray(prim_org, prim_dir, 0.0, 256.0);
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
	}*/

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
				float rr_death_probability = 0.75;
				if(mk_noise.x >= rr_death_probability) {
					break;
				} else {
					weight = weight * (1.0 / rr_death_probability);
				}
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
					Ray sun_ray = Ray(hitPoint, normalize(10.0 * world.sunPosition + mk_noise.xyz), 0.0, 256.0);
					Hit sun_hit = raytrace(sun_ray);
					color = color + hit.data.rgb * INVPI * 5.0 * sunRadiance * dot(hit.normal, sun_ray.direction) * (sun_hit.t < 0.0 ? 1.0 : 0.0);
				}

				if(last_specular || depth == 0) {
					vec3 L_e = vec3(1.0)  * clamp((hit.data.a - 0.5) * 2.0, 0.0, 1.0);
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
					L_e = 0.25 * getAtmosphericScatteringAmbient();
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

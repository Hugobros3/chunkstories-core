#version 450

//Passed variables
in vec2 vertexPos;
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

//uniform sampler2D colorBuffer;
//uniform sampler2D normalBuffer;
//uniform sampler2D depthBuffer;

#ifdef USE_SHADOWMAPS
#include struct xyz.chunkstories.graphics.common.structs.ShadowMappingInfo
uniform ShadowMappingInfo shadowInfo;
uniform sampler2DShadow shadowBuffers[4];

void sampleShadowMap(vec4 worldSpacePosition, mat4 cameraMatrix, sampler2DShadow shadowMap, int cascade, float NdL, in out float shadowFactor, in out float outOfBounds)  {
	vec4 coordinatesInShadowmap = cameraMatrix * worldSpacePosition;
	coordinatesInShadowmap.xy *= 0.5;
	coordinatesInShadowmap.xy += vec2(0.5);
	
	if(coordinatesInShadowmap.x > 1.0 || coordinatesInShadowmap.y > 1.0 || coordinatesInShadowmap.x < 0.0 || coordinatesInShadowmap.y < 0.0 || coordinatesInShadowmap.z < 0.0 || coordinatesInShadowmap.z > 1.0) {
		//outOfBounds = 1.0;
		//shadowFactor = 1.0;
		return;
	} else {
		float bias = pow(2.0, 4 - cascade) * 0.0004 * (1.0 - NdL);
		shadowFactor = clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xyz + vec3(0.0, 0.0, bias)), 0.0)), 0.0, 1.0);
		outOfBounds = 0.0;
	}
}

void sampleShadowMaps(vec4 worldSpacePosition, float NdL, in out float shadowFactor, in out float outOfBounds) {
	#define sampleLvl(i) sampleShadowMap(worldSpacePosition, shadowInfo.cameras[i].viewMatrix, shadowBuffers[i], i, NdL, shadowFactor, outOfBounds);

	// Unrolled for GLSL 330 compatibility
	if(shadowInfo.cascadesCount >= 1) {
		sampleLvl(0)
	}
	if(shadowInfo.cascadesCount >= 2) {
		sampleLvl(1)
	}
	if(shadowInfo.cascadesCount >= 3) {
		sampleLvl(2)
	}
	if(shadowInfo.cascadesCount >= 4) {
		sampleLvl(3)
	}
}
#endif

uniform sampler2D blueNoise;

uniform sampler3D voxelData;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

//#define voxel_data_size 128
//#define voxel_data_sizef 128.0

const int voxel_data_size = VOLUME_TEXTURE_SIZE;
const float voxel_data_sizef = float(VOLUME_TEXTURE_SIZE);

#include struct xyz.chunkstories.graphics.vulkan.systems.drawing.rt.VolumetricTextureMetadata
uniform VolumetricTextureMetadata voxelDataInfo;

#include struct xyz.chunkstories.graphics.common.structs.ViewportSize
uniform ViewportSize viewportSize;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#define sunRadiance sunAbsorb
#define skyRadiance getAtmosphericScatteringAmbient() / pi
/*
#define PI 3.14159265359
#define pi PI
#define sunRadiance vec3(1.0)
#define skyRadiance vec3(0.0, 0.2, 0.5)*/

#include ../bbox.glsl
#include ../raytracing.glsl
#include ../normalcompression.glsl

const float goldenRatio = (1.0 + sqrt(5.0)) / 2.0;
const float inverseVoxelDataSize = 1.0 / voxel_data_sizef;

//#define RT_USE_MASK_OPS yes

Hit raytrace(vec3 origin, vec3 direction, float tMax) {
	ivec3 gridPosition = ivec3(floor(origin + 0.));

	if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
		return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));

	vec3 tSpeed = abs(vec3(1.0) / direction);
	ivec3 vstep = ivec3(greaterThan(direction, vec3(0.0))) * 2 - ivec3(1);

	const int maxLevel = 5;
	int level = maxLevel;

	vec3 nextEdge = vec3(gridPosition & ivec3(-1 << level)) + (vec3(vstep) * 0.5 + vec3(0.5)) * (1 << level);
	
	vec3 timeToEdge = abs((nextEdge - origin) * tSpeed);
	float f = 0.0;
	vec3 normal = vec3(0.0);

	// Stupid thing to stop drivers from crashing when the normal exit condition is fudged
	int buggy_driver = 0;

	while(true) {

		// Vertical movement (across mip lvls)
		while(true) {
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

			level -= non_empty ? 1 : 0;

            bool qx = ((gridPosition.x >> level) & 1) == 1;
            bool qy = ((gridPosition.y >> level) & 1) == 1;
            bool qz = ((gridPosition.z >> level) & 1) == 1;

            float dx = (((vstep.x > 0) ^^ qx) ? tSpeed.x * float(1 << level) : 0.0f);
            float dy = (((vstep.y > 0) ^^ qy) ? tSpeed.y * float(1 << level) : 0.0f);
            float dz = (((vstep.z > 0) ^^ qz) ? tSpeed.z * float(1 << level) : 0.0f);

            timeToEdge.x += non_empty ? -dx : dx;
            timeToEdge.y += non_empty ? -dy : dy;
            timeToEdge.z += non_empty ? -dz : dz;

            level += non_empty ? 0 : 1;
			//normal = vec3(0.0);
		}

		float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

		if(level == 0 && minTime > f) {
			const int mip = level;
			vec4 data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)) >> mip, mip);
			if(data.a != 0.0) {
				return Hit(f, gridPosition, data, normal);
			}
		}

		if(minTime > tMax)
			break;

		buggy_driver++;
		if(buggy_driver > 512) {
			discard;
		}

		if(minTime == timeToEdge.x) {
			gridPosition.x = (gridPosition.x & (-1 << level)) + ((vstep.x > 0) ? (1 << level) : -1);

			if(((gridPosition.x - voxelDataInfo.baseChunkPos.x * 32) & 0xFFFF) >= voxel_data_size)
				break;
			timeToEdge.x += tSpeed.x * float(1 << level);
			if(minTime > f)
			normal = vec3(-float(vstep.x), 0.0, 0.0);
		} else if(minTime == timeToEdge.y) {
			gridPosition.y = (gridPosition.y & (-1 << level)) + ((vstep.y > 0) ? (1 << level) : -1);

			if(((gridPosition.y - voxelDataInfo.baseChunkPos.y * 32) & 0xFFFF) >= voxel_data_size)
				break;
			timeToEdge.y += tSpeed.y * float(1 << level);
			if(minTime > f)
			normal = vec3(0.0, -float(vstep.y), 0.0);
		} else {
			gridPosition.z = (gridPosition.z & (-1 << level)) + ((vstep.z > 0) ? (1 << level) : -1);

			if(((gridPosition.z - voxelDataInfo.baseChunkPos.z * 32) & 0xFFFF) >= voxel_data_size)
				break;
			timeToEdge.z += tSpeed.z * float(1 << level);
			if(minTime > f)
			normal = vec3(0.0, 0.0, -float(vstep.z));
		}

		f = max(f, minTime);
	}

	return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));
}

Hit raytrace_non_mipmapped(vec3 origin, vec3 direction, float tMax) {
	ivec3 gridPosition = ivec3(floor(origin + 0.));

	if(any(greaterThanEqual((gridPosition - voxelDataInfo.baseChunkPos * ivec3(32)) & ivec3(0xFFFF), ivec3(voxel_data_size))))
		return Hit(-1.0f, gridPosition, vec4(0.0), vec3(0.0));

	vec3 tSpeed = abs(vec3(1.0) / direction);
	ivec3 vstep = ivec3(greaterThan(direction, vec3(0.0))) * 2 - ivec3(1);

	vec3 nextEdge = floor(origin) + vec3(vstep) * 0.5 + vec3(0.5);
	
	vec3 timeToEdge = abs((nextEdge - origin) * tSpeed);
	float f = 0.0;
	vec3 normal = vec3(0.0);

	// Stupid thing to stop drivers from crashing when the normal exit condition is fudged
	int buggy_driver = 0;

	while(true) {
		vec4 data = texelFetch(voxelData, (gridPosition & ivec3(voxel_data_size - 1)), 0);
		if(data.a != 0.0)
			return Hit(f, gridPosition, data, normal);

		float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

		if(minTime > tMax)
			break;

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

vec3 sunlightContribution(vec3 position, vec3 surfaceNormal) {
	float ndl = clamp(dot(normalize(world.sunPosition), surfaceNormal), 0.0, 1.0);
	if(ndl == 0.0)
		return vec3(0.0);

	#ifdef USE_SHADOWMAPS
		float shadowFactor = 1.0;
		float outOfBounds = 1.0;

		sampleShadowMaps(vec4(position, 1.0), ndl, shadowFactor, outOfBounds);

		shadowFactor = mix(shadowFactor, 1.0, outOfBounds);
		return sunRadiance * ndl * shadowFactor;
	#else
		Hit hit = raytrace(position, normalize(world.sunPosition), 64.0);
		if (hit.t >= 0.0) {
			return vec3(0.0);
		}

		return sunRadiance * ndl;
	#endif
}

void radiosityBounce(in vec3 rayPos, in vec3 rayDir, out float ao, out vec4 colour) {
	Hit hit = raytrace(rayPos, normalize(rayDir), 64.0);
	//ao = hit.t >= 0.0 ? 1.0 : 0.0;
	if(hit.t >= 0.0) {
		ao = 0.0;

		vec3 hit_pos = rayPos + rayDir * hit.t;

		//what if we sampled the shadowmap there HUMMMM
		vec3 bouncedLight = sunlightContribution(hit_pos, hit.normal) * hit.data.rgb;
		vec3 emittedLight = hit.data.rgb * clamp((hit.data.a - 0.5) * 2.0, 0.0, 1.0);
		
		colour.rgb = (bouncedLight + emittedLight);
		colour.a = 1.0;
	} else {
		ao = 1.0;
		colour = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}
}

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer) {
    vec4 cameraSpacePosition = camera.projectionMatrixInverted * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

vec3 mapRectToCosineHemisphere(const vec3 n, const vec2 uv) {
	// create tnb:
	//http://jcgt.org/published/0006/01/01/paper.pdf
	float signZ = (n.z >= 0.0f) ? 1.0f : -1.0f;     //do not use sign(nor.z), it can produce 0.0
	float a = -1.0f / (signZ + n.z);
	float b = n.x * n.y * a;
	vec3 b1 = vec3(1.0f + signZ * n.x * n.x * a, signZ*b, -signZ * n.x);
	vec3 b2 = vec3(b, signZ + n.y * n.y * a, -n.y);

	// remap uv to cosine distributed points on the hemisphere around n
	float phi = 2.0f * 3.141592 * uv.x;
	float cosTheta = sqrt(uv.y);
	float sinTheta = sqrt(1.0f - uv.y);
	return normalize(cosTheta * (cos(phi)*b1 + sin(phi)*b2) + sinTheta * n);
}

void main() {
	/*colorOut = vec4(1.0, 0.0, 0.0, 1.0);
	
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
	vec4 albedo = pow(texture(colorBuffer, texCoord), vec4(2.1));

	float blocklight = texture(normalBuffer, texCoord).w;

	if(albedo.a < 1.0) {
		discard;
	}

	vec4 worldSpacePosition = camera.viewMatrixInverted * convertScreenSpaceToCameraSpace(texCoord, depthBuffer);

	vec4 accumulator = vec4(1.0);
	float ao;
	
	vec2 pixelCoordinates = texCoord * vec2(viewportSize.size);
	vec4 noise = fract(texture(blueNoise, (pixelCoordinates) / vec2(1024.0)) + goldenRatio * voxelDataInfo.noise);

	vec3 normal = normalize(camera.normalMatrixInverted * normalize(decodeNormal(texture(normalBuffer, texCoord).rg)));
	vec3 direction = mapRectToCosineHemisphere(normal, noise.xz);

	vec3 adjusted_worldspace_pos = worldSpacePosition.xyz + normal * 0.01;
	radiosityBounce(adjusted_worldspace_pos, direction, ao, accumulator);
	
	vec4 litPixel = vec4(0.0, 0.0, 0.0, 1.0);

	// Ambient light
	litPixel.rgb += ao * skyRadiance;

	litPixel.rgb += vec3(clamp((blocklight - 14.0 / 15.0) * 150.0, 0.0, 1.0));

	// Direct light
	litPixel.rgb += sunlightContribution(adjusted_worldspace_pos, normal);

	// Light bounce
	litPixel.rgb += accumulator.rgb;

	litPixel *= pi;

	colorOut = litPixel * albedo;*/

	Hit hit = raytrace(camera.position, normalize(eyeDirection), 256.0);
	colorOut = hit.data;
	colorOut.xyz *= dot(hit.normal, -eyeDirection);
	//colorOut.xyz = hit.normal * 0.5 + vec3(0.5);
	//colorOut.xyz *= 1.0 - clamp(log(sqrt(hit.t)), 0.0, 0.9);
	//colorOut.xyz = normalize(eyeDirection);
	colorOut.a = 1.0;
	
	//colorOut = vec4(noise.xyz, 1.0);
}

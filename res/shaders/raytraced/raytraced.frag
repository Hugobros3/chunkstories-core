#version 450

//Passed variables
in vec2 vertexPos;
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

uniform sampler2D colorBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D depthBuffer;

uniform sampler2D blueNoise;

uniform sampler3D voxelData;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#define voxel_data_size 128
#define voxel_data_sizef 128.0

#define MAX_RAY_STEPS_GI 64
#define MAX_RAY_STEPS_SHADOW 64

#include struct xyz.chunkstories.graphics.vulkan.systems.drawing.rt.VolumetricTextureMetadata
uniform VolumetricTextureMetadata voxelDataInfo;

#include struct xyz.chunkstories.graphics.common.structs.ViewportSize
uniform ViewportSize viewportSize;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

const float goldenRatio = (1.0 + sqrt(5.0)) / 2.0;
const float inverseVoxelDataSize = 1.0 / voxel_data_sizef;

struct Hit {
	float t;
	ivec3 voxel;
	vec3 data;
	vec3 normal;
};

#define RT_USE_MASK_OPS yes

Hit raytrace(vec3 origin, vec3 direction, float tMax) {
	ivec3 gridPosition = ivec3(floor(origin + 0.)) & ivec3(voxel_data_size - 1);

	vec3 tSpeed = abs(vec3(1.0) / direction);
	/*ivec3 vstep = ivec3(
		direction.x >= 0.0 ? 1 : -1,
	 	direction.y >= 0.0 ? 1 : -1,
	 	direction.z >= 0.0 ? 1 : -1);*/
	ivec3 vstep = ivec3(greaterThan(direction, vec3(0.0))) * 2 - ivec3(1);

	vec3 nextEdge = floor(origin) + vec3(vstep) * 0.5 + vec3(0.5);

	vec3 timeToEdge = abs((nextEdge - origin) * tSpeed);
	float f = 0.0;
	vec3 normal = vec3(0.0);

	while(true) {
		vec4 data = texture(voxelData, gridPosition * inverseVoxelDataSize);
		if(data.a != 0.0)
			return Hit(f, gridPosition, data.rgb, normal);

		float minTime = min(timeToEdge.x, min(timeToEdge.y, timeToEdge.z));

		if(minTime > tMax)
			break;

		#ifdef RT_USE_MASK_OPS
			bvec3 mask = lessThanEqual(timeToEdge.xyz, min(timeToEdge.yzx, timeToEdge.zxy));
			gridPosition += ivec3(mask) * vstep;
			timeToEdge += vec3(mask) * tSpeed;
			normal = vec3(mask) * vec3(vstep);
			if(any(greaterThanEqual(gridPosition & ivec3(0xFFFF), ivec3(voxel_data_size))))
				break;
		#else
			if(minTime == timeToEdge.x) {
				gridPosition.x += vstep.x;
				if((gridPosition.x & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.x += tSpeed.x;
				normal = vec3(float(vstep.x), 0.0, 0.0);
			} else if(minTime == timeToEdge.y) {
				gridPosition.y += vstep.y;
				if((gridPosition.y & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.y += tSpeed.y;
				normal = vec3(0.0, float(vstep.y), 0.0);
			} else {
				gridPosition.z += vstep.z;
				if((gridPosition.z & 0xFFFF) >= voxel_data_size)
					break;
				timeToEdge.z += tSpeed.z;
				normal = vec3(0.0, 0.0, float(vstep.z));
			}
		#endif

		f = minTime;
	}

	return Hit(-1.0f, gridPosition, vec3(0.0), vec3(0.0));
}

bool getVoxel(ivec3 c) {
	ivec3 adjusted = c - voxelDataInfo.baseChunkPos * 32;
	vec3 adjusted_scaled = mod(c, voxel_data_size) * inverseVoxelDataSize;

	bvec3 outOfBounds1 = lessThan(adjusted, ivec3(0));
	bvec3 outOfBounds2 = greaterThanEqual(adjusted, vec3(voxel_data_size));
	bool outOfBounds = any(outOfBounds1) || any(outOfBounds2);
	return texture(voxelData, c * inverseVoxelDataSize).a != 0.0 && !outOfBounds;
}

bool shadow(in vec3 rayPos, in vec3 rayDir) {
	Hit hit = raytrace(rayPos, normalize(rayDir), 64.0);
	return (hit.t >= 0.0);
}

void gi(in vec3 rayPos, in vec3 rayDir, out float ao, out vec4 colour) {
	vec3 sunLight_g = sunAbsorb;

	Hit hit = raytrace(rayPos, normalize(rayDir), 64.0);
	ao = hit.t >= 0.0 ? 1.0 : 0.0;
	if(hit.t >= 0.0) {
		ao = 1.0;

		vec3 hit_pos = rayPos + rayDir * hit.t;

		//what if we sampled the shadowmap there HUMMMM
		vec3 light = float(!shadow(vec3(hit_pos), normalize(world.sunPosition))) * sunLight_g * clamp(dot(-normalize(world.sunPosition), vec3(hit.normal) * sign(rayDir)), 0.0, 1.0);
		
		light += colour.rgb * clamp((colour.a - 0.5) * 2.0, 0.0, 1.0);
		
		colour.rgb *= light;
		colour.a = 1.0;
	} else {
		ao = 0.0;
		colour = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}
}

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer) {
    vec4 cameraSpacePosition = camera.projectionMatrixInverted * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

//note: uniform pdf rand [0;1[
float hash12n(vec2 p) {
	p  = fract(p * vec2(5.3987, 5.4421));
    p += dot(p.yx, p.xy + vec2(21.5351, 14.3137));
	return fract(p.x * p.y * 95.4307);
}

void main() {
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5);
	vec4 albedo = pow(texture(colorBuffer, texCoord), vec4(2.1));
	//vec3 normal = camera.normalMatrixInverted * texture(normalBuffer, texCoord).xyz * 2.0 - vec3(1.0);
	vec3 decodedNormal = normalize(decodeNormal(texture(normalBuffer, texCoord).rg));
	vec3 normal = normalize(camera.normalMatrixInverted * decodedNormal);

	float blocklight = texture(normalBuffer, texCoord).w;

	if(albedo.a < 1.0) {
		discard;
	}

	vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(texCoord, depthBuffer);
	vec4 worldSpacePosition = camera.viewMatrixInverted * cameraSpacePosition;

	vec4 accumulator = vec4(1.0);
	float ao;
	
	vec2 pixelCoordinates = texCoord * vec2(viewportSize.size);
	vec2 noiseBucket = floor(pixelCoordinates / vec2(64.0));
	
	int tileId = int(fract(hash12n(noiseBucket) + goldenRatio * voxelDataInfo.noise) * 256.0) % 256;

	float tx = tileId % 8;
	float ty = tileId / 8;
	vec2 offsetTimed = vec2(64 * tx, 64 * ty);
	vec4 baseNoise = clamp(texture(blueNoise, (pixelCoordinates + offsetTimed) / vec2(1024.0)), 0.0, 0.9999);

	vec4 noise = vec4(0.0);
	
	/*int noiseSamples = 8;
	for(int i = 0; i < noiseSamples; i++) {
		noise += fract(baseNoise + (float(voxelDataInfo.noise % 8 + i) * gr));
	}
	noise /= float(noiseSamples);*/
	//noise = fract(baseNoise + (float(voxelDataInfo.noise) * gr));
	noise = baseNoise;

	vec3 direction = normalize(noise.xyz * 2.0 - vec3(1.0));

	//Flip the random vector if it's facing in the wrong direction so we get an hemisphere
	direction *= sign(dot(direction, normal));

	vec3 adjusted_worldspace_pos = worldSpacePosition.xyz + normal * 0.1;
	gi(adjusted_worldspace_pos, direction, ao, accumulator);
	
	vec4 litPixel = vec4(0.0, 0.0, 0.0, 1.0);
	// Ambient light
	litPixel.rgb += (1.0 - ao) * getAtmosphericScatteringAmbient() / pi;

	//litPixel.rgb += vec3(pow(blocklight, 2.0));
	litPixel.rgb += vec3(clamp((blocklight - 14.0 / 15.0) * 150.0, 0.0, 1.0));

	// Direct light
	litPixel.rgb += float(!shadow(adjusted_worldspace_pos, world.sunPosition)) * clamp(dot(normal, world.sunPosition), 0.0, 1.0) * sunAbsorb;

	// Light bounce
	litPixel.rgb += accumulator.rgb * pi;

	litPixel *= pi;

	colorOut = litPixel * albedo;
	//colorOut = litPixel;

	/*if(texCoord.x > 0.5)
		colorOut = vec4(baseNoise.x);
	else
		colorOut = vec4(noise.x);*/
	//colorOut = clamp((vec4(noise.x) - vec4(0.5)) * 500.0, 0.0, 1.0);

	//colorOut = vec4(fract(adjusted_worldspace_pos.xyz / 10.0), 1.0);
	//colorOut = accumulator;
	//colorOut = vec4(1.0 - ao);
	//colorOut = vec4(decodedNormal, 1.0);
	//colorOut = vec4(accumulator.xyz, 1.0);
	//colorOut = vec4(eyeDirection, 1.0);
}

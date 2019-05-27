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

#include struct xyz.chunkstories.graphics.vulkan.systems.world.VolumetricTextureMetadata
uniform VolumetricTextureMetadata voxelDataInfo;

#include struct xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize
uniform ViewportSize viewportSize;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

const float gr = (1.0 + sqrt(5.0)) / 2.0;

const float inverseVoxelDataSize = 1.0 / voxel_data_sizef;

bool getVoxel(ivec3 c) {
	ivec3 adjusted = c - voxelDataInfo.baseChunkPos * 32;
	vec3 adjusted_scaled = mod(c, voxel_data_size) * inverseVoxelDataSize;

	bvec3 outOfBounds1 = lessThan(adjusted, ivec3(0));
	bvec3 outOfBounds2 = greaterThanEqual(adjusted, vec3(voxel_data_size));
	/*if(adjusted.x < 0 || adjusted.y < 0 || adjusted.z < 0)
		return false;
	if(adjusted.x >= voxel_data_size || adjusted.y >= voxel_data_size || adjusted.z >= voxel_data_size)
		return false;*/
	bool outOfBounds = any(outOfBounds1) || any(outOfBounds2);
	return texture(voxelData, c * inverseVoxelDataSize).a != 0.0 && !outOfBounds;
}

bool shadow(in vec3 rayPos, in vec3 rayDir) {
	ivec3 mapPosz = ivec3(floor(rayPos + 0.));

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPosz) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;

	int i;

	//bool result = false;
	
	for (i = 0; i < MAX_RAY_STEPS_SHADOW; i++) {
		if (getVoxel(mapPosz)){
		    //result = true;
			return true;
		}
		mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

		sideDist += vec3(mask) * deltaDist;
		mapPosz += ivec3(mask) * rayStep;
	}

	//return result;
	return false;
}

void gi(in vec3 rayPos, in vec3 rayDir, out float ao, out vec4 colour) {
	ivec3 mapPos = ivec3(floor(rayPos + 0.));
	ivec3 initPos = mapPos;

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPos) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;
	
	//float ao = 0.0;
	ao = 0.0;
	
	int i;
	for (i = 0; i < MAX_RAY_STEPS_GI; i++) {
		if (getVoxel(mapPos) && i > 0){
			colour = texture(voxelData, mapPos / voxel_data_sizef);
			ao = 1.0;
			break;
		}
		mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

		sideDist += vec3(mask) * deltaDist;
		mapPos += ivec3(mask) * rayStep;
	}
	//Computing the hit position
	vec3 hit_pos;

	//for each cube face we might have hit, this has it's position
	vec3 faceHitPos = mapPos + clamp(-rayStep * 2.0, vec3(-0.001), vec3(1.001));
	vec3 distanceTraveled = abs(rayPos - faceHitPos); //we derive the distance travaled in each direction

	vec3 axisScaling = distanceTraveled / rayDir; // for each direction we computes a scaling factor
	float axisScaled = dot(vec3(mask), axisScaling); // we actually only cares about the collision axis
	
	hit_pos = rayPos + rayDir * abs(axisScaled);
	
	//crappy method
	//hit_pos = lineIntersection(mapPos - vec3(0.001), mapPos + vec3(1.001), rayPos, rayDir);
	
	//if(colour.a < 1.0)
	//	colour.rgb *= vec3(0.2, 1.0, 0.5);

	vec3 sunLight_g = sunAbsorb;
	//vec3 shadowLight_g = getAtmosphericScatteringAmbient() / pi;
	
	if(i == MAX_RAY_STEPS_GI) {
		colour = vec4(0.0, 0.0, 0.0, 0.0);
		return;
	}
	
	//light += shadowLight_g;

	//what if we sampled the shadowmap there HUMMMM
	vec3 light = float(!shadow(vec3(hit_pos), normalize(world.sunPosition))) * sunLight_g * clamp(dot(-normalize(world.sunPosition), vec3(mask) * sign(rayDir)), 0.0, 1.0);
	
	light += colour.rgb * clamp((colour.a - 0.5) * 2.0, 0.0, 1.0);
	
	colour.rgb *= light;
	colour.a = 1.0;
	
	//add light if the hitpoint happens to be an emmissive block
	//if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
	//	colour.rgb += 1.0 * pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
	
	//colour.a = ao;
}

vec4 convertScreenSpaceToCameraSpace(vec2 screenSpaceCoordinates, sampler2D depthBuffer) {
    vec4 cameraSpacePosition = camera.projectionMatrixInverted * vec4(vec3(screenSpaceCoordinates * 2.0 - vec2(1.0), texture(depthBuffer, screenSpaceCoordinates, 0.0).x), 1.0);
    cameraSpacePosition /= cameraSpacePosition.w;
    return cameraSpacePosition;
}

//note: uniform pdf rand [0;1[
float hash12n(vec2 p)
{
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
	
	int tileId = int(fract(hash12n(noiseBucket) + gr * voxelDataInfo.noise) * 256.0) % 256;

	float tx = tileId % 8;
	float ty = tileId / 8;
	vec2 offsetTimed = vec2(64 * tx, 64 * ty);
	vec4 baseNoise = clamp(texture(blueNoise, (pixelCoordinates + offsetTimed) / vec2(1024.0)), 0.0, 0.9999);
	//baseNoise.x = hash12n(texCoord);

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

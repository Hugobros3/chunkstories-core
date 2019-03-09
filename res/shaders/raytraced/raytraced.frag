#version 450

//Passed variables
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

uniform sampler3D voxelData;

//Common camera matrices & uniforms
#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#define voxel_data_size 128
#define voxel_data_sizef 128.0

#define MAX_RAY_STEPS_GI 128
#define MAX_RAY_STEPS_SHADOW 64

#include struct xyz.chunkstories.graphics.vulkan.systems.world.VolumetricTextureMetadata
uniform VolumetricTextureMetadata voxelDataInfo;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include ../sky/sky.glsl

bool getVoxel(ivec3 c) {
	ivec3 adjusted = c - voxelDataInfo.baseChunkPos * 32;
	vec3 adjusted_scaled = mod(c, voxel_data_size) / voxel_data_sizef;

	if(adjusted.x < 0 || adjusted.y < 0 || adjusted.z < 0)
		return false;
	if(adjusted.x >= voxel_data_size || adjusted.y >= voxel_data_size || adjusted.z >= voxel_data_size)
		return false;
	return texture(voxelData, c / voxel_data_sizef).a != 0.0;
}

bool shadow(in vec3 rayPos, in vec3 rayDir) {
	ivec3 mapPosz = ivec3(floor(rayPos + 0.));

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPosz) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;

	int i;
	for (i = 0; i < MAX_RAY_STEPS_SHADOW; i++) {
		if (getVoxel(mapPosz)){
		    return true;
		}
		mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

		sideDist += vec3(mask) * deltaDist;
		mapPosz += ivec3(mask) * rayStep;
	}

	return false;
}

void gi(in vec3 rayPos, in vec3 rayDir, out vec4 colour) {
	ivec3 mapPos = ivec3(floor(rayPos + 0.));
	ivec3 initPos = mapPos;

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPos) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;
	
	float ao = 0.0;
	
	int i;
	for (i = 0; i < MAX_RAY_STEPS_GI; i++) {
		if (getVoxel(mapPos) && i > 0){
			colour = texture(voxelData, mapPos / voxel_data_sizef);
			ao = 0.85;
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
	
	vec3 sunLight_g = sunAbsorb;
	vec3 shadowLight_g = getAtmosphericScatteringAmbient() / pi;
	
	vec3 light = vec3(0.0);
	if(i == MAX_RAY_STEPS_GI) {
		//colour = vec4(0.0, 0.0, 0.0, 0.0);
	}
	
	light += shadowLight_g;

	//what if we sampled the shadowmap there HUMMMM
	light += float(!shadow(vec3(hit_pos), normalize(world.sunPosition))) * sunLight_g * clamp(dot(-normalize(world.sunPosition), vec3(mask) * sign(rayDir)), 0.0, 1.0);
	colour.rgb *= light;
	
	//add light if the hitpoint happens to be an emmissive block
	//if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
	//	colour.rgb += 1.0 * pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
	
	colour.a = ao;
}

void main()
{
	vec4 accumulator = vec4(1.0);
	vec3 pos = camera.position.xyz;

	/*if(shadow(pos, eyeDirection)) {
		accumulator = vec4(eyeDirection, 1.0);
		accumulator = vec4(normalize(eyeDirection), 1.0);
	}*/

	gi(pos, eyeDirection, accumulator);

	/*for(int i = 0; i < 16; i++) {
		vec4 data = texture(voxelData, pos);
		pos += normalize(eyeDirection.xyz) * 0.25;

		if(data.a > 0.0) {
			accumulator += data;
			break;
		}
	}*/
	
	//colorOut = accumulator;
	colorOut = vec4(accumulator.xyz, 1.0);
	//colorOut = vec4(eyeDirection, 1.0);
}

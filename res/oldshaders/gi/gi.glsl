const int MAX_RAY_STEPS = 48;
const int MAX_RAY_STEPS_SHADOW = 72;
const int MAX_RAY_STEPS_GI = 48;

const int GI_SAMPLES = 1;

uniform sampler3D currentChunk;

uniform int voxel_size;
uniform float voxel_sizef;
uniform vec3 voxelOffset;

bool getVoxel(ivec3 c) {
	if(c.x < 0 || c.y < 0 || c.z < 0)
		return false;
	if(c.x >= voxel_size || c.y >= voxel_size || c.z >= voxel_size)
		return false;
	return texture(currentChunk, vec3(c) / voxel_sizef).a != 0.0;
}

//not optimized at all, ported straight from the java code
//gives the intersection between an aabb and a ray
vec3 lineIntersection(vec3 min, vec3 max, vec3 lineStart, vec3 lineDirectionIn)
{
	float minDist = 0.0;
	float maxDist = 256.0;
	
	vec3 lineDirection = vec3(lineDirectionIn);
	lineDirection = normalize(lineDirection);
	
	vec3 invDir = vec3(1.0 / lineDirection.x, 1.0 / lineDirection.y, 1.0 / lineDirection.z);

	bool signDirX = invDir.x < 0;
	bool signDirY = invDir.y < 0;
	bool signDirZ = invDir.z < 0;

	vec3 bbox = signDirX ? max : min;
	float tmin = (bbox.x - lineStart.x) * invDir.x;
	bbox = signDirX ? min : max;
	float tmax = (bbox.x - lineStart.x) * invDir.x;
	bbox = signDirY ? max : min;
	float tymin = (bbox.y - lineStart.y) * invDir.y;
	bbox = signDirY ? min : max;
	float tymax = (bbox.y - lineStart.y) * invDir.y;

	/*if ((tmin > tymax) || (tymin > tmax)) {
		return vec3(0.0);
	}*/
	if (tymin > tmin) {
		tmin = tymin;
	}
	if (tymax < tmax) {
		tmax = tymax;
	}

	bbox = signDirZ ? max : min;
	float tzmin = (bbox.z - lineStart.z) * invDir.z;
	bbox = signDirZ ? min : max;
	float tzmax = (bbox.z - lineStart.z) * invDir.z;

	/*if ((tmin > tzmax) || (tzmin > tmax)) {
		return vec3(0.0);
	}*/
	if (tzmin > tmin) {
		tmin = tzmin;
	}
	if (tzmax < tmax) {
		tmax = tzmax;
	}
	if ((tmin < maxDist) && (tmax > minDist)) {
		vec3 intersect = vec3(lineStart);
		
		intersect += lineDirection * (tmin);
		return intersect;
	}
	return vec3(0.0);
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
	
	
	//vec3 rayPos = vec3(mod(worldSpacePosition.x - voxelOffset.x, voxel_sizef), mod(worldSpacePosition.y - voxelOffset.y, voxel_sizef), mod(worldSpacePosition.z - voxelOffset.z, voxel_sizef));
	
	
	/*vec4 worldSpaceRay = vec4(rayPos + voxelOffset.xyz, 1.0);
	vec4 cameraSpacePosition = modelViewMatrixInv * worldSpaceRay;
	
	
	vec4 shadowCoord = shadowMatrix * (untranslatedMVInv * cameraSpacePosition);
	float distFactor = getDistordFactor(shadowCoord.xy);
	vec4 coordinatesInShadowmap = accuratizeShadow(shadowCoord);

	float clamped = 10 * clamp(NdotL, 0.0, 0.1);
	
	//How much in shadows's brightness the object is
	float shadowIllumination = 0.0;
	
	//How much in shadow influence's zone the object is
	float edgeSmoother = 0.0;
	
	//How much does the pixel is lit by directional light
	float directionalLightning = clamp((NdotL * 1.1 - 0.1), 0.0, 1.0);
	
		//Bias to avoid shadow acne
		float bias = distFactor/32768.0 * 64.0;
		//Are we inside the shadowmap zone edge ?
		edgeSmoother = 1.0-clamp(pow(max(0,abs(coordinatesInShadowmap.x-0.5) - 0.45)*20.0+max(0,abs(coordinatesInShadowmap.y-0.5) - 0.45)*20.0, 1.0), 0.0, 1.0);
		
		shadowIllumination += clamp((texture(shadowMap, vec3(coordinatesInShadowmap.xy, coordinatesInShadowmap.z-bias), 0.0)), 0.0, 1.0);
	
	float sunlightAmount = ( directionalLightning * shadowIllumination * ( mix( shadowIllumination, voxelLight.y, 1-edgeSmoother) )) * clamp(sunPos.y, 0.0, 1.0);
	*/
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
			colour = texture(currentChunk, vec3(mapPos) / voxel_sizef);
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
	
	vec3 sunLight_g = sunLightColor * pi;
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();
	
	vec3 light = vec3(0.0);
	if(i == MAX_RAY_STEPS_GI) {
		light += shadowLight_g;
		colour = vec4(0.0, 0.0, 0.0, 0.0);
	}

	//what if we sampled the shadowmap there HUMMMM
	light += float(!shadow(vec3(hit_pos), normalize(sunPos))) * sunLight_g * clamp(dot(-normalize(sunPos), vec3(mask) * sign(rayDir)), 0.0, 1.0);
	colour.rgb *= light;
	
	//add light if the hitpoint happens to be an emmissive block
	if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
		colour.rgb += 1.0 * pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
	
	colour.a = ao;
}

float bayer2(vec2 a){
    a = floor(a);
    return fract(dot(a,vec2(.5, a.y*.75)));
}

float bayer4(vec2 a)   {return bayer2( .5*a)   * .25     + bayer2(a); }
float bayer8(vec2 a)   {return bayer4( .5*a)   * .25     + bayer2(a); }
float bayer16(vec2 a)  {return bayer4( .25*a)  * .0625   + bayer4(a); }
float bayer32(vec2 a)  {return bayer8( .25*a)  * .0625   + bayer4(a); }
float bayer64(vec2 a)  {return bayer8( .125*a) * .015625 + bayer8(a); }
float bayer128(vec2 a) {return bayer16(.125*a) * .015625 + bayer8(a); }

vec4 giMain(vec4 worldSpacePosition, vec3 normalWorldSpace, vec2 texCoord)
{
	vec4 color = vec4(1.0);
	
	vec4 acc = vec4(0.);
	vec4 contrib = vec4(0.0);
	
	//map the world space position into the 3d texture
	vec3 rayPos = vec3(mod(worldSpacePosition.x - voxelOffset.x, voxel_sizef), mod(worldSpacePosition.y - voxelOffset.y, voxel_sizef), mod(worldSpacePosition.z - voxelOffset.z, voxel_sizef));
	rayPos += normalWorldSpace * 0.1; // offsets it by the normal vector a bit so it doesn't get stuck in a wall
	
	float seed = snoise(vec2(animationTimer + 321.1, 30.5));
	
	//for(int sample = 0; sample < GI_SAMPLES; sample++) {
		/*float rx = -1.0 + 2.0 * bayer16(gl_FragCoord.xy * seed + sample + seed);
		float ry = -1.0 + 2.0 * bayer16(gl_FragCoord.yx * seed);
		float rz = -1.0 + 2.0 * bayer16(gl_FragCoord.xy * ry + animationTimer * rx + 1.0);*/
		
		/*float rx = -1.0 + 2.0 * bayer32(worldSpacePosition.xy*0 + gl_FragCoord.xy + vec2(seed * 0.05, sample)); //snoise(gl_FragCoord.xy * seed + 64.2 + sample + seed);
		float ry = -1.0 + 2.0 * bayer32(worldSpacePosition.yz*0 + gl_FragCoord.yx + vec2(sample, seed)); //snoise(gl_FragCoord.yx * rx * animationTimer * 1.15);
		float rz = -1.0 + 2.0 * bayer32(worldSpacePosition.zx*0 + gl_FragCoord.xy + vec2(seed + 500, sample)); //snoise(gl_FragCoord.xy * ry + 321.1);*/
		
		float rx = snoise(gl_FragCoord.xy * seed + 64.2 + /*sample*/ + seed);
		float ry = snoise(gl_FragCoord.yx * rx * animationTimer * 1.15);
		float rz = snoise(gl_FragCoord.xy * ry + 321.1);
		
		//seed = rz;
		
		vec3 rng = vec3(rx, ry, rz);
		rng = normalize(rng);
		
		//Flip the random vector if it's facing in the wrong direction so we get an hemisphere
		if(dot(rng, normalWorldSpace) < 0)
			rng = -rng;
		
		//gi(hit_pos, rng, contrib);
		gi(rayPos.xyz, rng, contrib);
		acc = max(contrib, 0.0);
	//}
	
	//acc /= float(GI_SAMPLES);
	//color *= acc;
	
	//if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
	//	color.rgb += pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
    
	return acc;
}

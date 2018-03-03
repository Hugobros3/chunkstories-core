#version 330
uniform sampler2D shadedBuffer;

uniform sampler2D albedoBuffer;
uniform sampler2D depthBuffer;
uniform sampler2D normalBuffer;
uniform sampler2D voxelLightBuffer;
uniform sampler2D specularityBuffer;
uniform usampler2D materialBuffer;
uniform sampler2D debugBuffer;

uniform sampler2DShadow shadowMap;

uniform sampler2D bloomBuffer;
uniform sampler2D reflectionsBuffer;

uniform sampler2D pauseOverlayTexture;
uniform float pauseOverlayFade;

uniform samplerCube environmentMap;

in vec2 texCoord;
in vec2 pauseOverlayCoords;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
uniform vec3 camPos;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;
uniform float dayTime;

uniform float animationTimer;
uniform float underwater;

uniform float apertureModifier;
uniform vec2 screenViewportSize;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

const vec4 waterColor = vec4(0.2, 0.4, 0.45, 1.0);

<include ../lib/transformations.glsl>
<include ../lib/shadowTricks.glsl>
<include dither.glsl>
<include ../lib/normalmapping.glsl>
<include ../lib/noise.glsl>
<include ../sky/sky.glsl>

vec4 getDebugShit(vec2 coords);

out vec4 fragColor;

uniform mat4 untranslatedMVInv;
uniform mat4 shadowMatrix;

uniform float shadowVisiblity;
in vec3 eyeDirection;

uniform sampler3D currentChunk;

float bayer2(vec2 a){
    a = floor(a);
    return fract(dot(a,vec2(.5, a.y*.75)));
}

float bayer4(vec2 a)   {return bayer2( .5*a)   * .25     + bayer2(a); }
float bayer8(vec2 a)   {return bayer4( .5*a)   * .25     + bayer2(a); }
float bayer16(vec2 a)  {return bayer4( .25*a)  * .0625   + bayer4(a); }

vec3 ComputeVolumetricLight(vec3 background, vec4 worldSpacePosition, vec3 lightVec, vec3 eyeDirection){
	const int steps = 16;
	const float oneOverSteps = 1.0 / float(steps);

	vec3 startRay = mat3(shadowMatrix) * untranslatedMVInv[3].xyz;
	vec3 endRay = (shadowMatrix * (untranslatedMVInv * worldSpacePosition)).rgb;

	vec3 increment = (startRay - endRay) * oneOverSteps;
	vec3 rayPosition = increment * bayer16(gl_FragCoord.xy) + endRay;

	float weight = clamp(sqrt(dot(increment, increment)), 0.0, 0.25);

	float ray = 0.0;

	for (int i = 0; i < steps; i++){
		vec3 shadowCoord = accuratizeShadow(vec4(rayPosition, 0.0)).rgb + vec3(0.0, 0.0, 0.0001);

		ray += texture(shadowMap, shadowCoord, 0.0);

		rayPosition += increment;
	}
	
	float lDotU = dot(normalize(-lightVec), vec3(0.0, 1.0, 0.0));
	float lDotV = dot(normalize(lightVec), normalize(eyeDirection));
	
	vec3 sunLight_g = sunLightColor;//pow(sunColor, vec3(gamma));
	float sunlightAmount = (ray * shadowVisiblity) * (oneOverSteps * weight) * (gPhase(lDotV, 0.9) * mCoeff);

	return sunlightAmount * sunLight_g * pi;
}

float poltergeist(vec2 coordinate, float seed)
{
    return fract(sin(dot(coordinate*seed, vec2(12.9898, 78.233)))*43758.5453);
}

vec3 jodieReinhardTonemap(vec3 c){
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 tc = c / (c + 1.0);

    return mix(c / (l + 1.0), tc, tc);
}

void main_() {
	vec2 finalCoords = texCoord;
    vec4 cameraSpacePosition = convertScreenSpaceToCameraSpace(finalCoords, depthBuffer);
	
	// Water coordinates distorsion
	finalCoords.x += underwater*sin(finalCoords.x * 50.0 + finalCoords.y * 60.0 + animationTimer * 1.0) / screenViewportSize.x * 5.0;
	finalCoords.y += underwater*cos(finalCoords.y * 60.0 + animationTimer * 1.0) / screenViewportSize.y * 2.0;
	
	// Sampling
	vec4 compositeColor = texture(shadedBuffer, finalCoords);
	
	// Tints pixels blue underwater
	compositeColor = mix(compositeColor, compositeColor * waterColor, underwater);
	
	//Applies reflections
	float reflectionsAmount = texture(specularityBuffer, finalCoords).x;
	
	vec4 reflection = texture(reflectionsBuffer, finalCoords);
	compositeColor.rgb = mix(compositeColor.rgb, reflection.rgb, reflectionsAmount);
	//Dynamic reflections
	
	compositeColor.rgb += ComputeVolumetricLight(compositeColor.rgb, cameraSpacePosition, sunPos, eyeDirection);

	//Applies bloom
	<ifdef doBloom>
	compositeColor.rgb += pow(texture(bloomBuffer, finalCoords).rgb, vec3(gamma)) * pi;
	<endif doBloom>
	
	//Gamma-corrects stuff
	compositeColor.rgb = pow(compositeColor.rgb, vec3(gammaInv));
	
	//Darkens further pixels underwater
	compositeColor = mix(compositeColor, vec4(waterColor.rgb * getSkyColor(dayTime, vec3(0.0, -1.0, 0.0)), 1.0), underwater * clamp(length(cameraSpacePosition) / 32.0, 0.0, 1.0));
	
	// Eye adapatation
	compositeColor *= apertureModifier;
	
	//Dither the final pixel colour
	vec3 its2 = compositeColor.rgb;
    vec3 rnd2 = screenSpaceDither( gl_FragCoord.xy );
    compositeColor.rgb = its2 + rnd2.xyz;
	
	//Applies pause overlay
	vec3 overlayColor = texture(pauseOverlayTexture, pauseOverlayCoords).rgb;
	overlayColor = vec3(
	
	( mod(gl_FragCoord.x + gl_FragCoord.y, 2.0) * 0.45 + 0.55 )
	* 
	( poltergeist(gl_FragCoord.xy, animationTimer) * 0.15 + 0.85 )
	
	);
	compositeColor.rgb *= mix(vec3(1.0), overlayColor, clamp(pauseOverlayFade, 0.0, 1.0));

	compositeColor.rgb = pow(jodieReinhardTonemap(compositeColor.rgb * 5.0), vec3(gamma));
	
	//Ouputs
	fragColor = compositeColor;
	
	//Debug flag
	<ifdef debugGBuffers>
	fragColor = getDebugShit(texCoord);
	<endif debugGBuffers>
}

//Draws divided screen with debug buffers
vec4 getDebugShit(vec2 coords)
{
	vec2 sampleCoords = coords;
	sampleCoords.x = mod(sampleCoords.x, 0.5);
	sampleCoords.y = mod(sampleCoords.y, 0.5);
	sampleCoords *= 2.0;
	
	vec4 shit = vec4(0.0);
	if(coords.x > 0.5)
	{
		if(coords.y > 0.5)
			shit = pow(texture(shadedBuffer, sampleCoords, 0.0), vec4(gammaInv));
		else
			shit = texture(normalBuffer, sampleCoords);
	}
	else
	{
		if(coords.y > 0.5)
		{
			shit = texture(albedoBuffer, sampleCoords);
			shit += (1.0-shit.a) * vec4(1.0, 0.0, 1.0, 1.0);
		}
		else
		{
			shit = vec4(texture(voxelLightBuffer, sampleCoords).xy, texture(specularityBuffer, sampleCoords).r, 1.0);
			
			<ifdef dynamicGrass>
			
			shit = texture(debugBuffer, sampleCoords, 80.0);
			shit = pow(texture(debugBuffer, sampleCoords, 0.0), vec4(gammaInv));
			<endif dynamicGrass>
			shit = vec4(texture(shadowMap, vec3(sampleCoords, 0.0)), 0.0, 0.0, 1.0);
		}
	}
	shit.a = 1.0;
	return shit;
}

const bool USE_BRANCHLESS_DDA = true;
const int MAX_RAY_STEPS = 128;

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

vec2 rotate2d(vec2 v, float a) {
	float sinA = sin(a);
	float cosA = cos(a);
	return vec2(v.x * cosA - v.y * sinA, v.y * cosA + v.x * sinA);	
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

	if ((tmin > tymax) || (tymin > tmax)) {
		return vec3(0.0);
	}
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

	if ((tmin > tzmax) || (tzmin > tmax)) {
		return vec3(0.0);
	}
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
	for (i = 0; i < 64; i++) {
        if (getVoxel(mapPosz)){
            return true;
        }
            
        //Thanks kzy for the suggestion!
        mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

        sideDist += vec3(mask) * deltaDist;
        mapPosz += ivec3(mask) * rayStep;
	}
	
	return false;
}

const vec3 SUN_LIGHT_COLOUR = vec3(2.0, 2.0, 1.5) * 1.0;

void gi(in vec3 rayPos, in vec3 rayDir, out vec3 colour) {
	ivec3 mapPos = ivec3(floor(rayPos + 0.));
	ivec3 initPos = mapPos;

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPos) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	
	bvec3 mask;
	
    int i;
	for (i = 0; i < 32; i++) {
        if (getVoxel(mapPos)){
            colour = pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
			break;
        }
            
        //Thanks kzy for the suggestion!
        mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

        sideDist += vec3(mask) * deltaDist;
        mapPos += ivec3(mask) * rayStep;
	}
	
	//float distance_traced = pow(0. + (initPos.x - mapPos.x) * (initPos.x - mapPos.x) + (initPos.y - mapPos.y) * (initPos.y - mapPos.y) + (initPos.z - mapPos.z) * (initPos.z - mapPos.z), 0.5);
	vec3 hit_pos = lineIntersection(mapPos - vec3(0.001), mapPos + vec3(1.001), rayPos, rayDir);
	
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();//pow(shadowColor, vec3(gamma));
	
	vec3 light = vec3(0.0);
	if(i == 32)
		light += shadowLight_g;
	light += shadow(vec3(hit_pos), normalize(sunPos)) ? vec3(0) : sunLight_g;
	colour *= light;
	
	if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
		colour += pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
}

void gi2(in vec3 rayPos, in vec3 rayDir, out vec3 colour) {
	ivec3 mapPos = ivec3(floor(rayPos + 0.));
	ivec3 initPos = mapPos;

	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPos) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;
	
    int i;
	for (i = 0; i < 32; i++) {
        if (getVoxel(mapPos)){
            colour = texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb;
			break;
        }
            
        //Thanks kzy for the suggestion!
        mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

        sideDist += vec3(mask) * deltaDist;
        mapPos += ivec3(mask) * rayStep;
	}
	
	float distance_traced = pow(0. + (initPos.x - mapPos.x) * (initPos.x - mapPos.x) + (initPos.y - mapPos.y) * (initPos.y - mapPos.y) + (initPos.z - mapPos.z) * (initPos.z - mapPos.z), 0.5);
	vec3 hit_pos = lineIntersection(mapPos - vec3(0.001), mapPos + vec3(1.001), rayPos, rayDir);
	
	vec3 sunLight_g = sunLightColor * pi;//pow(sunColor, vec3(gamma));
	vec3 shadowLight_g = getAtmosphericScatteringAmbient();//pow(shadowColor, vec3(gamma));
	
	vec3 light = vec3(0.0);
	if(i == 32)
		light += shadowLight_g;
	light += shadow(vec3(hit_pos), normalize(sunPos)) ? vec3(0) : sunLight_g;
	colour *= light;
	
	//colour *= clamp(dot(sunPos, rayDir), 0.5, 1.0);
	
	float rx = snoise(gl_FragCoord.xy + animationTimer + 321.1);
	float ry = snoise(gl_FragCoord.yx * rx * 1.15);
	float rz = snoise(gl_FragCoord.xy * ry + 321.1);
	
	vec3 rng = vec3(rx, ry, rz);
	rng = normalize(rng);
	
	vec3 scratch = vec3(0.);
	gi(vec3(hit_pos), rng, scratch);
	colour *= scratch;
}

void main()
{
	vec3 rayPos = vec3(mod(camPos.x - voxelOffset.x, voxel_sizef), mod(camPos.y - voxelOffset.y, voxel_sizef), mod(camPos.z - voxelOffset.z, voxel_sizef)); 
	vec3 rayDir = eyeDirection;
	
	ivec3 mapPos = ivec3(floor(rayPos + 0.));
	ivec3 initPos = mapPos;
	
	vec3 deltaDist = abs(vec3(length(rayDir)) / rayDir);
	ivec3 rayStep = ivec3(sign(rayDir) + 0.);
	vec3 sideDist = (sign(rayDir) * (vec3(mapPos) - rayPos) + (sign(rayDir) * 0.5) + 0.5) * deltaDist; 
	
	bvec3 mask;
	
    int i;
	for (i = 0; i < MAX_RAY_STEPS; i++) {
        if (getVoxel(mapPos)){
            break;
        }
            
        //Thanks kzy for the suggestion!
        mask = lessThanEqual(sideDist.xyz, min(sideDist.yzx, sideDist.zxy));	

        sideDist += vec3(mask) * deltaDist;
        mapPos += ivec3(mask) * rayStep;
	}
	
	float distance_traced = pow(0. + (initPos.x - mapPos.x) * (initPos.x - mapPos.x) + (initPos.y - mapPos.y) * (initPos.y - mapPos.y) + (initPos.z - mapPos.z) * (initPos.z - mapPos.z), 0.5);
	vec3 hit_pos = lineIntersection(mapPos - vec3(0.001), mapPos + vec3(1.001), rayPos, rayDir);
	//rayPos + distance_traced * rayDir;
	
	vec3 color = texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb;
	if (mask.x) {
		color *= vec3(0.5);
	}
	if (mask.y) {
		color *= vec3(1.0);
	}
	if (mask.z) {
		color *= vec3(0.75);
	}
    
	//color *= shadow(vec3(hit_pos), normalize(sunPos)) ? vec3(0.5) : vec3(1.0);
	
	if(i == MAX_RAY_STEPS) {
        //color.rgb = vec3(0.5, 0.5, 1.0); //
		color.rgb = getSkyColor(dayTime, normalize(eyeDirection));
		//color.rgb = texture(shadedBuffer, texCoord).rgb;
	} else {
	
		int samples = 20;
		vec3 acc = vec3(0.);
		vec3 contrib = vec3(1.0);
		
		float seed = snoise(gl_FragCoord.xy * animationTimer + vec2(321.1, 30.5));
		for(int sample = 0; sample < samples; sample++) {
			float rx = snoise(gl_FragCoord.xy * seed + 64.2 + sample + seed);
			float ry = snoise(gl_FragCoord.yx * rx * animationTimer * 1.15);
			float rz = snoise(gl_FragCoord.xy * ry + 321.1);
			seed = rz;
			
			vec3 rng = vec3(rx, ry, rz);
			rng = normalize(rng);
			
			gi(hit_pos, rng, contrib);
			acc += max(contrib * 1.0, 0.0);
			
			//gi(hit_pos, rng, contrib);
			//acc += max(contrib * 2.0, 0.0);
			
			//acc += shadow(vec3(hit_pos), normalize(mix(normalize(sunPos), rng, 0.25))) ? vec3(0.3, 0.3, 0.6) : vec3(1.0);
		}
		acc /= float(pow(samples, 1.0));
		
		color *= acc;
		
		if(texture(currentChunk, vec3(mapPos) / voxel_sizef).a >= 2.0 / 255.0)
			color += pow(texture(currentChunk, vec3(mapPos) / voxel_sizef).rgb, vec3(gamma));
    }
	
	fragColor.a = 1.0;
	fragColor.rgb = pow(color, vec3(gammaInv));
	fragColor.rgb = pow(jodieReinhardTonemap(fragColor.rgb * 5.0), vec3(gamma));
	
	<ifdef debugGBuffers>
	fragColor = getDebugShit(texCoord);
	<endif debugGBuffers>
	//fragColor.r = texture(currentChunk, vec3(gl_FragCoord.xy / 500.0, 0.5)).x;
	//fragColor.rgb = vec3(0.1 * noiseDeriv);
}
vec4 raytrace(sampler2D depthBuffer, sampler2D colorBuffer, vec3 fragpos, vec3 rvector);

vec4 computeReflectedPixel(sampler2D depthBuffer, sampler2D colorBuffer, samplerCube fallbackCubemap, vec2 screenSpaceCoords, vec3 cameraSpacePosition, vec3 pixelNormal, float showSkybox)
{
    vec2 screenSpacePosition2D = screenSpaceCoords;
	
    vec3 cameraSpaceViewDir = normalize(cameraSpacePosition);
    vec3 cameraSpaceVector = normalize(reflect(cameraSpaceViewDir, pixelNormal));
    
	// Is the reflection pointing in the right direction ?
	vec4 color = vec4(0.0);
	
	//SSR stepping goes here
	#ifdef realtimeReflections
	color = raytrace(depthBuffer, colorBuffer, cameraSpacePosition, cameraSpaceVector);
	#endif
	
	vec3 normSkyDirection = normalMatrixInv * cameraSpaceVector;
		
	vec3 skyColor = getSkyColor(dayTime, normSkyDirection);
	
	if(color.a == 0.0)
	{
		#ifdef doDynamicCubemaps
		skyColor = texture(fallbackCubemap, vec3(normSkyDirection.x, -normSkyDirection.y, -normSkyDirection.z)).rgb;
		#endif
	
		skyColor *= showSkybox;
		color.rgb = skyColor;
	}
	return color;
}

// CREDIT: Robobo1211's Shader from http://robobo1221.net
// Minor modifications applied to make it fit in chunkstories's shader codebase

vec3 nvec3(vec4 pos) {
    return pos.xyz/pos.w;
}

vec4 nvec4(vec3 pos) {
    return vec4(pos.xyz, 1.0);
}

//don't touch these lines if you don't know what you do!
const int maxf = 3;				//number of refinements
const float stp = 1.5;			//size of one step for raytracing algorithm
const float ref = 0.025;		//refinement multiplier
const float inc = 2.2;			//increasement factor at each step

float cdist(vec2 coord) {
	return max(abs(coord.x-0.5),abs(coord.y-0.5))*2.0;
}
vec4 raytrace(sampler2D depthBuffer, sampler2D colorBuffer, vec3 fragpos, vec3 rvector/*, float fresnel*/) {
	//#define fragdepth texture(depthBuffer, pos.st).r

	float border = 0.0;
	vec3 pos = vec3(0.0);

	vec4 color = vec4(0.0);
	vec3 start = fragpos;
	vec3 vector = stp * rvector;
	vec3 oldpos = fragpos;

	fragpos += vector;
	vec3 tvector = vector;
	int sr = 0;

	for(int i=0;i<18;i++){
		pos = nvec3(projectionMatrix * nvec4(fragpos)) * 0.5 + 0.5;

		if(pos.x < 0 || pos.x > 1 || pos.y < 0 || pos.y > 1 || pos.z < 0 || pos.z > 1.0) break;

		vec3 spos = vec3(pos.st, texture(depthBuffer, pos.st).r);
		
		spos = nvec3(projectionMatrixInv * nvec4(spos * 2.0 - 1.0));

		float err = distance(fragpos.xyz, spos.xyz);
		
		if(err < pow(sqrt(dot(vector,vector))*pow(sqrt(dot(vector,vector)),0.11),1.1) * 1.1){

			sr++;
			
			if(sr >= maxf){
				color.a = 1.0;
				break;
			}

			tvector -=vector;
			vector *=ref;

		}
		vector *= inc;
		oldpos = fragpos;
		tvector += vector;
		fragpos = start + tvector;
	}

	border = clamp(1.0 - pow(cdist(pos.st), 10.0), 0.0, 1.0);

	color.a = texture(colorBuffer, pos.st).a * color.a;
	color.rgb = texture(colorBuffer, pos.st).rgb;

	return color;
}

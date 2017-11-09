float bayer2(vec2 a){
    a = floor(a);
    return fract(dot(a,vec2(.5, a.y*.75)));
}

float bayer4(vec2 a)   {return bayer2( .5*a)   * .25     + bayer2(a); }
float bayer8(vec2 a)   {return bayer4( .5*a)   * .25     + bayer2(a); }
float bayer16(vec2 a)  {return bayer4( .25*a)  * .0625   + bayer4(a); }

vec4 ComputeVolumetricLight(vec4 worldSpacePosition, vec3 lightVec, vec3 eyeDirection){
	const int steps = 16;
	const float oneOverSteps = 1.0 / float(steps);

	vec3 startRay = (shadowMatrix * (untranslatedMVInv * vec4(0.0))).rgb;
	vec3 endRay = (shadowMatrix * (untranslatedMVInv * worldSpacePosition)).rgb;

	vec3 increment = normalize(startRay - endRay) * distance(startRay, endRay) * oneOverSteps;
	vec3 rayPosition = increment * bayer16(gl_FragCoord.xy) + endRay;

	float weight = sqrt(dot(increment, increment));

	float ray = 0.0;

	for (int i = 0; i < steps; i++){
		vec3 shadowCoord = accuratizeShadow(vec4(rayPosition, 0.0)).rgb + vec3(0.0, 0.0, 0.0001);

		ray += texture(shadowMap, shadowCoord, 0.0) * weight;

		rayPosition += increment;
	}
	
	float lDotU = dot(normalize(lightVec), vec3(0.0, 1.0, 0.0));
	float lDotV = 1.0 - dot(normalize(lightVec), normalize(eyeDirection));
	
	vec3 sunLight_g = mix(getSkyAbsorption(skyColor, zenithDensity(lDotU + multiScatterPhase)), vec3(0.0), overcastFactor);//pow(sunColor, vec3(gamma));
	float sunlightAmount = ray * shadowVisiblity * (1.0 / (lDotV));

	return vec4(jodieReinhardTonemap(clamp(sunLight_g * sunlightAmount, 0.0, 4096) * oneOverSteps), 0.0);
}

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

<include ../lib/noise2D.glsl>

/*

	Non physical based atmospheric scattering made by robobo1221
	Site: http://www.robobo1221.net/shaders
	Shadertoy: http://www.shadertoy.com/user/robobo1221

*/

const float pi = 3.14159265359;

const float invPi = 1.0 / pi;

const float zenithOffset = -0.1;
const float multiScatterPhase = 0.1;
const float density = 0.7;

const float anisotropicIntensity = 0.0; //Higher numbers result in more anisotropic scattering

const vec3 skyColor = vec3(0.37, 0.55, 1.0) * (1.0 + anisotropicIntensity); //Make sure one of the conponents is never 0.0

const vec3 upVec = vec3(0.0, 1.0, 0.0);

const float TAU = pi * 2;

#define smooth(x) x*x*(3.0-2.0*x)
#define zenithDensity(x) density / max(x * 2.0 - zenithOffset, 0.35e-2)

vec3 getSkyAbsorption(vec3 x, float y){
	
	vec3 absorption = x * y;
	     absorption = pow(absorption, 1.0 - (y + absorption) * 0.5) / x / y;
	
	return absorption;
}

float getSunPoint(float angle){
	return smoothstep(0.001, 0.0005, angle) * 50.0;
}

float getRayleigMultiplier(float angle){
	return 0.45 + pow(1.0 - angle, 2.0) * 0.2;
}

float getMie(float angle){
	float disk = clamp(1.0 - pow(angle, 0.02), 0.0, 1.0);
		  disk *= disk;
	
	return smooth(disk) * pi * 262.0;
}

vec3 jodieReinhardTonemap(vec3 c){
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 tc = c / (c + 1.0);

    return mix(c / (l + 1.0), tc, tc);
}

const float skyExp = 4.0;

vec3 getAtmosphericScattering(vec3 fPos, vec3 lightVec, vec3 upVec){
	vec3 viewVec = normalize(fPos);
	
	float sunVisibility = clamp(1.0 - overcastFactor * 2.0, 0.0, 1.0);
	float storminess = clamp(-1.0 + overcastFactor * 2.0, 0.0, 1.0);

	//Make sure all of the other vectors are in viewspace and normalized.

	float vDotL = 1.0 - dot(viewVec, normalize(lightVec));
	float vDotU = dot(viewVec, upVec);
	float lDotU = dot(normalize(lightVec), upVec);
		
	float zenith = zenithDensity(vDotU);
	float sunPointDistMult =  clamp(length(max(lDotU + multiScatterPhase - zenithOffset, 0.0)), 0.0, 1.0);
	
	float rayleighMult = getRayleigMultiplier(vDotL);
	
	vec3 absorption = getSkyAbsorption(skyColor, zenith);
    vec3 sunAbsorption = getSkyAbsorption(skyColor, zenithDensity(lDotU + multiScatterPhase));
	vec3 sky = skyColor * zenith * rayleighMult;
	vec3 sun = getSunPoint(vDotL) * absorption;
	vec3 mie = getMie(vDotL) * sunAbsorption;
	
	vec3 totalSky = mix(sky * absorption, sky / (sky + 0.5), sunPointDistMult);
         totalSky += sunVisibility * mie;
	     totalSky *= sunAbsorption * 0.5 + 0.5 * length(sunAbsorption);
		 totalSky *= pi;
		 totalSky = jodieReinhardTonemap(totalSky);
		 totalSky += 10.0 * sunVisibility * sun * pi;
		 totalSky = pow(totalSky, vec3(0.25 + 2.0 * (1.0 - overcastFactor)));
         //totalSky = pow(totalSky, vec3(2.2)); //Back to linear
	
	//return vec3(0.2);
	return clamp(totalSky * (0.1 + 0.4 * sunVisibility + 0.5 * (1.0 - storminess)), 0.0, 1000.0);//;
}

//Internal method to obtain pre-mixed sun gradient color
vec4 getSkyTexture(vec2 coordinates)
{
	float greyFactor = clamp((overcastFactor - 0.2) / 0.5, 0.0, 1.0);
	float darkFactor = clamp((overcastFactor - 0.5), 0.0, 1.0);
	return mix(texture(skyTextureSunny, coordinates), mix(texture(skyTextureRaining, coordinates), vec4(0.0), darkFactor), greyFactor);
}

//Returns the sky color depending on direction and time
//Requires sunPos, skyTextureSunny, skyTextureRaining, sunSetRiseTexture, overcastFactor
vec3 getSkyColor(float time, vec3 eyeDirection)
{
	/*float sunEyeDot = clamp(dot(normalize(eyeDirection), normalize(sunPos)), 0.0, 1.0);

	vec4 skyGlow = texture(sunSetRiseTexture, vec2(time, clamp(0.5 - pow(sunEyeDot, 2.0) * 0.5, 0.0, 1.0)));
	vec3 skyColor = vec3(0.0);
	
	//We compute the gradient ourselves to avoid color banding
	vec3 skyColorTop = getSkyTexture(vec2(time, 0.0)).rgb;
	vec3 skyColorBot = getSkyTexture(vec2(time, 1.0)).rgb;
	float gradient = clamp(normalize(eyeDirection).y, 0.0, 1.0);
	skyColor = mix(skyColorBot, skyColorTop, gradient);
	
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	//We add in the sun
	skyColor += clamp(1.0-overcastFactor * 2.0, 0.0, 1.0)*max(vec3(5.0)*pow(clamp(sunEyeDot, 0.0, 1.0), 750.0), 0.0);
	
	return pow(skyColor, vec3(gamma));*/
	return getAtmosphericScattering(eyeDirection, sunPos, upVec);
}

/*vec3 getSkyColorDiffuse(float time, vec3 eyeDirection)
{
	float sunEyeDot = 1.0 + dot(normalize(eyeDirection), normalize(sunPos));

	vec4 skyGlow = texture(sunSetRiseTexture, vec2(time, clamp(0.5 - sunEyeDot * 0.5, 0.0, 1.0)));
	vec3 skyColor = vec3(0.0);
	
	//We compute the gradient ourselves to avoid color banding
	vec3 skyColorTop = getSkyTexture(vec2(time, 0.0)).rgb;
	vec3 skyColorBot = getSkyTexture(vec2(time, 1.0)).rgb;
	float gradient = clamp(normalize(eyeDirection).y, 0.0, 1.0);
	skyColor = mix(skyColorBot, skyColorTop, gradient);
	
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	//We add in the sun
	skyColor += clamp(1.0-overcastFactor * 2.0, 0.0, 1.0)*max(vec3(5.0)*pow(clamp(sunEyeDot, 0.0, 1.0), 750.0), 0.0);
	
	return pow(skyColor, vec3(gamma));
}*/
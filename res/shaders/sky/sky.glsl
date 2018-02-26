
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

<include ../lib/noise2D.glsl>

/*

	Non physical based atmospheric scattering made by robobo1221
	Site: http://www.robobo1221.net/shaders
	Shadertoy: http://www.shadertoy.com/user/robobo1221

*/

const vec3 upVec = vec3(0.0, 1.0, 0.0);
#define PI 3.14159265359
#define pi PI

#define aWeather	overcastFactor*overcastFactor			//0 is clear 1 is rainy

#define rCoeff vec3(0.3,0.5,0.9)	//Rayleigh coefficient //You can edit this to your liking
#define mCoeff mix(0.1, 2.5, aWeather)	//Mie coefficient //You can edit this to your liking
#define mieSize mix(0.05, 1.0, aWeather)	//Mie Multiscatter Radius //You can edit this to your liking
#define eR 800.0			//Earth radius (not particulary accurate) //You can edit this to your liking
#define aR 0.25				//Atmosphere radius (also not accurate) //You can edit this to your liking
#define scatterBrightness 1.0	//Brightness of the sky //You can edit this to your liking
#define sunBrightness 50.0; //Brightness of the sunspot //You can edit this to your liking

#define aRef(x,x2,y)(x*y+x2*y)		//Reflects incomming light
#define aAbs(x,x2,y)exp2(-aRef(x,x2,y))	//Absorbs incomming light
#define d0Fix(x)abs(x+1.0e-32)		//Fixes devide by zero infinites
#define sA(x,y,z,w)d0Fix(x-y)/d0Fix(z-w)	//Absorbs scattered light
#define aScatter(x,y,z,w,s)sA(x,y,z,w)*s //Scatters reflected light

float gDepth(float x){const float d=eR+aR,eR2=eR*eR;float b=x*eR ;return sqrt(d*d+b*b-eR2)+b;}	//Calculates the distance between the camera and the edge of the atmosphere
float rPhase(float x){return 0.375*(x*x+1.0);}								//Rayleigh phase function
float gPhase(float x,float g){float g2 = g*g;return (1.0/4.0*PI)*((1.0-g2)/pow(1.0+g2-2.0*g*x,1.5));}	//Henyey greenstein phase function
float mPhase(float x,float d){return gPhase(x,exp2(d*-mieSize));}						//Mie phase function

float calcSunSpot(float x){const float sunSize = 0.9997; return smoothstep(sunSize, sunSize+0.00001,x);}	//Calculates sunspot

float lDotU = dot(normalize(sunPos), vec3(0.0, -1.0, 0.0)); //float lDotV = dot(l, v);
float opticalSunDepth = gDepth(lDotU);	//Get depth from lightpoint
vec3 sunAbsorb    = aAbs(rCoeff, mCoeff, opticalSunDepth);
vec3 sunLightColor = sunAbsorb;

vec3 getAtmosphericScatteringAmbient(){
	float uDotV = -1.0; //float lDotV = dot(l, v);
	
	float opticalDepth    = gDepth(uDotV);	//Get depth from viewpoint
	
	float phaseRayleigh = rPhase(lDotU);		//Rayleigh Phase
	float phaseMie = mPhase(lDotU, opticalDepth);	//Mie Phase
	
	vec3 viewAbsorb   = aAbs(rCoeff, mCoeff, opticalDepth);
	vec3 sunCoeff     = aRef(rCoeff, mCoeff, opticalSunDepth);
	vec3 viewCoeff    = aRef(rCoeff, mCoeff, opticalDepth);
	vec3 viewScatter  = aRef(rCoeff * phaseRayleigh, mCoeff * phaseMie, opticalDepth);
	
	vec3 finalScatter = aScatter(sunAbsorb, viewAbsorb, sunCoeff, viewCoeff, viewScatter); //Scatters all sunlight
	vec3 result = (finalScatter * PI) * (2.0 * scatterBrightness);
	
	return result;
}

vec3 getAtmosphericScattering(vec3 v, vec3 sunVec, vec3 upVec){ //vec3 v, vec3 lp
	sunVec = normalize(sunVec);
	v = normalize(v);
	upVec = normalize(upVec);
	
	float lDotV = dot(sunVec, v); //float lDotV = dot(l, v);
	float uDotV = dot(upVec, -v); //float lDotV = dot(l, v);
	
	float opticalDepth    = gDepth(uDotV);	//Get depth from viewpoint
	
	float phaseRayleigh = rPhase(lDotV);		//Rayleigh Phase
	float phaseMie = mPhase(lDotV, opticalDepth);	//Mie Phase
	
	vec3 viewAbsorb   = aAbs(rCoeff, mCoeff, opticalDepth);
	vec3 sunCoeff     = aRef(rCoeff, mCoeff, opticalSunDepth);
	vec3 viewCoeff    = aRef(rCoeff, mCoeff, opticalDepth);
	vec3 viewScatter  = aRef(rCoeff * phaseRayleigh, mCoeff * phaseMie, opticalDepth);
	
	vec3 finalScatter = aScatter(sunAbsorb, viewAbsorb, sunCoeff, viewCoeff, viewScatter); //Scatters all sunlight
	vec3 sunSpot = (calcSunSpot(lDotV) * viewAbsorb) * sunBrightness; //Sunspot
	
	vec3 result = (finalScatter + sunSpot) * PI * (2.0 * scatterBrightness);
	
	return result;
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
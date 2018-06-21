#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in float alphaPassed;
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform vec3 camUp;
uniform float overcastFactor;

//World
uniform float time;

//Gamma constants
#include ../lib/gamma.glsl

//Sky functions
#include ../sky/sky.glsl
#include ../sky/fog.glsl

vec3 calculateCloudScattering(vec3 backColor, float density, vec3 v, vec3 l){
	float vDotL = dot(v, l);
	
	const float mieCoeff = 0.1;
	
	float opticalDepth = density * mix(16.0, 4.0, pow(overcastFactor, 0.25));
	float transMittance = exp2(-opticalDepth * mieCoeff);
	vec3 sunScattering = (gPhase(vDotL, 0.9) + 0.5) * mieCoeff * opticalDepth * sunLightColor * transMittance;
	vec3 skyScattering = backColor * mieCoeff * opticalDepth * transMittance / pi;
	vec3 scattering = sunScattering + skyScattering;
	
	return backColor * transMittance + scattering * pi;
}

void main()
{
	vec3 skyColor = getSkyColor(time, eyeDirection);
	vec3 fogColor = getFogColor(time, 2000 * normalize(vec3(eyeDirection.x  + 1.0, 0.0, eyeDirection.z))).rgb;
	
	float belowHorizon = clamp(-300 * eyeDirection.y, 0.0, 1.0);
	float weatherMist = overcastFactor * clamp(1.0 - abs(normalize(eyeDirection).y) * 1.0, 0.0, 1.0);
	
	vec4 color = vec4(mix(skyColor, fogColor, clamp(belowHorizon + weatherMist, 0.0, 1.0)), 1.0);
	
	vec3 cloudsColor = calculateCloudScattering(color.rgb, alphaPassed, normalize(eyeDirection), normalize(sunPos));
	
	color.rgb = cloudsColor;
	
	shadedFramebufferOut = color;
}
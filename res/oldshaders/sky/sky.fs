#version 330

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform sampler2D sunSetRiseTexture;

uniform samplerCube envmap;
uniform float overcastFactor;
uniform vec3 sunPos;
uniform vec3 camUp;

//World
uniform float time;

//Gamma constants
#include ../lib/gamma.glsl

//Sky functions
#include ../sky/sky.glsl
#include ../sky/fog.glsl

void main()
{
	//Straight output of library's method
	vec3 skyColor = getSkyColor(time, eyeDirection);
	vec3 fogColor = getFogColor(time, 2000 * normalize(vec3(eyeDirection.x  + 1.0, 0.0, eyeDirection.z))).rgb;
	
	float belowHorizon = clamp(-300 * eyeDirection.y, 0.0, 1.0);
	float weatherMist = overcastFactor * clamp(1.0 - abs(normalize(eyeDirection).y) * 1.0, 0.0, 1.0);
	
	shadedFramebufferOut = vec4(mix(skyColor, fogColor, clamp(belowHorizon + weatherMist, 0.0, 1.0)), 1.0);
	
}

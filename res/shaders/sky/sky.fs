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
	shadedFramebufferOut = vec4(mix(getSkyColor(time, eyeDirection), getFogColor(time, 2000 * normalize(vec3(eyeDirection.x  + 1.0, 0.0, eyeDirection.z))).rgb, clamp(-300 * eyeDirection.y, 0.0, 1.0)), 1.0);
	//shadedFramebufferOut = vec4(pow(texture(envmap, eyeDirection).rgb, vec3(2.1)), 1.0);
}

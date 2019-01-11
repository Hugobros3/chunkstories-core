#version 450

//Passed variables
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 colorOut;

#include struct <xyz.chunkstories.api.graphics.structs.WorldConditions>
uniform WorldConditions world;

//Common camera matrices & uniforms
#include struct <xyz.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

//Gamma constants
//#include ../lib/gamma.glsl

//Sky functions

#include ../sky/sky.glsl
//#include ../sky/fog.glsl

void main()
{
	//Straight output of library's method
	vec3 skyColor = getSkyColor(world.time, eyeDirection);
	/*vec3 fogColor = getFogColor(time, 2000 * normalize(vec3(eyeDirection.x  + 1.0, 0.0, eyeDirection.z))).rgb;
	
	float belowHorizon = clamp(-300 * eyeDirection.y, 0.0, 1.0);
	float weatherMist = overcastFactor * clamp(1.0 - abs(normalize(eyeDirection).y) * 1.0, 0.0, 1.0);
	
	colorOut = vec4(mix(skyColor, fogColor, clamp(belowHorizon + weatherMist, 0.0, 1.0)), 1.0);*/
	colorOut = vec4(skyColor, 1.0);
}

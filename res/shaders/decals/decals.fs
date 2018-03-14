#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec2 texCoordPassed;
in vec3 eyeDirection;

//Framebuffer outputs
// out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D diffuseTexture;
uniform sampler2D zBuffer;

uniform vec2 screenViewportSize;

//World
uniform float time;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

//Gamma constants
#include ../lib/gamma.glsl
#include ../lib/normalmapping.glsl

out vec4 outDiffuseColor;

void main()
{
	vec4 color = texture(diffuseTexture, texCoordPassed);
	
	//float depth = texture(zBuffer, gl_FragCoord.xy / screenViewportSize ).x;
	
	vec3 normal = vec3(0.0, 1.0, 0.0);
	
	if(color.a == 0.0)
		discard;
	
	//color.rgb = vec3(texCoordPassed, 0.0);
	
	float dynamicFresnelTerm = 0.0 + 1.0 * clamp(0.7 + dot(normalize(eyeDirection), vec3(normal)), 0.0, 1.0);
	
	//We just want to tint the target diffuse color
	outDiffuseColor = vec4(color.rgb, color.a);
}
#version 330
// Copyright 2015 XolioWare Interactive

in vec4 interpolatedColor;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;
uniform vec3 camUp;

out vec4 fragColor;
uniform float sunTime;


uniform vec3 sunPos;
uniform float dayTime;
uniform float overcastFactor;
#include ../sky/sky.glsl
#include ../sky/fog.glsl

uniform sampler2D sunlightCycle;

const float gamma = 2.2;
const float gammaInv = 1/2.2;
vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
}

void main()
{
	//Diffuse G-Buffer
	fragColor = interpolatedColor;
	fragColor.rgb *= getSkyColor(dayTime, vec3(0.0, 1.0, 0.0));
	fragColor.a = 0.5;
}
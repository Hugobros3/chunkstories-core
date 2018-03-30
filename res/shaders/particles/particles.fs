#version 330
// Copyright 2015 XolioWare Interactive

//General data
in vec4 texcoord; // Coordinate
in vec3 eye; // eye-position

//Diffuse colors
uniform sampler2D diffuseTexture; // diffuse texture
uniform sampler2D normalTexture; // diffuse texture

uniform sampler2D normalMap; // Blocks normal texture atlas

//Block and sun Lightning
in vec2 lightMapCoords; //Computed in vertex shader
uniform float sunIntensity; // Adjusts the lightmap coordinates
uniform sampler2D lightColors; // Sampler to lightmap

//Normal mapping
in vec3 inNormal;
in vec4 inVertex;

uniform sampler2D diffuseGBuffer;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

in float back;

out vec4 outDiffuseColor;
out vec3 outNormal;
out vec2 outVoxelLight;
out float outSpecularity;
out uint outMaterial;

#include ../lib/normalmapping.glsl

void main(){
	//Basic texture color
	vec3 baseColor = texture(diffuseTexture, texcoord.st).rgb;
	
	//light coloring
	vec3 finalLight = texture(lightColors,lightMapCoords).rgb;
	
	//Texture transparency
	float alpha = texture(diffuseTexture, texcoord.st).a;
	
	//Rain makes shit glint
	float specular = 0.0;
	//specular+=1;
	//Shadow
	
	vec4 source = texture(diffuseGBuffer, gl_FragCoord.xy);
	
	if(alpha < 1.0)
		discard;
	
	vec3 normalMapped = texture(normalTexture, texcoord.st).xyz;
    normalMapped = normalMapped * 2.0 - 1.0;
	normalMapped.x = normalMapped.x;
	
	outDiffuseColor = vec4(baseColor,alpha);
	outNormal = encodeNormal(normalMapped);
	outVoxelLight = lightMapCoords.xy;
	outSpecularity = 0.0;
	outMaterial = 255u;
}

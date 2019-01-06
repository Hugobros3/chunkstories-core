#version 330
/*
	Deffered shading (cone)light shader
	Something I wrote ages ago and that is probably terribly optimised
*/

//G-buffer samplers
uniform sampler2D albedoBuffer;
uniform sampler2D zBuffer;
uniform sampler2D normalBuffer;

//Pixel texture position
in vec2 screenCoord;

//Common camera matrices & uniforms
uniform mat4 modelViewMatrixInv;
uniform mat4 modelViewMatrix;
uniform mat4 projectionMatrixInv;
uniform mat4 projectionMatrix;
uniform mat3 normalMatrixInv;
uniform mat3 normalMatrix;
uniform vec3 camPos;

//Point lights
uniform float lightDecay[64];
uniform vec3 lightPos[64];
uniform vec3 lightColor[64];

//Cone lights
uniform vec3 lightDir[64];
uniform float lightAngle[64];

uniform int lightsToRender;

out vec4 fragColor;

//Gamma constants
#include ../lib/gamma.glsl
#include ../lib/transformations.glsl
#include ../lib/normalmapping.glsl

void main() {
	//Accumulator buffer
	vec4 totalLight = vec4(0.0);
	
	//Get normal from g-buffer
	vec3 normal = decodeNormal(texture(normalBuffer, screenCoord));
	vec3 normalWorld = normalize(normalMatrixInv * normal);
	
	//Get reflectivity of surface
	float spec = texture(normalBuffer, screenCoord).z;
	
	vec3 pixelPositionCamera = convertScreenSpaceToCameraSpace(screenCoord, zBuffer).xyz;
	
	//Discard if too far from camera
	//if(length(pixelPositionCamera) > 512.0)
	//	discard;
		
	vec3 pixelPositionWorld = ( modelViewMatrixInv * vec4(pixelPositionCamera, 1.0) ).xyz;
	
	//We batch multiple lights per fullscreen pass ( another strategy: cull lights more appropriately to save fillrate )
	for(int i = 0; i < lightsToRender; i++)
	{
		vec3 lightPositionWorld = lightPos[i];
		float distance = length(pixelPositionWorld-lightPositionWorld);
		
		vec4 lightAmount = vec4(pow(lightColor[i], vec3(gamma)), 1.0);
		
		float squareme = distance;// * lightDecay[i];
		lightAmount /= squareme * squareme;
		
		vec3 lightRay = normalize((vec4(lightPositionWorld-pixelPositionWorld, 1.0)).xyz);
		
		//Normal influence
		float dotL = clamp(dot(normalWorld, lightRay), 0.0, 1.0);
		lightAmount.rgb *= dotL;
		
		//Add specular term if light should be reflected by surface
		//Optional : cone light, view direction influence
		if(lightAngle[i] > 0.0)
		{
			float dotCone = dot(-1.0 * lightRay, lightDir[i]);
			float cosAngle = cos(lightAngle[i]);
			
			lightAmount.rgb *= clamp(3.0*(dotCone-cosAngle), 0.0, 1.0);
		}
		if(spec > 0.0)
		{
			//Additional speculars reflecting off materials ( computationally heavy-ass and require FS pass )
			//lightAmount.rgb += pow(lightColor[i], vec3(gamma)) * 20 * clamp(pow(clamp(dot(reflect(lightRay, normalWorld), normalize(pixelPositionWorld-camPos)), 0.0, 10.0), 1000), 0.0, 10.0);
		}
		totalLight += max(lightAmount, 0.0);
	}
	fragColor = totalLight * textureGammaIn(albedoBuffer, screenCoord);
}

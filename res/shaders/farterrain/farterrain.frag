#version 450

in vec3 position;
//in vec2 texCoord;
//in vec3 eyeDirection;
in vec3 barycentric;
in vec3 normalOut;
flat in int column;
flat in int row;

out vec4 colorBuffer;
out vec4 normalBuffer;

//#include struct xyz.chunkstories.api.graphics.structs.Camera
//uniform Camera camera;

//#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
//uniform WorldConditions world;

//#include ../sky/sky.glsl
#include ../normalcompression.glsl

//#material sampler2D albedoTexture;
//#material sampler2D normalTexture;
uniform sampler2D terrainColor;

float edgeFactor(){
    vec3 d = fwidth(barycentric);
    vec3 a3 = smoothstep(vec3(0.0), d*0.5, barycentric);
    return min(min(a3.x, a3.y), a3.z);
}

void main()
{
    //vec4 albedo = vec4(vec3(float((column + row) % 2) * 1.0), 1.0);
    //vec4 albedo = vec4(vec3(edgeFactor()), 1.0);
    vec4 albedo = vec4(vec3(1.0), 1.0);
	albedo = texture(terrainColor, mod(vec2(position.x, position.z), vec2(4096.0)) / vec2(4096.0));
	//albedo.a = 1.0;

	if(albedo.a < 1.0) {
		float temperature = 0.5;//temperatureAt(vertex.xz);

		//albedo.rgb *= vec3(0.4, 0.8, 0.4);
		//vec3 grassColor = mix(vec3(0.0, 0.0, 1.0), vec3(1.0, 0.0, 0.0), 0.5 + 0.5 * temperature);
		vec3 grassColor = mix(vec3(0.4, 0.8, 0.4), vec3(0.5, 0.85, 0.25), 0.5 + 0.5 * temperature);
		albedo.rgb *= grassColor;
		albedo.a = 1.0;
	}

	/*if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		//albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}*/

	colorBuffer = vec4(albedo.rgb, 1.0);
	normalBuffer = vec4(encodeNormal(normalize(normalOut)), 1.0, 0.0);
}
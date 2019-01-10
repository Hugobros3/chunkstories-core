#version 450

in vec2 vertexPos;

out vec4 colorOut;

uniform sampler2D colorBuffer;
uniform sampler2D normalBuffer;

#include struct <xyz.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

#include struct <xyz.chunkstories.api.graphics.structs.WorldConditions>
uniform WorldConditions world;

void main()
{
	vec4 albedo = texture(colorBuffer, vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5));
	vec3 normal = texture(normalBuffer, vec2(vertexPos.x * 0.5 + 0.5, 0.5 + vertexPos.y * 0.5)).xyz * 2.0 - vec3(1.0);

	if(albedo.a == 0) {
		discard;
	}

	vec2 color = vec2(1.0, 0.0);

	float NdL = clamp(dot(world.sunPosition, normal.xyz), 0.0, 1.0);
	
	vec3 shadowLight = vec3(52.0 / 255.0, 68.0 / 255.0, 84.0 / 255.0);
	vec3 sunLight = vec3(1.0) - shadowLight;

	vec3 lightColor = (NdL * sunLight + shadowLight) * color.x;
	lightColor += vec3(1.0) * pow(color.y, 2.0);

	vec3 litSurface = albedo.rgb * lightColor;
	//litSurface = camera.lookingAt.rgb;
	//litSurface = normal.rgb;
	//vec3 E = camera.lookingAt.xyz;
	//E = normalize(vertex - camera.position);
	//vec3 R = reflect(E, normal);
	//litSurface = clamp(dot(R, world.sunPosition), 0.0, 1.0) * vec3(1.0);
	//litSurface = R;
	//litSurface = getSkyColor(0.5, E);

	vec3 fog = vec3(0.0, 0.5, 1.0);
	
	float fogStrength = 1.0;

	colorOut = vec4(mix(fog, litSurface, fogStrength), albedo.a);
}
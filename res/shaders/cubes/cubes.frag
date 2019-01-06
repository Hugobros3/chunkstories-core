#version 450

layout(set=0, location=0) uniform sampler2D virtualTextures[1024];

in vec4 color;
in vec3 normal;
in vec2 texCoord;
flat in int textureId;

in float fogStrength;

out vec4 colorOut;
out vec4 normalOut;

void main()
{
	// Hardcoded sun position for now
	vec3 sunPos = vec3(1.0, 1.5, 0.3);
	sunPos = normalize(sunPos);

	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	vec4 albedo = texture(virtualTextures[textureId], texCoord);
	//vec4 albedo = vec4(1.0, 0.2, 0.2, 1.0);

	// Unused code ( can reproduce the crash without it )
	float NdL = clamp(dot(sunPos, normal.xyz), 0.0, 1.0);

	if(albedo.a == 0.0) {
		discard;
	}

	if(albedo.a < 1.0) {
		albedo.rgb *= vec3(0.2, 1.0, 0.5);
		albedo.a = 1.0;
	}
	
	vec3 shadowLight = vec3(52.0 / 255.0, 68.0 / 255.0, 84.0 / 255.0);
	vec3 sunLight = vec3(1.0) - shadowLight;

	vec3 litSurface = (albedo.rgb * NdL * sunLight + albedo.rgb * shadowLight) * color.x;

	litSurface += vec3(1.0) * pow(color.y, 2.0);

	vec3 fog = vec3(0.0, 0.5, 1.0);
	
	colorOut = vec4(mix(fog, litSurface, fogStrength), albedo.a);
	normalOut = vec4(normal * 0.5 + vec3(0.5), 1.0);
}
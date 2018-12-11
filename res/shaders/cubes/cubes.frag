#version 450
/*
	AMD potential bug report: This is the shader that can cause the crash.
	I made it very simple to demonstrate the problem is likely due to a driver bug
	rather than just complex shaders timing out.
*/

layout(set=0, location=0) uniform sampler2D virtualTextures[1024];

// Don't worry about the lack of layout annotations, I preprocess the shader with spirv-cross
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

	// Compute some crappy-ass lighting
	// Commenting this out will also solve the crash on it's own, which is very strange
	// considering I don't use the computed value! 
	float NdL = clamp(dot(sunPos, normal.xyz), 0.0, 1.0);

	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	vec4 albedo = texture(virtualTextures[textureId], texCoord);
	
	// Unused code ( can reproduce the crash without it )
	//vec3 litSurface = albedo.rgb * NdL * 0.5 + albedo.rgb * 0.5;
	//vec3 fog = vec3(0.0, 0.5, 1.0);

	// If you comment-out this statement, you can keep the dot product below and it works fine
	if(albedo.a == 0.0) {
		discard;
	}

	// If you comment-out this statement and output a vec4(1), you can keep the discard case above and it works fine
	colorOut = vec4(vec3(dot(sunPos, normal.xyz)), 1.0);
	//colorOut = vec4(1.0);

	// disabled for minimal test case
	//colorOut = vec4(litSurface, albedo.a);
	//colorOut = vec4(mix(fog, litSurface, fogStrength), albedo.a);
	normalOut = vec4(normal * 0.5 + vec3(0.5), 1.0);
}
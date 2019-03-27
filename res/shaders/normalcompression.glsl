vec3 decodeNormal(vec2 compressed) {
	float scale = 1.7777;
	
	vec3 nn = vec3(compressed.xy, 0.0) * vec3(2.0 * scale, 2 * scale, 0.0) + vec3(-scale, -scale, 1.0);
	float g = 2.0 / dot(nn.xyz, nn.xyz);
	vec3 n = vec3(g * nn.xy, g - 1.0);
	
	return n;
}

vec2 encodeNormal(vec3 uncompressed) {
	float scale = 1.7777;
	vec2 enc = uncompressed.xy / (uncompressed.z + 1.0);
	enc /= scale;
	enc = enc * 0.5 + vec2(0.5);
	
	return enc;
}
/*vec3 decodeNormal(vec2 compressed) {
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
}*/


vec2 encodeNormal(vec3 n)
{
    float f = sqrt(8.0*n.z+8.0);
    return n.xy / f + 0.5;
}

vec3 decodeNormal(vec2 enc)
{
    vec2 fenc = enc*4.0-vec2(2.0);
    float f = dot(fenc,fenc);
    float g = sqrt(1.0-f/4.0);
    vec3 n;
    n.xy = fenc*g;
    n.z = 1.0-f/2.0;
    return n;
}

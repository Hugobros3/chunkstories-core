vec3 deep(vec2 coords)
{
	return texture(waterNormalDeep, coords).rgb * 2.0 - vec3(1.0);
}

vec3 shallow(vec2 coords)
{
	return texture(waterNormalShallow, coords).rgb * 2.0 - vec3(1.0);
}

vec2 dir(float angle) {
	return vec2(sin(angle), cos(angle));
}

vec2 wave(vec2 point) {
	return vec2(sin(point.x), cos(point.y));
}

vec3 water() {
	vec3 normalMap = 0.05 * vec3(0.0, 0.0, 1.0);
	normalMap += 0.25 * deep((vertexPassed.xz/5.0+vec2(0.0,animationTimer * 0.02))/15.0);
	
	normalMap += 0.25 * mixedTextures((vertexPassed.zx*2.0-vec2(400.0, 45.0 * 0.05+animationTimer/25.0)/350.0)/10.0);
	
	normalMap += 1.0 * mixedTextures((vertexPassed.zx*0.8+vec2(400.0, sin(-animationTimer/5.0)+animationTimer * 2.0)/350.0)/10.0);
	
	normalMap = normalize(normalMap);
	
	if(normalMap.z <= 0.5)
		normalMap = vec3(0.0);
	
	return normalMap;
}
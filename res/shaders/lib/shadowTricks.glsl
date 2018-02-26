//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io
const float distScale = 0.8;

float getDistordFactor(vec2 worldposition){
	vec2 pos1 = abs(worldposition * 1.2);

	float dist = pow(pow(pos1.x, 8.0) + pow(pos1.y, 8.0), 0.125);
	return mix(1.0, dist, distScale);
}

//Transform coordinates to skew buffer while reading
vec4 accuratizeShadow(vec4 shadowMap)
{
	shadowMap.xy /= getDistordFactor(shadowMap.xy);
	shadowMap.xyz = shadowMap.xyz * vec3(0.5,0.5,0.2) + vec3(0.5,0.5,0.5);

	return shadowMap;
}

//Transform coordinates to skew buffer while writing
vec4 accuratizeShadowIn(vec4 shadowMap)
{
	shadowMap.xy /= getDistordFactor(shadowMap.xy);
	shadowMap.z *= 0.4;
	
	return shadowMap;
}
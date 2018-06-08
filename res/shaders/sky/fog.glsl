//Returns the sky color without sun, depending on direction and time
//Used in fog
//Requires sunPos, skyTextureSunny, skyTextureRaining, sunSetRiseTexture, overcastFactor
vec4 getFogColor(float time, vec3 eyePosition)
{	
	float overCastRatio = clamp(aWeather * 2.0, 0.0, 1.0);
	
	float dist = clamp(length(eyePosition) - mix(64.0, 0.0, foggyness), 0.0, 4096.0) * mix(0.002, 0.015, foggyness);
	float fogIntensity = 1.0 - exp2(-dist);	//Proper realistic fog distribution
	
	//vec3 blended = mix(skyColor / TAU, totalSky, clamp(( -0 + length(eyePosition)) / 256.0, 0.0, 1.0));

	vec3 fogColor = getAtmosphericScatteringAmbient();// * mix(vec3(1.0), sunAbsorb * 0.9 + 0.1, overCastRatio);
	
	return vec4(fogColor, fogIntensity);
}

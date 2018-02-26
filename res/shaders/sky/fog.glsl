//Returns the sky color without sun, depending on direction and time
//Used in fog
//Requires sunPos, skyTextureSunny, skyTextureRaining, sunSetRiseTexture, overcastFactor
vec4 getFogColor(float time, vec3 eyePosition)
{	
	
	float dist = clamp(length(eyePosition), 0.0, 4096.0) * 0.002;
	float fogIntensity = 1.0 - exp2(-dist);	//Proper realistic fog distribution
	
	//vec3 blended = mix(skyColor / TAU, totalSky, clamp(( -0 + length(eyePosition)) / 256.0, 0.0, 1.0));

	vec3 fogColor = getAtmosphericScatteringAmbient(sunPos, vec3(0.0, 1.0 - overcastFactor * 0.9, 0.0));

	//blended = mix(backGroundColor, fogColor / TAU, fogIntensity / TAU);
	
	return vec4(fogColor, fogIntensity);	
	/*float sunEyeDot = clamp(dot(normalize(eyeDirection), normalize(sunPos)), 0.0, 1.0);
	
	vec4 skyGlow = texture(sunSetRiseTexture, vec2(time, clamp(0.5 - sunEyeDot * 0.5, 0.0, 1.0)));
	vec3 skyColor = getSkyTexture(vec2(time, clamp(1.0-normalize(eyeDirection).y, 0.0, 1.0))).rgb;
    
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	return pow(skyColor, vec3(gamma));*/
}

#version 450

in vec2 vertexPos;
out vec4 fragColor;

uniform sampler2D shadedBuffer;
uniform sampler2D bloomBuffer;

#include ../gamma.glsl

vec3 jodieReinhardTonemap(vec3 c){
    float l = dot(c, vec3(0.2126, 0.7152, 0.0722));
    vec3 tc = c / (c + 1.0);

    return mix(c / (l + 1.0), tc, tc);
}

vec3 screenSpaceDitherCrazy( vec2 vScreenPos )
{
	// Iestyn's RGB dither (7 asm instructions) from Portal 2 X360, slightly modified for VR
	//vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy + iGlobalTime ) );
    vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy ) );
    vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) );
    return vDither.rgb; //note: looks better without 0.375...

    //note: not sure why the 0.5-offset is there...
    //vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) ) - vec3( 0.5, 0.5, 0.5 );
	//return (vDither.rgb / 255.0) * 0.375;
}

vec3 screenSpaceDither( vec2 vScreenPos ) {
	// Iestyn's RGB dither (7 asm instructions) from Portal 2 X360, slightly modified for VR
	//vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy + iGlobalTime ) );
    vec3 vDither = vec3( dot( vec2( 171.0, 231.0 ), vScreenPos.xy ) );
    vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) );
    return vDither.rgb; //note: looks better without 0.375...

    //note: not sure why the 0.5-offset is there...
    //vDither.rgb = fract( vDither.rgb / vec3( 103.0, 71.0, 97.0 ) ) - vec3( 0.5, 0.5, 0.5 );
	//return (vDither.rgb / 255.0) * 0.375;
}

float luminance(vec3 color) {
	return color.r * 0.2125 + color.g * 0.7154 + color.b * 0.0721;
}

void main()
{
	vec2 texCoord = vec2(vertexPos.x * 0.5 + 0.5, 0.5 - vertexPos.y * 0.5);
	vec3 hdrColor = texture(shadedBuffer, texCoord).rgb;

	hdrColor += texture(bloomBuffer, texCoord).rgb * 0.05;

	vec3 tonemapped = jodieReinhardTonemap(hdrColor.rgb);
	vec3 gammaCorrected = pow(tonemapped, vec3(gammaInv));

	vec3 greyscale = vec3(luminance(gammaCorrected));

	float bitdepth = 255.0;

	vec3 dithering = screenSpaceDither( gl_FragCoord.xy / 1.0 ) - vec3(0.5);
	vec3 dithered = gammaCorrected + 1.0 * dithering.x / bitdepth;

	vec3 crunched = floor(dithered * bitdepth + 0.5 / bitdepth) / bitdepth;
    
	//looks fantastic
	//vec3 rnd2 = screenSpaceDitherCrazy( gl_FragCoord.xy / 1024.0 + vec2(length(greyscale.xyz) + vec2(0.0, 5.0)) );
	//vec3 rnd2 = screenSpaceDitherCrazy( gl_FragCoord.xy / 1024.0) );
    //vec3 ditheredColor = its2 + rnd2.xyz;
	fragColor = vec4(dithered, 1.0);
}
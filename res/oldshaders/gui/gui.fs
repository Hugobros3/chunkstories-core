#version 330
//(c) 2015 XolioWare Interactive

uniform sampler2D sampler;
uniform float useTexture;

struct passMe {
	vec2 texCoord;
	vec4 color;
};

in passMe passed;

out vec4 fragColor;

void main()
{
	if(useTexture > 0.5)
		fragColor = passed.color * texture(sampler, passed.texCoord);// + vec4(1.0, 0.0, 1.0, 0.5);
	else
		fragColor = passed.color;// + vec4(1.0, 0.0, 1.0, 0.5);
}
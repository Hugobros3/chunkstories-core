#version 450

in vec2 vertexPos;
out vec4 fragColor;

uniform sampler2D shadedBuffer;

void main()
{
	fragColor = vec4(texture(shadedBuffer, vec2(vertexPos.x * 0.5 + 0.5, 0.5 - vertexPos.y * 0.5)).rgb, 1.0);
}
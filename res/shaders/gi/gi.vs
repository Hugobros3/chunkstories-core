#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec2 vertexIn;

//Passed variables
out vec2 screenCoord;
out vec3 eyeDirection;

uniform mat4 projectionMatrixInv;
uniform mat4 untranslatedMVInv;

void main(void)
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	screenCoord = vertexIn.xy * 0.5 + vec2(0.5);
	
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = normalize(untranslatedMVInv * projectionMatrixInv * transformedSS ).xyz;
}
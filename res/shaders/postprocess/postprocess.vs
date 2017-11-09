#version 330
in vec2 vertexIn;

out vec2 texCoord;
out vec2 pauseOverlayCoords;


out vec3 eyeDirection;

uniform vec2 screenViewportSize;
 
const vec2 backgroundSize = vec2(1024);

uniform mat4 projectionMatrixInv;
uniform mat4 untranslatedMVInv;
	
void main(void) {
	gl_Position = vec4(vertexIn, 0.0, 1.0);

	texCoord = vertexIn.xy * 0.5 + vec2(0.5);
	pauseOverlayCoords = ( ( vertexIn.xy * 0.5 + vec2(0.5) ) * screenViewportSize ) / backgroundSize;
	
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	eyeDirection = normalize(untranslatedMVInv * projectionMatrixInv * transformedSS ).xyz;
}
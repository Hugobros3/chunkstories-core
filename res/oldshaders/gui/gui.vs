#version 330
in vec2 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;

struct passMe {
	vec2 texCoord;
	vec4 color;
};

//Simply transfer data
out passMe pass;

void main(void) {
	pass.texCoord = vec2(texCoordIn);
	pass.color = colorIn;
	gl_Position = vec4(vertexIn, 0.0, 1.0);
}
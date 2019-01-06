#version 330 core
layout (triangles) in;
layout (triangle_strip, max_vertices = 6) out;

struct passMe {
	vec2 texCoord;
	vec4 color;
};

in passMe pass[];
out passMe passed;

void main() {    
	for(int i = 0; i < 3; i++) {
		gl_Position = gl_in[i].gl_Position;
		
		passed.color = pass[i].color;
		passed.texCoord = pass[i].texCoord;
		
		EmitVertex();
	}
	EndPrimitive();
} 
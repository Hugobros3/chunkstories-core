#version 450

in vec3 vertex;
in vec4 color;

out vec4 shadedBuffer;

void main() {
    shadedBuffer = color;
}

#version 450

in vec3 vertexIn;
in vec4 colorIn;

out vec3 vertex;
out vec4 color;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

void main() {
    vec4 animatedVertex = vec4(vertexIn, 1.0);
    vec4 projected = camera.projectionMatrix * camera.viewMatrix * vec4(animatedVertex);

    vertex = animatedVertex.xyz;
    color = colorIn;

    gl_Position = projected;
}

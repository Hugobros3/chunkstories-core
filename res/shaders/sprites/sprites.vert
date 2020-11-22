#version 450

in vec2 vertexIn;

out vec3 position;
out vec2 texCoord;

out vec3 eyeDirection;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.representation.Sprite
instanced Sprite sprite;

void main()
{
    vec4 point = vec4(sprite.position, 1.0);

    vec4 viewSpace = camera.viewMatrix * point + vec4(vertexIn * 0.5, 0.0, 0.0) * sprite.size;
    vec4 projected = camera.projectionMatrix * viewSpace;

    position = point.xyz;
    texCoord = vertexIn * vec2(0.5, -0.5) + vec2(0.5);

    eyeDirection = normalize(camera.normalMatrixInverted * vec3(0.0, 1.0, 0.0));
    
    gl_Position = projected;
}

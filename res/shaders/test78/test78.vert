#version 330

#include struct xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize
uniform ViewportSize viewportSize;

in vec2 vertexIn;
out vec2 texCoord;

void main()
{
	vec2 pixelSize = vec2(1.0) / viewportSize.size;
	//vec2 pixelSize = vec2(1.0) / vec2(512.0);
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = (vertexIn.xy*0.5+0.5) * viewportSize.size;
}
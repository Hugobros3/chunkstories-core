#version 450

in vec3 vertexIn;
in vec3 colorIn;
in vec3 normalIn;
in vec2 texCoordIn;
in uint textureIdIn;

out vec3 vertex;
out vec4 color;
out vec3 normal;
out vec2 texCoord;
flat out int textureId;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.graphics.common.world.ChunkRenderInfo
//uniform ChunkRenderInfo chunkInfo;
instanced ChunkRenderInfo chunkInfo; // look mom, no uniforms !

#define PI 3.14159265359
#define pi PI
//#include struct xyz.chunkstories.api.math.random.PrecomputedSimplexSeed
//uniform PrecomputedSimplexSeed simplexSeed;

//#include simplex.glsl
//#include noise.glsl

void main()
{
	vec3 vertexPos = vertexIn.xyz;
	vertexPos += vec3(chunkInfo.chunkX, chunkInfo.chunkY, chunkInfo.chunkZ) * 32.0;

	/*vertexPos.xz -= camera.position.xz;
	vertexPos.x = abs(pow(vertexPos.x, 1.0)) * sign(vertexPos.x);
	vertexPos.z = abs(pow(vertexPos.z, 1.0)) * sign(vertexPos.z);
	vertexPos.xz += camera.position.xz;*/

	//if((mod(vertexPos.x , 32.0) < 16.0) == (mod(vertexPos.z , 32.0) < 16.0))
	//vertexPos.y = 1+(heightAt(vertexPos.xz));

	/*float dx = -(1+heightAt(vertexPos.xz + vec2(0.1, 0.0)) - vertexPos.y);
	float dz = -(1+heightAt(vertexPos.xz + vec2(0.0, 0.1)) - vertexPos.y);

	float r = sqrt(1.0 - dx * dx - dz * dz);
	vec3 nrml = vec3(dx, r, dz);*/

	//nrml = pow(nrml, vec3(0.8));

	//vec4 viewSpace = camera.viewMatrix * vec4(vertexPos, 1.0);
	//vec4 projected = camera.projectionMatrix * viewSpace;
	vec4 projected = camera.combinedViewProjectionMatrix * vec4(vertexPos, 1.0);


	vertex = vertexPos;

	color = vec4(colorIn.x, colorIn.y, colorIn.z, 1.0);
	//color = vec4(1.0);
	
	normal = camera.normalMatrix * normalIn;
	//normal = camera.normalMatrix * nrml;
	texCoord = texCoordIn;
	textureId = int(textureIdIn);

	gl_Position = projected;
}
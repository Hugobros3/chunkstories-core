#version 450

in vec3 vertexIn;
in vec3 normalIn;
in vec2 texCoordIn;

in ivec4 boneIdIn;
in vec4 boneWeightIn;

out vec3 vertex;
out vec3 normal;
out vec2 texCoord;

#ifdef ENABLE_ANIMATIONS
flat out ivec4 boneId;
out vec4 boneWeight;
#else
#endif

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.representation.ModelPosition
instanced ModelPosition modelPosition;

#ifdef ENABLE_ANIMATIONS 
#include struct xyz.chunkstories.graphics.vulkan.systems.models.ExperimentalBonesData
uniform ExperimentalBonesData animationData;
#endif

void main()
{
	#ifdef ENABLE_ANIMATIONS 
	vec4 v0 = animationData.bones[boneIdIn.x] * vec4(vertexIn.xyz, 1.0);
	vec4 v1 = animationData.bones[boneIdIn.y] * vec4(vertexIn.xyz, 1.0);
	vec4 v2 = animationData.bones[boneIdIn.z] * vec4(vertexIn.xyz, 1.0);
	vec4 v3 = animationData.bones[boneIdIn.w] * vec4(vertexIn.xyz, 1.0);
	
	vec4 animatedVertex = (v0 * boneWeightIn.x + v1 * boneWeightIn.y + v2 * boneWeightIn.z + v3 * boneWeightIn.w);
	
	vec4 n0 = animationData.bones[boneIdIn.x] * vec4(normalIn.xyz, 0.0);
	vec4 n1 = animationData.bones[boneIdIn.y] * vec4(normalIn.xyz, 0.0);
	vec4 n2 = animationData.bones[boneIdIn.z] * vec4(normalIn.xyz, 0.0);
	vec4 n3 = animationData.bones[boneIdIn.w] * vec4(normalIn.xyz, 0.0);
	
	vec4 animatedNormal = (n0 * boneWeightIn.x + n1 * boneWeightIn.y + n2 * boneWeightIn.z + n3 * boneWeightIn.w);
	#else
	vec4 animatedVertex = vec4(vertexIn, 1.0);
	vec4 animatedNormal = vec4(normalIn, 0.0);
	#endif

	vec4 viewSpace = camera.viewMatrix * modelPosition.matrix * vec4(animatedVertex);
	vec4 projected = camera.projectionMatrix * viewSpace;

	vertex = animatedVertex.xyz;
	normal = (modelPosition.matrix * animatedNormal).xyz;
	texCoord = texCoordIn;

	#ifdef ENABLE_ANIMATIONS
	boneId = boneIdIn;
	boneWeight = boneWeightIn;
	#endif

	gl_Position = projected;
}
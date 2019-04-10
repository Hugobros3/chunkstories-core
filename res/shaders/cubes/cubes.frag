#version 450

struct Box {
    vec3 center;
    vec3 radius;
    vec3 invRadius;
    mat3 rot;
};

struct Ray {
	vec3 origin;
	vec3 dir;
};

in vec3 vertex;
in vec2 texCoord;
flat in int textureId;

in Box box;

uniform sampler2DArray albedoTextures;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.api.graphics.structs.WorldConditions
uniform WorldConditions world;

#include struct xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize
uniform ViewportSize viewportSize;

#include ../sky/sky.glsl
#include ../normalcompression.glsl

float max(vec3 v) { return max (max(v.x, v.y), v.z); }

// box.rotation = object-to-world, invRayDir unused if oriented
bool ourIntersectBox(Box box, Ray ray, out float distance, out vec3 normal, const bool canStartInBox, const in bool oriented, in vec3 _invRayDir) {
	ray.origin = ray.origin - box.center;
	if (oriented) { 
		ray.dir *= box.rot; 
		ray.origin *= box.rot; 
	}

	float winding = (canStartInBox && (max(abs(ray.origin) * box.invRadius)	< 1.0)) ? -1.0 : 1.0;
	vec3 sgn = -sign(ray.dir);
	// Distance to plane
	vec3 d = box.radius * winding * sgn - ray.origin;
	if (oriented) 
		d /= ray.dir;
	else 
		d *= _invRayDir;

	# define TEST(U, VW) (d.U >= 0.0) && \
	all(lessThan(abs(ray.origin.VW + ray.dir.VW * d.U), box.radius.VW))
	bvec3 test = bvec3(TEST(x, yz), TEST(y, zx), TEST(z, xy));
	sgn = test.x ? vec3(sgn.x,0,0) : (test.y ? vec3(0,sgn.y,0) :
	vec3(0,0,test.z ? sgn.z:0));
	# undef TEST

	distance = (sgn.x != 0) ? d.x : ((sgn.y != 0) ? d.y : d.z);
	normal = oriented ? (box.rot * sgn) : sgn;
	return (sgn.x != 0) || (sgn.y != 0) || (sgn.z != 0);
}

void main()
{
	// The magic virtual texturing stuff 
	// ( requires EXT_descriptor_indexing.shaderSampledImageArrayNonUniformIndexing )
	//vec4 albedo = vtexture2D(textureId, texCoord);
	vec4 albedo = texture(albedoTextures, vec3(texCoord, textureId));

	vec4 screenSpaceCoordinates = vec4((gl_FragCoord.xy / viewportSize.size) * 2.0 - vec2(1.0), 0.0, 1.0);
	vec4 cameraSpaceCoordinates = (camera.projectionMatrixInverted * screenSpaceCoordinates);

	//cameraSpaceCoordinates /= cameraSpaceCoordinates.w;

	//eyeDirection = normalize(camera.normalMatrixInverted * (cameraSpaceCoordinates.xyz));
	vec3 eyeDirection = normalize((camera.viewMatrixInverted * vec4(cameraSpaceCoordinates.xyz, 0.0)).xyz);

	Ray ray;
	ray.origin = camera.position;
	ray.dir = eyeDirection;

	float distanceToBlock;
	vec3 hitNormal;

	bool hit = ourIntersectBox(box, ray, distanceToBlock, hitNormal, false, false, vec3(1.0) / ray.dir);

	if(!hit) {
		discard;
		colorBuffer = vec4(vec3(1.0, 0.0, 0.0), 1.0);
		normalBuffer = vec4(0.);
		
		gl_FragDepth = 0.0;
	} else {
		/*if(albedo.a == 0.0) {
			discard;
		}

		if(albedo.a < 1.0) {
			albedo.rgb *= vec3(0.2, 1.0, 0.5);
			albedo.a = 1.0;
		}*/

		//colorBuffer = vec4(gl_FragCoord.xy / viewportSize.size, 0.0, 1.0);
		//colorBuffer = vec4(eyeDirection.xyz, 1.0);
		//colorBuffer = vec4(normalize(camera.position - box.center), 1.0);
		colorBuffer = vec4(hitNormal * 0.5 + vec3(0.5), 1.0);

		//colorBuffer = vec4((distanceToBlock / 200.0), 0.0, 0.0, 1.0);

		normalBuffer = vec4(encodeNormal(camera.normalMatrix * vec3(0.0, 1.0, 0.0)), vec2(1.0, 0.0));

		gl_FragDepth = clamp((distanceToBlock - 0.1) / 2000.0,0.0, 1.0);
	}
}
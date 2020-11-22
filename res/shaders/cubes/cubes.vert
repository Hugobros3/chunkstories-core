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

in uvec3 vertexIn;
in uint textureIdIn;

out vec3 vertex;
out vec2 texCoord;
flat out int textureId;

out Box box;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

#include struct xyz.chunkstories.graphics.common.structs.ViewportSize
uniform ViewportSize viewportSize;

#include struct xyz.chunkstories.graphics.common.world.ChunkRenderInfo
//uniform ChunkRenderInfo chunkInfo;
instanced ChunkRenderInfo chunkInfo; // look mom, no uniforms !

//Fast Quadric Proj: "GPU-Based Ray-Casting of Quadratic Surfaces" http://dl.acm.org/citation.cfm?id=2386396
void quadricProj(in vec3 osPosition, in float voxelSize, in mat4 objectToScreenMatrix, in vec2 screenSize, inout vec4 position, inout float pointSize) {
    const vec4 quadricMat = vec4(1.0, 1.0, 1.0, -1.0);
    float sphereRadius = voxelSize * 1.732051;
    vec4 sphereCenter = vec4(osPosition.xyz, 1.0);
    mat4 modelViewProj = transpose(objectToScreenMatrix);

    mat3x4 matT = mat3x4( mat3(modelViewProj[0].xyz, modelViewProj[1].xyz, modelViewProj[3].xyz) * sphereRadius);
    matT[0].w = dot(sphereCenter, modelViewProj[0]);
    matT[1].w = dot(sphereCenter, modelViewProj[1]);
    matT[2].w = dot(sphereCenter, modelViewProj[3]);

    mat3x4 matD = mat3x4(matT[0] * quadricMat, matT[1] * quadricMat, matT[2] * quadricMat);
    vec4 eqCoefs =	
        vec4(dot(matD[0], matT[2]), dot(matD[1], matT[2]), dot(matD[0], matT[0]), dot(matD[1], matT[1])) 
        / dot(matD[2], matT[2]);

    vec4 outPosition = vec4(eqCoefs.x, eqCoefs.y, 0.0, 1.0);
    vec2 AABB = sqrt(eqCoefs.xy * eqCoefs.xy - eqCoefs.zw);
    AABB *= screenSize;

    position.xy = outPosition.xy * position.w;
    pointSize = max(AABB.x, AABB.y);
}

void main() {
    vec3 vertexPos = vertexIn.xyz;
    vertexPos += vec3(chunkInfo.chunkX, chunkInfo.chunkY, chunkInfo.chunkZ) * 32.0;

    //vec4 viewSpace = camera.viewMatrix * vec4(vertexPos, 1.0);
    //vec4 projected = camera.projectionMatrix * viewSpace;

    //mat4 matrix = camera.projectionMatrix * camera.viewMatrix;
    vec4 projected = camera.combinedViewProjectionMatrix * vec4(vertexPos, 1.0);

    vec4 pos = vec4(projected);
    float pSize;

    //cool
    vec2 halfViewport = viewportSize.size;
    quadricProj(vertexPos, 1.0, camera.combinedViewProjectionMatrix, halfViewport, pos, pSize);

    vertex = vertexPos;
    /*color = vec4(colorIn, 1.0);
    normal = camera.normalMatrix * normalIn;*/
    texCoord = vec2(0.0);
    textureId = int(textureIdIn);

    Box niceBox;// = Box(vertexPos + vec3(0.5), vec3(1.0), vec3(1.0), mat3());
    niceBox.center = vertexPos + vec3(0.);
    niceBox.radius = vec3(1.0);
    niceBox.invRadius = vec3(1.0);
    box = niceBox;

    gl_Position = vec4(pos.xyzw);
    gl_PointSize = pSize;

    if(dot(normalize(niceBox.center - camera.position), normalize(camera.lookingAt)) < 0.0) {
        gl_Position = vec4(-1.0);
        gl_PointSize = 0.0;
    }

    projected /= projected.w;

    if(projected.x > 1.0 || projected.x < -1.0 || projected.y > 1.0 || projected.y < -1.0) {
        gl_Position = vec4(-1.0);
        gl_PointSize = 0.0;
    }

    /*if(dot(normalize(niceBox.center - camera.position), normalize(vec3(1.0, 0.0, 0.0))) < 0.0) {
        gl_Position = vec4(-1.0);
        gl_PointSize = 0.0;
    }*/

    //gl_PointSize = 200.0;

    //gl_Position = projected;
    //gl_PointSize = 4096.0 / projected.z;
}

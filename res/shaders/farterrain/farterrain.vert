#version 450

out vec3 position;
//out vec3 barycentric;
out vec3 computedNormal;
//out int column;
//out int row;

#include struct xyz.chunkstories.api.graphics.structs.Camera
uniform Camera camera;

struct grid {
    float baseX;
    float baseZ;
    float scale; // scale of each cell
    int hcount; // the number of cells per side
};

layout(std430) buffer elementsBuffer {
    grid elements[];
};

//const vec3 bary[3] = { vec3(1.0, 0.0, 0.0), vec3(0.0, 1.0, 0.0), vec3(0.0, 0.0, 1.0)};
//const vec2 quadsVerts[6] = { vec2(0.0, 0.0), vec2(1.0, 1.0), vec2(1.0, 0.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(0.0, 0.0) };

uniform isampler2D heightTexture;

void main() {
    grid inst = elements[gl_InstanceIndex];
    //int patchIndex = gl_VertexIndex / 6;
    //int cell = patchIndex;
    //int cellx = cell % inst.hcount;
    //int cellz = cell / inst.hcount;
    //vec2 vert = quadsVerts[gl_VertexIndex % 6];

    //vec2 hPos = vec2( inst.baseX + (float(cellx) + vert.x) * inst.scale, inst.baseZ + (float(cellz) + vert.y) * inst.scale);
    
    int vertex_z = gl_VertexIndex % (inst.hcount + 1);
    int vertex_x = gl_VertexIndex / (inst.hcount + 1);
    vec2 hPos = vec2( inst.baseX + (float(vertex_x)) * inst.scale, inst.baseZ + (float(vertex_z)) * inst.scale);
    
    //vec2 chPos = vec2( inst.baseX + (float(cellx)) * inst.scale, inst.baseZ + (float(cellz)) * inst.scale);

    int height = texture(heightTexture, mod(vec2(hPos.x, hPos.y), vec2(4096.0)) / vec2(4096.0) ).r;

    /*int second = (gl_VertexIndex % 6) / 3;
    vec2 hp0 = chPos + quadsVerts[3 * second + 0];
    vec2 hp1 = chPos + quadsVerts[3 * second + 1];
    vec2 hp2 = chPos + quadsVerts[3 * second + 2];

    float h0 = float(texture(heightTexture, mod(hp0, vec2(4096.0)) / vec2(4096.0) )).r;
    float h1 = float(texture(heightTexture, mod(hp1, vec2(4096.0)) / vec2(4096.0) )).r;
    float h2 = float(texture(heightTexture, mod(hp2, vec2(4096.0)) / vec2(4096.0) )).r;

    vec3 p0 = vec3(hp0.x, h0, hp0.y);
    vec3 p1 = vec3(hp1.x, h1, hp1.y);
    vec3 p2 = vec3(hp2.x, h2, hp2.y);

    vec3 a = p1 - p0;
    vec3 b = p0 - p2;
    vec3 normal = normalize(cross(b, a));*/
    vec3 normal = vec3(0.0, 1.0, 0.0);

    vec4 point = vec4(hPos.x, float(height), hPos.y, 1.0);
	vec4 viewSpace = camera.viewMatrix * point;
	vec4 projected = camera.projectionMatrix * viewSpace;

	position = point.xyz;
    //column = cellx;
    //row = cellz;
    computedNormal = camera.normalMatrix * normal;
    //barycentric = bary[gl_VertexIndex % 3];
    
	gl_Position = projected;

	/*position = vec3(0.0);
    column = 0;
    row = 0;
    normalOut = vec3(0.0);
    barycentric = vec3(0.0);
    
	gl_Position = vec4(0.0);*/
}
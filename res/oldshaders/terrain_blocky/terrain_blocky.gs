#version 330 core
layout (points) in;
layout (triangle_strip, max_vertices = 18) out;

//Common camera matrices & uniforms
uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform mat4 modelViewProjectionMatrix;
uniform mat4 modelViewProjectionMatrixInv;

uniform vec3 camPos;

//Fog
uniform float fogStartDistance;
uniform float fogEndDistance;

flat in ivec4 indexPassed[];
in vec4 displacementPassed[];

uniform usampler2DArray heights;
uniform usampler2DArray topVoxels; // Block ids

uint access(usampler2DArray tex, vec2 coords) {

	float xBlend = 0.5 + sign(coords.x - 1.0) * 0.5;
	float yBlend = 0.5 + sign(coords.y - 1.0) * 0.5;

	/*uint t00 = texture(tex, vec3(coords, indexPassed[0].x)).r;
	uint t01 = texture(tex, vec3(coords - vec2(0.0, 1.0), indexPassed[0].y)).r;
	uint t10 = texture(tex, vec3(coords - vec2(1.0, 0.0), indexPassed[0].z)).r;
	uint t11 = texture(tex, vec3(coords - vec2(1.0), indexPassed[0].w)).r;
	
	return t00;*/
	if(coords.x <= 1.0) {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords, indexPassed[0].x)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(0.0, 1.0), indexPassed[0].y)).r;
		}
	}
	else {
		if(coords.y <= 1.0) {
			return texture(tex, vec3(coords - vec2(1.0, 0.0), indexPassed[0].z)).r;
		}
		else {
			return texture(tex, vec3(coords - vec2(1.0), indexPassed[0].w)).r;
		}
	}
	return 250u;
}

out vec4 colorPassed;
out vec4 vertexPassed;
out vec3 normalPassed;
out vec3 eyeDirection;
out float fogIntensity;
flat out uint voxelId;

uniform int lodLevel;

const vec2 triangleConstruction[] = vec2[](vec2(0.0), vec2(0.0, 1.0), vec2(1.0, 1.0), vec2(0.0), vec2(1.0, 0.0), vec2(1.0, 1.0));

void createSlab(vec2 pos, int lod);

uint getHeight(vec2 pos, int lod);

void main() {
	
	//Compute coordinates & height
	createSlab(gl_in[0].gl_Position.xz, lodLevel);
}
uint getHeight(vec2 pos, int lod) {
	vec2 inMeshCoords = (pos.yx + mod(displacementPassed[0].yx, 256.0) + vec2(0.5)) / vec2(256.0);
	return access(heights, inMeshCoords) + 1u;
}

uint voxel(vec2 pos, int lod) {
	vec2 inMeshCoords = (pos.yx + mod(displacementPassed[0].yx, 256.0) + vec2(0.5)) / vec2(256.0);
	return access(topVoxels, inMeshCoords);
}

void createSlab(vec2 pos, int lod) {
	int height = int(getHeight(pos, lod));
	
	/*vec3 sum = (modelViewMatrix * (vec4(pos.x, height, pos.y, 1.0) + vec4(displacementPassed[0].x, 0, displacementPassed[0].y, 0.0))).xyz;
	float dist = length(sum)-fogStartDistance;
	const float LOG2 = 1.442695;
	float density = 0.0025;
	float fogFactor = exp2( -density * 
					   density * 
					   dist * 
					   dist * 
					   LOG2 );
	fogFactor = (dist) / (fogEndDistance-fogStartDistance);
	fogIntensity = clamp(fogFactor, 0.0, 1.0);*/
	
	uint voId = voxel(pos, 0);
	
	//colorPassed = vec4(vec3(0.10), 1.0);
	normalPassed = vec3(0.0, 1.0, 0.0);
	voxelId = voId;
	for(int i = 0; i < 6; i++) {
		vec4 vertice = vec4(pos.x, 0.0, pos.y, 1.0);
		
		vertice.xz += displacementPassed[0].xy + triangleConstruction[i] * 32 / pow(2, lod);
		vertice.y += height;
		
		gl_Position = modelViewProjectionMatrix * vertice;
		vertexPassed = vertice;
		eyeDirection = vertice.xyz-camPos;
		
		EmitVertex();
	}
	EndPrimitive();
	
	int hnx = int(getHeight(pos + vec2(32 / pow(2, lod), 0.0), lod));
	int hnz = int(getHeight(pos + vec2(0.0, 32 / pow(2, lod)), lod));
	
	if((hnx - height) != 0) {
		voxelId = voId;
		if(hnx > height) {
			normalPassed = vec3(-1.0, 0.0, 0.0);
			voxelId = voxel(pos + vec2(32 / pow(2, lod), 0.0), 0);
		} else {
			normalPassed = vec3(1.0, 0.0, 0.0);
		}
		
		for(int i = 0; i < 6; i++) {
			vec4 vertice = vec4(pos.x + 32 / pow(2, lod), 0.0, pos.y, 1.0);
			
			vertice.xz += displacementPassed[0].xy;
			vertice.yz += triangleConstruction[i] * vec2(float(hnx - height), 32 / pow(2, lod));
			vertice.y += height;
			
			gl_Position = modelViewProjectionMatrix * vertice;
			vertexPassed = vertice;
			
			EmitVertex();
		}
		EndPrimitive();
	}
	
	if((hnz - height) != 0) {
		voxelId = voId;
		if(hnz > height) {
			normalPassed = vec3(0.0, 0.0, -1.0);
			voxelId = voxel(pos + vec2(0.0, 32 / pow(2, lod)), 0);
		} else
			normalPassed = vec3(0.0, 0.0, 1.0);
		
		for(int i = 0; i < 6; i++) {
			vec4 vertice = vec4(pos.x, 0.0, pos.y + 32 / pow(2, lod), 1.0);
			
			vertice.xz += displacementPassed[0].xy;
			vertice.yx += triangleConstruction[i] * vec2(float(hnz - height), 32 / pow(2, lod));
			vertice.y += height;
			
			gl_Position = modelViewProjectionMatrix * vertice;
			vertexPassed = vertice;
			
			EmitVertex();
		}
		EndPrimitive();
	}
}

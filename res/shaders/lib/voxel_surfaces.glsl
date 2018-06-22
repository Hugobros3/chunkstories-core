//Include this to have access to voxel surfaces data in your shader

struct VoxelSurface {
	vec3 albedoColor;
};

#define MAX_SURFACES_TYPES 512

layout(std140) uniform VoxelSurfaces {
	VoxelSurface surfaces[MAX_SURFACES_TYPES];
};
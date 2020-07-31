struct BBox {
    vec3 min;
    vec3 max;
};

vec2 intersect_bbox(BBox bbox, vec3 origin, vec3 direction) {
    vec3 ray_inv_dir = vec3(1.0) / direction;

    float txmin = bbox.min.x * ray_inv_dir.x - (origin.x * ray_inv_dir.x);
    float txmax = bbox.max.x * ray_inv_dir.x - (origin.x * ray_inv_dir.x);
    float tymin = bbox.min.y * ray_inv_dir.y - (origin.y * ray_inv_dir.y);
    float tymax = bbox.max.y * ray_inv_dir.y - (origin.y * ray_inv_dir.y);
    float tzmin = bbox.min.z * ray_inv_dir.z - (origin.z * ray_inv_dir.z);
    float tzmax = bbox.max.z * ray_inv_dir.z - (origin.z * ray_inv_dir.z);

    float t0x = min(txmin, txmax);
    float t1x = max(txmin, txmax);
    float t0y = min(tymin, tymax);
    float t1y = max(tymin, tymax);
    float t0z = min(tzmin, tzmax);
    float t1z = max(tzmin, tzmax);
    
    float t0 = max(max(t0x, t0y), t0z);
    float t1 = min(min(t1x, t1y), t1z);

    return vec2(t0, t1);
}
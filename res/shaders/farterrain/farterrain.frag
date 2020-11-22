#version 450

in vec3 position;
//in vec3 barycentric;
in vec3 computedNormal;
//flat in int column;
//flat in int row;

out vec4 colorBuffer;
out vec4 normalBuffer;

#include ../normalcompression.glsl

uniform sampler2D terrainColor;

/*float edgeFactor() {
    vec3 d = fwidth(barycentric);
    vec3 a3 = smoothstep(vec3(0.0), d*0.5, barycentric);
    return min(min(a3.x, a3.y), a3.z);
}*/

void main() {
    //vec4 albedo = vec4(vec3(float((column + row) % 2) * 1.0), 1.0);
    //vec4 albedo = vec4(vec3(edgeFactor()), 1.0);

    vec4 albedo = vec4(vec3(1.0), 1.0);
    albedo = texture(terrainColor, mod(vec2(position.x, position.z), vec2(4096.0)) / vec2(4096.0));
    
    if(albedo.a < 1.0) {
        float temperature = 0.5;
        vec3 grassColor = mix(vec3(0.4, 0.8, 0.4), vec3(0.5, 0.85, 0.25), 0.5 + 0.5 * temperature);
        albedo.rgb *= grassColor;
        albedo.a = 1.0;
    }

    vec3 normal = computedNormal;
    //vec3 normal = vec3(0.0);

    colorBuffer = vec4(albedo.rgb, 1.0);
    normalBuffer = vec4(encodeNormal(normalize(normal)), 1.0, 0.0);
}

#version 400 core

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec2 fragTexCoord;
out vec3 fragNormal;
out vec3 fragPosition;
out vec2 texCoordOut;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform sampler2D heightMap;
uniform sampler2D biomeMap;
uniform vec2 planetScale;
uniform vec2 chunkOffset;
uniform float radius = 1.0;
const float cellSize = 0.01;
const float PI = 3.14159;

vec3 sphereify(float x, float y) {
    float zenith = clamp(x * PI, 0.001, PI - 0.001);
    float azimuth = mod(y * 4.0 * PI, 4.0 * PI);
    float height = texture(heightMap, texCoord).r;
    float r = radius;

    float _x = r * sin(zenith) * cos(azimuth);
    float _y = r * cos(zenith);
    float _z = r * sin(zenith) * sin(azimuth);
    return vec3(_x, _y, _z);
}

//vec3 calculateNormal(float x, float y) {
//    float heightL = texture(heightMap, vec2(x - 0.001, y)).r;
//    float heightR = texture(heightMap, vec2(x + 0.001, y)).r;
//    float heightD = texture(heightMap, vec2(x, y - 0.001)).r;
//    float heightU = texture(heightMap, vec2(x, y + 0.001)).r;
//
//    vec3 dx = vec3(0.002, 0.0, heightR - heightL);
//    vec3 dy = vec3(0.0, 0.002, heightU - heightD);
//
//    return normalize(cross(dx, dy));
//}
vec3 calculateNormal(float x, float y) {
    vec3 dx = vec3(sphereify(x + 0.01, y) - sphereify(x - 0.01, y));
    vec3 dy = vec3(sphereify(x, y + 0.01) - sphereify(x, y - 0.01));
    return normalize(cross(dx, dy));
}

void main() {
    vec2 globalUV = position.xz * planetScale + chunkOffset;
    vec3 pos = sphereify(globalUV.x, globalUV.y);
    vec4 worldPosition = transformationMatrix * vec4(pos, 1.0);
    gl_Position =  projectionMatrix * viewMatrix * worldPosition;
    fragNormal = calculateNormal(globalUV.x, globalUV.y);
    fragPosition = worldPosition.xyz;
    fragTexCoord = texCoord;
    texCoordOut = texCoord;
//    fragBiomeColor = vec3(fragHeight);

}

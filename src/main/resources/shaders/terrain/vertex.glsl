#version 400 core

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec2 fragTexCoord;
out vec3 fragNormal;
out float fragHeight;
out vec3 fragPosition;
out vec3 fragBiomeColor;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform sampler2D heightMap;
uniform sampler2D biomeMap;
const float cellSize = 0.02;

vec3 calculateNormal(float x, float y) {
    float heightL = texture(heightMap, vec2(x - 0.001, y)).r;
    float heightR = texture(heightMap, vec2(x + 0.001, y)).r;
    float heightD = texture(heightMap, vec2(x, y - 0.001)).r;
    float heightU = texture(heightMap, vec2(x, y + 0.001)).r;

    vec3 dx = vec3(0.002, 0.0, heightR - heightL);
    vec3 dy = vec3(0.0, 0.002, heightU - heightD);

    return normalize(cross(dx, dy));
}

void main() {
    vec3 pos = vec3(position.x, (texture(heightMap, texCoord).x)*100, position.z);
    vec4 worldPosition = transformationMatrix * vec4(pos, 1.0);
    gl_Position =  projectionMatrix * viewMatrix * worldPosition;
    fragNormal = calculateNormal(position.x, position.y);
    fragPosition = worldPosition.xyz;
    fragHeight = texture(heightMap, texCoord).r;
    fragTexCoord = texCoord;
    fragBiomeColor = texture(biomeMap, texCoord+vec2(0, cellSize)).xyz * 0.25f +
    texture(biomeMap, texCoord+vec2(cellSize, 0)).xyz * 0.25f + texture(biomeMap, texCoord+vec2(0, -cellSize)).xyz * 0.25f +
    texture(biomeMap, texCoord+vec2(-cellSize, 0)).xyz * 0.25f;

}

//4. Handle Seams Between Grids
//To avoid visible seams:
//
//Ensure consistent normals across edges
//
//Add a 1-vertex overlap between adjacent patches
//
//Use triplanar mapping or blend textures at edges
//https://www.youtube.com/watch?v=4xR46-YI828

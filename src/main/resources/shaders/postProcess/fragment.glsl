#version 400 core

in vec2 fragTexCoord;

out vec4 fragColor;


uniform sampler2D textureSampler;
uniform sampler2D depthSampler;
uniform int planetCount;
uniform vec3 planetPositions[10];
uniform float planetWaterLevels[10];
uniform vec3 camPos;
uniform mat4 projView;
uniform mat4 projMatInv;
uniform mat4 viewMatInv;
uniform ivec2 resolution;

const int steps = 1000;
const float minDist = 0.01f;
const float maxDist = 2000.0f;

const float near = 0.01f;
const float far = 2000.0f;

float linearizeDepth(float depth)
{
    float z = depth * 2.0 - 1.0; // Convert to NDC
    return (2.0 * near * far) / (far + near - z * (far - near));
}



vec2 raySphere(vec3 centre, float radius, vec3 rayOrigin, vec3 rayDir) {
    vec3 offset = rayOrigin - centre;
    const float a = 1; // set to dot(rayDir, rayDir) instead if rayDir may not be normalized
    float b = 2 * dot(offset, rayDir);
    float c = dot (offset, offset) - radius * radius;

    float discriminant = b*b-4*a*c;
    if (discriminant > 0) {
        float s = sqrt(discriminant);
        float dstToSphereNear = max(0, (-b - s) / (2 * a));
        float dstToSphereFar = (-b + s) / (2 * a);

        if (dstToSphereFar >= 0) {
            return vec2(dstToSphereNear, dstToSphereFar - dstToSphereNear);
        }
    }
    // Ray did not intersect sphere
    return vec2(maxDist, 0);
}

vec3 getWorldPosition(vec2 uv, float depth) {
    float clipZ = depth * 2.0 - 1.0;
    vec2 ndc = uv * 2.0 - 1.0;
    vec4 clip = vec4(ndc, clipZ, 1.0);

    vec4 view = projMatInv * clip;
    vec4 world = viewMatInv * view;

    return world.xyz / world.w;
}

void main() {
    fragColor = texture(textureSampler, fragTexCoord);
//    vec2 uv = (gl_FragCoord.xy + 0.5) / vec2(resolution);
//    float depth = texture(depthSampler, uv).x;
//    vec3 worldPos = getWorldPosition(uv, depth);
//    float sceneDepth = length(worldPos - camPos);
//
//    vec3 rayOrigin = camPos;
//    vec3 rayDir = normalize(worldPos - rayOrigin);
//
//    vec2 sphere = raySphere(vec3(0, 0, -400), 200, rayOrigin, rayDir);

//    fragColor = vec4(vec3(sceneDepth - sphere.x),1);

}

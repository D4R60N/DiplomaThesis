#version 430

layout (local_size_x = 8, local_size_y = 4, local_size_z = 1) in;

layout(rgba32f, binding = 0) uniform image2D transmittanceImage;

uniform float Re;
uniform float Ra;
uniform float Hr;
uniform float Hm;
uniform vec3 BetaR;
uniform vec3 BetaM;

const float NUM_SAMPLES = 50.0;
const float PI = 3.14159265359;

float getDensity(float altitude, float scaleHeight) {
    float h = altitude - Re;
    if (h < 0.0) return 0.0;
    return exp(-h / scaleHeight);
}

vec2 raySphereIntersect(vec3 rayOrigin, vec3 rayDir, float radius) {
    float a = dot(rayDir, rayDir);
    float b = 2.0 * dot(rayOrigin, rayDir);
    float c = dot(rayOrigin, rayOrigin) - radius * radius;
    float discriminant = b * b - 4.0 * a * c;

    if (discriminant < 0.0) {
        return vec2(-1.0);
    }

    float sqrtDisc = sqrt(discriminant);
    float t0 = (-b - sqrtDisc) / (2.0 * a);
    float t1 = (-b + sqrtDisc) / (2.0 * a);

    return vec2(t0, t1);
}


void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(transmittanceImage);

//    if (coord.x >= size.x || coord.y >= size.y) {
//        return;
//    }

    float u = float(coord.x) / float(size.x);
    float r = Re + u * (Ra - Re);

    float v = float(coord.y) / float(size.y);
    float mu = -1.0 + v * 2.0;

    vec3 rayOrigin = vec3(0.0, r, 0.0);
    float sin_phi = sqrt(1.0 - mu * mu);
    vec3 rayDir = vec3(sin_phi, mu, 0.0);

    vec2 t_intersect = raySphereIntersect(rayOrigin, rayDir, Ra);

    float t_max = t_intersect.y;
    if (t_max < 0.0) t_max = 0.0;

    vec2 t_ground = raySphereIntersect(rayOrigin, rayDir, Re);
    if (t_ground.x > 0.0 && t_ground.x < t_max) {
        t_max = t_ground.x;
    }


    float stepSize = t_max / NUM_SAMPLES;
    vec3 opticalDepth = vec3(0.0);
    vec3 currentPos = rayOrigin;

    for (int i = 0; i < int(NUM_SAMPLES); ++i) {
        vec3 midPos = currentPos + rayDir * (stepSize * 0.5);
        float midAltitude = length(midPos);

        float densityR = getDensity(midAltitude, Hr);
        float densityM = getDensity(midAltitude, Hm);

        vec3 scattering = (BetaR * densityR + BetaM * densityM) * stepSize;

        opticalDepth += scattering;
        currentPos += rayDir * stepSize;
    }

    vec3 T = exp(-opticalDepth);

    imageStore(transmittanceImage, coord, vec4(T, 1.0));
}
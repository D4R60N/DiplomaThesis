#version 450
#extension GL_ARB_shading_language_include : require
in vec3 view_ray;
in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2D depthSampler;
uniform sampler2D transmittanceTexture;
uniform sampler2D multiScatteringTexture;
uniform sampler2D skyViewTexture;
uniform sampler3D aerialPerspectiveTexture;

uniform ivec2 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform ivec2 uSkyViewTextureSize;
uniform ivec3 uAerialPerspectiveTextureSize;

uniform vec3 camPos;
uniform float exposure;
uniform vec3 sunDirection;
uniform vec3 sunIlluminance;

uniform mat4 viewMatInv;
uniform mat4 projMatInv;
uniform mat4 invViewProj;
uniform ivec2 resolution;

uniform vec3 RayMarchMinMaxSPP;

const bool RENDER_SUN_DISK = false;
#include "/common.glsl"
#define AP_SLICE_COUNT uAerialPerspectiveTextureSize.z
#define AP_KM_PER_SLICE 4.0

uniform AtmosphereParameters uAtmosphere;

float getDistance(float depth, vec2 uv) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 posWorld = invViewProj * ndc;
    posWorld /= posWorld.w;
    return length(posWorld.xyz - camPos);
}

vec3 tonemapACES(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec2 uv = gl_FragCoord.xy / vec2(resolution);
    float depth = texture(depthSampler, uv).r;

    vec3 worldDir = normalize(view_ray);

    vec3 worldPos = camPos + vec3(0.0, uAtmosphere.BottomRadius, 0.0);
    float viewHeight = length(worldPos);

    vec3 finalLuminance = vec3(0.0);
    float opacity = 0.0;

    if (depth >= 1.0 || depth <= 0.0) {
        vec3 upVector = normalize(worldPos);
        float viewZenithCosAngle = dot(worldDir, upVector);

        vec3 sideVector = normalize(cross(upVector, worldDir));
        vec3 forwardVector = normalize(cross(sideVector, upVector));
        vec2 lightOnPlane = normalize(vec2(dot(sunDirection, forwardVector), dot(sunDirection, sideVector)));
        float lightViewCosAngle = lightOnPlane.x;

        bool intersectGround = raySphereIntersectNearest(worldPos, worldDir, vec3(0.0), uAtmosphere.BottomRadius) >= 0.0;

        vec2 lutUv;
        SkyViewLutParamsToUv(uAtmosphere, intersectGround, viewZenithCosAngle, lightViewCosAngle, viewHeight, lutUv);

        finalLuminance = texture(skyViewTexture, lutUv).rgb;
        finalLuminance += GetSunLuminance(worldPos, worldDir, uAtmosphere.BottomRadius);
        opacity = 1.0;
    }
    else {
        vec3 sceneColor = texture(textureSampler, uv).rgb;

        float tDepth = getDistance(depth, uv);

        float slice = tDepth / AP_KM_PER_SLICE;
        float weight = 1.0;
        if (slice < 0.5) {
            weight = clamp(slice * 2.0, 0.0, 1.0);
            slice = 0.5;
        }

        float w = sqrt(slice / AP_SLICE_COUNT);

        vec4 apSample = texture(aerialPerspectiveTexture, vec3(uv, w));

        finalLuminance = sceneColor * (1.0 - apSample.a) + (apSample.rgb * weight);
        opacity = 1.0;
    }

//    finalLuminance *= exposure;
    finalLuminance = tonemapACES(finalLuminance);

//    finalLuminance = pow(finalLuminance, vec3(1.0 / 2.2));

    fragColor = vec4(finalLuminance, opacity);
}


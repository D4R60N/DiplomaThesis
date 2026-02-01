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

const bool RENDER_SUN_DISK = true;
#include "/common.glsl"
#define AP_SLICE_COUNT uAerialPerspectiveTextureSize.z
#define AP_KM_PER_SLICE 4.0

uniform AtmosphereParameters uAtmosphere;

float AerialPerspectiveDepthToSlice(float depth) {
    return depth / AP_KM_PER_SLICE;
}

vec3 tonemapACES(vec3 x) {
    float a = 2.51; float b = 0.03; float c = 2.43; float d = 0.59; float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec2 uv = fragTexCoord;
    float depth = texture(depthSampler, uv).r;
    vec3 camPosKM = camPos * 0.001;

    vec3 WorldDir = normalize(view_ray);
    vec3 WorldPos = camPosKM + vec3(0.0, uAtmosphere.BottomRadius, 0.0);

    float viewHeight = length(WorldPos);

    vec3 L = vec3(0.0);
    float opacity = 0.0;

    if (viewHeight < uAtmosphere.TopRadius && depth >= 1.0) {
        vec3 upVector = normalize(WorldPos);
        float viewZenithCosAngle = dot(WorldDir, upVector);
        vec3 sideVector = normalize(cross(upVector, WorldDir));
        if (length(sideVector) < 0.001) sideVector = vec3(1, 0, 0);
        vec3 forwardVector = normalize(cross(sideVector, upVector));
        vec2 lightOnPlane = normalize(vec2(dot(sunDirection, forwardVector), dot(sunDirection, sideVector)));
        float lightViewCosAngle = lightOnPlane.x;
        bool itersectGround = raySphereIntersectNearest(WorldPos, WorldDir, vec3(0.0), uAtmosphere.BottomRadius) >= 0.0f;
        vec2 skyUv;
        SkyViewLutParamsToUv(uAtmosphere, itersectGround, viewZenithCosAngle, lightViewCosAngle, viewHeight, skyUv);
        L = texture(skyViewTexture, skyUv).rgb;
        L += GetSunLuminance(WorldPos, WorldDir, uAtmosphere.BottomRadius);
        opacity = 1.0;
    }
    else {
        vec3 depthClipSpace = vec3(uv * 2.0 - 1.0, depth * 2.0 - 1.0);
        vec4 DepthBufferWorldPos = invViewProj * vec4(depthClipSpace, 1.0);
        DepthBufferWorldPos /= DepthBufferWorldPos.w;
        float tDepth = length(DepthBufferWorldPos.xyz - camPos);
        tDepth *= 0.001;
        float Slice = AerialPerspectiveDepthToSlice(tDepth);
        float Weight = 1.0;
        if (Slice < 0.5) {
            Weight = saturate(Slice * 2.0);
            Slice = 0.5;
        }
        float w = sqrt(Slice / AP_SLICE_COUNT);
        vec4 AP = texture(aerialPerspectiveTexture, vec3(uv.x, uv.y, w));
        L.rgb = AP.rgb * Weight;
        opacity = AP.a * Weight;
        vec3 sceneColor = texture(textureSampler, uv).rgb;
        L = sceneColor * (1.0 - opacity) + L.rgb;
        opacity = 1.0;
    }

    L *= exposure;
    L = tonemapACES(L);
    L = pow(L, vec3(1.0 / 2.2));

    fragColor = vec4(L, opacity);
}


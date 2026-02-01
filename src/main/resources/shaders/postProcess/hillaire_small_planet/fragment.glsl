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
#define AP_KM_PER_SLICE 4000.0

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
    float rawDepth = texture(depthSampler, uv).r;
    
    vec3 WorldDir = normalize(view_ray);
    vec3 camPosKM = camPos * GU_TO_KM;
    vec3 worldPosKM = camPosKM + vec3(0.0, uAtmosphere.BottomRadius, 0.0);
    float viewHeightKM = length(worldPosKM);

    vec3 finalL = vec3(0.0);

    if (rawDepth >= 1.0) {
        vec3 rayAtmosPos = worldPosKM;

        if (MoveToTopAtmosphere(rayAtmosPos, WorldDir, uAtmosphere.TopRadius)) {
            float activeHeight = length(rayAtmosPos);
            vec3 upVector = normalize(rayAtmosPos);
            float viewZenithCosAngle = dot(WorldDir, upVector);

            vec3 sideVector = normalize(cross(upVector, WorldDir));
            if (length(sideVector) < 0.001) sideVector = vec3(1, 0, 0);
            vec3 forwardVector = normalize(cross(sideVector, upVector));
            vec2 lightOnPlane = normalize(vec2(dot(sunDirection, forwardVector), dot(sunDirection, sideVector)));

            bool intersectGround = false;

            vec2 skyUv;
            SkyViewLutParamsToUv(uAtmosphere, intersectGround, viewZenithCosAngle, lightOnPlane.x, activeHeight, skyUv);
            finalL = texture(skyViewTexture, skyUv).rgb;
        }

        finalL += GetSunLuminance(worldPosKM, WorldDir, uAtmosphere.BottomRadius);
    }
    else {
        vec3 depthClipSpace = vec3(uv * 2.0 - 1.0, rawDepth * 2.0 - 1.0);
        vec4 posGU = invViewProj * vec4(depthClipSpace, 1.0);
        posGU /= posGU.w;
        vec3 groundPosKM = posGU.xyz * GU_TO_KM + vec3(0.0, uAtmosphere.BottomRadius, 0.0);

        if (viewHeightKM > uAtmosphere.TopRadius) {
            float tEnter = raySphereIntersectNearest(worldPosKM, WorldDir, vec3(0.0), uAtmosphere.TopRadius);
            vec3 entryPos = worldPosKM + WorldDir * tEnter;
            float entryHeight = length(entryPos);
            vec3 upVector = entryPos / entryHeight;
            float viewZenithCosAngle = dot(WorldDir, upVector);

            vec3 sideVector = normalize(cross(upVector, WorldDir));
            if (length(sideVector) < 0.001) sideVector = vec3(1, 0, 0);
            vec3 forwardVector = normalize(cross(sideVector, upVector));
            vec2 lightOnPlane = normalize(vec2(dot(sunDirection, forwardVector), dot(sunDirection, sideVector)));

            vec2 skyUv;
            SkyViewLutParamsToUv(uAtmosphere, true, viewZenithCosAngle, lightOnPlane.x, entryHeight, skyUv);
            vec3 atmosColor = texture(skyViewTexture, skyUv).rgb;

            vec2 transUv;
            LutTransmittanceParamsToUv(uAtmosphere, entryHeight, viewZenithCosAngle, transUv);
            vec3 transmittance = texture(transmittanceTexture, transUv).rgb;

            vec3 sceneColor = texture(textureSampler, uv).rgb;
            finalL = (sceneColor * transmittance) + atmosColor;
        }
        else {
            float tDepthKM = length(posGU.xyz - camPos) * GU_TO_KM;
            float slice = tDepthKM / AP_KM_PER_SLICE;
            float weight = saturate(slice * 2.0);
            float w = clamp(sqrt(slice / AP_SLICE_COUNT), 0.0, 1.0);

            vec4 AP = texture(aerialPerspectiveTexture, vec3(uv.x, 1.0 - uv.y, w));
            vec3 sceneColor = texture(textureSampler, uv).rgb;
            finalL = sceneColor * (1.0 - (AP.a * weight)) + (AP.rgb * weight);
        }
    }

    finalL *= exposure;
    finalL = tonemapACES(finalL);
    finalL = pow(finalL, vec3(1.0 / 2.2));
    fragColor = vec4(finalL, 1.0);
}


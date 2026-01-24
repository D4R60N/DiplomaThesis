#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 8, local_size_y = 8) in;

layout(rgba32f, binding = 0) uniform writeonly image2D imgTransmittance;
uniform sampler2D u_TransmittanceLutTexture;

uniform ivec2 uTransmittanceTextureSize;
uniform mat4 gSkyInvViewProjMat;
uniform vec3 sunDirection;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 gSunIlluminance;
#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;

// A much faster version for Transmittance LUT only
vec3 IntegrateOpticalDepth(vec3 WorldPos, vec3 WorldDir, AtmosphereParameters Atmosphere, float SampleCountIni, bool VariableSampleCount)
{
    float tBottom = raySphereIntersectNearest(WorldPos, WorldDir, vec3(0.0), Atmosphere.BottomRadius);
    float tTop = raySphereIntersectNearest(WorldPos, WorldDir, vec3(0.0), Atmosphere.TopRadius);

    float tMax = 0.0f;
    if (tBottom < 0.0f)
    {
        if (tTop < 0.0f)
        {
            tMax = 0.0f;
            return vec3(0.0f);
        }
        else
        {
            tMax = tTop;
        }
    }
    else
    {
        if (tTop > 0.0f)
        {
            tMax = min(tTop, tBottom);
        }
    }

    float SampleCount = SampleCountIni;
    float SampleCountFloor = SampleCountIni;
    float tMaxFloor = tMax;
    float dt = tMax / SampleCount;
    vec3 OpticalDepth = vec3(0.0f);
    const float SampleSegmentT = 0.3f;
    float t = 0.0f;

    for (float s = 0.0; s < SampleCount; s += 1.0f)
    {
        if (VariableSampleCount)
        {
            float t0 = (s) / SampleCountFloor;
            float t1 = (s + 1.0f) / SampleCountFloor;
            t0 = t0 * t0;
            t1 = t1 * t1;
            t0 = tMaxFloor * t0;
            if (t1 > 1.0)
            {
                t1 = tMax;
            }
            else
            {
                t1 = tMaxFloor * t1;
            }
            t = t0 + (t1 - t0)*SampleSegmentT;
            dt = t1 - t0;
        }
        else
        {
            float NewT = tMax * (s + SampleSegmentT) / SampleCount;
            dt = NewT - t;
            t = NewT;
        }
        vec3 P = WorldPos + t * WorldDir;

        MediumSampleRGB medium = sampleMediumRGB(P, Atmosphere);
        OpticalDepth += medium.extinction * dt;
    }
    return OpticalDepth;
}

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uTransmittanceTextureSize.x) ||
    texelCoord.y >= int(uTransmittanceTextureSize.y)) {
        return;
    }
    AtmosphereParameters Atmosphere = uAtmosphere;

    vec2 pixPos = vec2(texelCoord) + 0.5;
    vec2 uv = pixPos / vec2(uTransmittanceTextureSize);
    float viewHeight;
    float viewZenithCosAngle;
    UvToLutTransmittanceParams(Atmosphere, viewHeight, viewZenithCosAngle, uv);

    vec3 WorldPos = vec3(0.0f, 0.0f, viewHeight);
    vec3 WorldDir = vec3(0.0f, sqrt(1.0 - viewZenithCosAngle * viewZenithCosAngle), viewZenithCosAngle);

    const float SampleCountIni = 40.0f;
    const bool VariableSampleCount = false;
    vec3 transmittance = exp(-IntegrateOpticalDepth(WorldPos, WorldDir, Atmosphere, SampleCountIni, VariableSampleCount));

    imageStore(imgTransmittance, texelCoord, vec4(transmittance, 1.0f));
}
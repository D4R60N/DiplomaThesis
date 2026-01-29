#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 1, local_size_y = 1, local_size_z = 64) in;

layout(rgba32f, binding = 0) uniform writeonly image2D imgMultipleScattering;
uniform sampler2D transmittanceTexture;
uniform sampler2D multiScatteringTexture;


uniform ivec2 uSkyViewTextureSize;
uniform ivec2 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform mat4 invViewProj;
uniform vec3 sunDirection;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 sunIlluminance;
uniform float MultipleScatteringFactor = 1.0;

shared vec3 MultiScatAs1SharedMem[64];
shared vec3 LSharedMem[64];
#define MULTISCATAPPROX_ENABLED 0
const bool RENDER_SUN_DISK = false;
#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;


void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    uint rayIndex = gl_LocalInvocationID.z;

    if (texelCoord.x >= int(uScatteringTextureSize.x) ||
    texelCoord.y >= int(uScatteringTextureSize.y)) {
        return;
    }
    AtmosphereParameters Atmosphere = uAtmosphere;

    vec2 pixPos = vec2(texelCoord) + 0.5;
    vec2 uv = pixPos / vec2(uScatteringTextureSize);


    uv = vec2(fromSubUvsToUnit(uv.x, MultiScatteringLUTRes), fromSubUvsToUnit(uv.y, MultiScatteringLUTRes));

    float cosSunZenithAngle = uv.x * 2.0 - 1.0;
    vec3 sunDir = vec3(0.0, sqrt(saturate(1.0 - cosSunZenithAngle * cosSunZenithAngle)), cosSunZenithAngle);
    float viewHeight = Atmosphere.BottomRadius + saturate(uv.y + PLANET_RADIUS_OFFSET) * (Atmosphere.TopRadius - Atmosphere.BottomRadius - PLANET_RADIUS_OFFSET);

    vec3 WorldPos = vec3(0.0f, 0.0f, viewHeight);
    vec3 WorldDir = vec3(0.0f, 0.0f, 1.0f);


    const bool ground = true;
    const float SampleCountIni = 20;
    const float DepthBufferValue = -1.0;
    const bool VariableSampleCount = false;
    const bool MieRayPhase = false;

    const float SphereSolidAngle = 4.0 * PI;
    const float IsotropicPhase = 1.0 / SphereSolidAngle;


    #define SQRTSAMPLECOUNT 8
    const float sqrtSample = float(SQRTSAMPLECOUNT);
    float i = 0.5f + float(rayIndex  / SQRTSAMPLECOUNT);
    float j = 0.5f + float(rayIndex  - float((rayIndex / SQRTSAMPLECOUNT)*SQRTSAMPLECOUNT));
    {
        float randA = i / sqrtSample;
        float randB = j / sqrtSample;
        float theta = 2.0f * PI * randA;
        float phi = acos(1.0f - 2.0f * randB);
        float cosPhi = cos(phi);
        float sinPhi = sin(phi);
        float cosTheta = cos(theta);
        float sinTheta = sin(theta);
        WorldDir.x = cosTheta * sinPhi;
        WorldDir.y = sinTheta * sinPhi;
        WorldDir.z = cosPhi;
        SingleScatteringResult result = IntegrateScatteredLuminance(pixPos, WorldPos, WorldDir, sunDir, Atmosphere, ground, SampleCountIni, DepthBufferValue, VariableSampleCount, MieRayPhase, uTransmittanceTextureSize);

        MultiScatAs1SharedMem[rayIndex] = result.MultiScatAs1 * SphereSolidAngle / (sqrtSample * sqrtSample);
        LSharedMem[rayIndex] = result.L * SphereSolidAngle / (sqrtSample * sqrtSample);
    }
    #undef SQRTSAMPLECOUNT

    groupMemoryBarrier();
    barrier();

    if (rayIndex < 32)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 32];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 32];
    }
    groupMemoryBarrier();
    barrier();

    if (rayIndex < 16)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 16];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 16];
    }
    groupMemoryBarrier();
    barrier();

    if (rayIndex < 8)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 8];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 8];
    }
    groupMemoryBarrier();
    barrier();
    if (rayIndex < 4)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 4];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 4];
    }
    groupMemoryBarrier();
    barrier();
    if (rayIndex < 2)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 2];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 2];
    }
    groupMemoryBarrier();
    barrier();
    if (rayIndex < 1)
    {
        MultiScatAs1SharedMem[rayIndex] += MultiScatAs1SharedMem[rayIndex + 1];
        LSharedMem[rayIndex] += LSharedMem[rayIndex + 1];
    }
    groupMemoryBarrier();
    barrier();
    if (rayIndex > 0)
    return;

    vec3 MultiScatAs1            = MultiScatAs1SharedMem[0] * IsotropicPhase;// Equation 7 f_ms
    vec3 InScatteredLuminance    = LSharedMem[0] * IsotropicPhase;// Equation 5 L_2ndOrder


    #if    MULTI_SCATTERING_POWER_SERIE==0
    vec3 MultiScatAs1SQR = MultiScatAs1 * MultiScatAs1;
    vec3 L = InScatteredLuminance * (1.0 + MultiScatAs1 + MultiScatAs1SQR + MultiScatAs1 * MultiScatAs1SQR + MultiScatAs1SQR * MultiScatAs1SQR);
    #else
    const vec3 r = MultiScatAs1;
    const vec3 SumOfAllMultiScatteringEventsContribution = 1.0f / (1.0 - r);
    vec3 L = InScatteredLuminance * SumOfAllMultiScatteringEventsContribution;
    #endif

    imageStore(imgMultipleScattering, texelCoord, vec4(MultipleScatteringFactor * L, 1.0f));
}
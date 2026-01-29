#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 8, local_size_y = 8) in;

layout(rgba32f, binding = 0) uniform writeonly image2D imgTransmittance;
uniform sampler2D transmittanceTexture;
uniform sampler2D multiScatteringTexture;

uniform ivec2 uSkyViewTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform mat4 invViewProj;
uniform vec3 sunDirection;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 sunIlluminance;
#define MULTISCATAPPROX_ENABLED 0
const bool RENDER_SUN_DISK = false;
#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;

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

    const bool ground = false;
    const float SampleCountIni = 40.0f;
    const float DepthBufferValue = -1.0;
    const bool VariableSampleCount = false;
    const bool MieRayPhase = false;
    vec3 transmittance = exp(-IntegrateScatteredLuminance(pixPos, WorldPos, WorldDir, sunDirection, Atmosphere, ground, SampleCountIni, DepthBufferValue, VariableSampleCount, MieRayPhase, uTransmittanceTextureSize).OpticalDepth);

    imageStore(imgTransmittance, texelCoord, vec4(transmittance, 1.0f));
}
#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 8, local_size_y = 8) in;

layout(rgba32f, binding = 0) uniform writeonly image2D imgSkyView;
uniform sampler2D transmittanceTexture;
uniform sampler2D multiScatteringTexture;

uniform ivec2 uSkyViewTextureSize;
uniform ivec2 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform mat4 viewMatInv;
uniform mat4 invViewProj;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 sunIlluminance;
uniform vec3 sunDirection;
uniform vec3 camera;

#define MULTISCATAPPROX_ENABLED 1
const bool RENDER_SUN_DISK = true;
#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uSkyViewTextureSize.x) ||
    texelCoord.y >= int(uSkyViewTextureSize.y)) {
        return;
    }
    AtmosphereParameters Atmosphere = uAtmosphere;

    vec2 pixPos = vec2(texelCoord) + 0.5;
    vec2 uv = pixPos / vec2(uSkyViewTextureSize);
    vec3 camPosKM = camera * 0.001;

    vec3 WorldPos = camPosKM + vec3(0.0, Atmosphere.BottomRadius, 0.0);

    float viewHeight = length(WorldPos);

    float viewZenithCosAngle;
    float lightViewCosAngle;
    UvToSkyViewLutParams(Atmosphere, viewZenithCosAngle, lightViewCosAngle, viewHeight, uv);
    
    vec3 UpVector = WorldPos / viewHeight;
    float sunZenithCosAngle = dot(UpVector, sunDirection);
    vec3 SunDir = normalize(vec3(sqrt(1.0 - sunZenithCosAngle * sunZenithCosAngle), sunZenithCosAngle, 0.0));
    
    WorldPos = vec3(0.0f, viewHeight, 0.0f);

    float viewZenithSinAngle = sqrt(1.0 - viewZenithCosAngle * viewZenithCosAngle);
    vec3 WorldDir = vec3(
    viewZenithSinAngle * lightViewCosAngle,
    viewZenithCosAngle,
    viewZenithSinAngle * sqrt(1.0 - lightViewCosAngle * lightViewCosAngle)
    );


    if (!MoveToTopAtmosphere(WorldPos, WorldDir, Atmosphere.TopRadius))
    {
        imageStore(imgSkyView, texelCoord, vec4(0.0f, 0.0f, 0.0f, 1.0f));
        return;
    }

    const bool ground = false;
    const float SampleCountIni = 30;
    const float DepthBufferValue = -1.0;
    const bool VariableSampleCount = true;
    const bool MieRayPhase = true;
    SingleScatteringResult ss = IntegrateScatteredLuminance(pixPos, WorldPos, WorldDir, SunDir, Atmosphere, ground, SampleCountIni, DepthBufferValue, VariableSampleCount, MieRayPhase, uSkyViewTextureSize);

    vec3 L = ss.L;

    imageStore(imgSkyView, texelCoord, vec4(L, 1.0f));
}
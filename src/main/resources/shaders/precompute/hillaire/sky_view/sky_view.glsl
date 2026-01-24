#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 8, local_size_y = 8) in;

layout(rgba32f, binding = 0) uniform writeonly image2D imgSkyView;
uniform sampler2D u_TransmittanceLutTexture;
uniform sampler2D u_MultipleScatteringLutTexture;

uniform ivec2 uSkyViewTextureSize;
uniform ivec2 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform mat4 gSkyInvViewMat;
uniform mat4 gSkyInvViewProjMat;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 gSunIlluminance;
uniform vec3 sunDirection = vec3(0.0, 0.0, 1.0);
uniform vec3 camera = vec3(10.0, 10.0, 10.0);

#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);
    uint rayIndex = gl_LocalInvocationID.z;

    if (texelCoord.x >= int(uSkyViewTextureSize.x) ||
    texelCoord.y >= int(uSkyViewTextureSize.y)) {
        return;
    }
    AtmosphereParameters Atmosphere = uAtmosphere;

    vec2 pixPos = vec2(texelCoord) + 0.5;
    vec2 uv = pixPos / vec2(uSkyViewTextureSize);

    vec3 WorldPos = camera + vec3(0, 0, Atmosphere.BottomRadius);

    float viewHeight = length(WorldPos);

    float viewZenithCosAngle;
    float lightViewCosAngle;
    UvToSkyViewLutParams(Atmosphere, viewZenithCosAngle, lightViewCosAngle, viewHeight, uv);
    
    vec3 UpVector = WorldPos / viewHeight;
    float sunZenithCosAngle = dot(UpVector, sunDirection);
    vec3 SunDir = normalize(vec3(sqrt(1.0 - sunZenithCosAngle * sunZenithCosAngle), 0.0, sunZenithCosAngle));
    
    WorldPos = vec3(0.0f, 0.0f, viewHeight);

    float viewZenithSinAngle = sqrt(1 - viewZenithCosAngle * viewZenithCosAngle);
    vec3 WorldDir = vec3(
    viewZenithSinAngle * lightViewCosAngle,
    viewZenithSinAngle * sqrt(1.0 - lightViewCosAngle * lightViewCosAngle),
    viewZenithCosAngle);


    if (!MoveToTopAtmosphere(WorldPos, WorldDir, Atmosphere.TopRadius))
    {
        imageStore(imgSkyView, texelCoord, vec4(0.0f, 0.0f, 0.0f, 1.0f));
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
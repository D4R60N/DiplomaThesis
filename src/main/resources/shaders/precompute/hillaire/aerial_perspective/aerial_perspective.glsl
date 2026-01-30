#version 450
#extension GL_ARB_shading_language_include : require
layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;

layout(rgba32f, binding = 0) uniform writeonly image3D imgArialPerspective;
layout(binding = 1) uniform sampler2D transmittanceTexture;
layout(binding = 2) uniform sampler2D multiScatteringTexture;

uniform ivec2 uSkyViewTextureSize;
uniform ivec3 uAerialPerspectiveTextureSize;
uniform ivec2 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform mat4 viewMatInv;
uniform mat4 projMatInv;
uniform mat4 invViewProj;
uniform vec2 RayMarchMinMaxSPP;
uniform vec3 sunIlluminance;
uniform vec3 sunDirection;
uniform vec3 camera;

#define MULTISCATAPPROX_ENABLED 1
const bool RENDER_SUN_DISK = true;
#include "/common.glsl"
uniform AtmosphereParameters uAtmosphere;

#define AP_SLICE_COUNT uAerialPerspectiveTextureSize.z
#define AP_KM_PER_SLICE 4.0

float AerialPerspectiveSliceToDepth(float slice) {
    return slice * AP_KM_PER_SLICE;
}

void main() {
    ivec3 texelCoord = ivec3(gl_GlobalInvocationID.xyz);

    if (texelCoord.x >= uAerialPerspectiveTextureSize.x ||
    texelCoord.y >= uAerialPerspectiveTextureSize.y ||
    texelCoord.z >= uAerialPerspectiveTextureSize.z) {
        return;
    }
    AtmosphereParameters Atmosphere = uAtmosphere;

    vec2 pixPos = vec2(texelCoord.xy) + 0.5;
    float zIdx = float(texelCoord.z);
    vec2 uv = pixPos / vec2(uAerialPerspectiveTextureSize.xy);
    vec3 camPosKM = camera * 0.001;

    vec3 ClipSpace = vec3(uv * vec2(2.0, 2.0) - vec2(1.0, 1.0), 1.0);
    vec4 HViewPos = projMatInv * vec4(ClipSpace, 1.0);
    vec3 WorldDirViewSpace = HViewPos.xyz / HViewPos.w;
    vec3 WorldDir = normalize(mat3(viewMatInv) * WorldDirViewSpace);

    float earthR = Atmosphere.BottomRadius;
    vec3 camPos = camPosKM + vec3(0, earthR, 0);
    vec3 SunDir = sunDirection;
    vec3 SunLuminance = vec3(0.0f);

    float Slice = ((zIdx + 0.5f) / AP_SLICE_COUNT);
    Slice *= Slice;
    Slice *= AP_SLICE_COUNT;

    vec3 WorldPos = camPos;
    float viewHeight;


    // Compute position from froxel information
    float tMax = AerialPerspectiveSliceToDepth(Slice);
    vec3 newWorldPos = WorldPos + tMax * WorldDir;


    viewHeight = length(newWorldPos);
    if (viewHeight <= (Atmosphere.BottomRadius + PLANET_RADIUS_OFFSET))
    {
        newWorldPos = normalize(newWorldPos) * (Atmosphere.BottomRadius + PLANET_RADIUS_OFFSET + 0.001f);
        WorldDir = normalize(newWorldPos - camPos);
        tMax = length(newWorldPos - camPos);
    }
    float tMaxMax = tMax;

    viewHeight = length(WorldPos);
    if (viewHeight >= Atmosphere.TopRadius)
    {
        vec3 prevWorlPos = WorldPos;
        if (!MoveToTopAtmosphere(WorldPos, WorldDir, Atmosphere.TopRadius))
        {
            imageStore(imgArialPerspective, texelCoord, vec4(0.0, 0.0, 0.0, 1.0));
            return;
        }
        float LengthToAtmosphere = length(prevWorlPos - WorldPos);
        if (tMaxMax < LengthToAtmosphere)
        {
            imageStore(imgArialPerspective, texelCoord, vec4(0.0, 0.0, 0.0, 1.0));
            return;
        }
        tMaxMax = max(0.0, tMaxMax - LengthToAtmosphere);
    }


    const bool ground = false;
    const float SampleCountIni = max(1.0, float(zIdx + 1.0) * 2.0f);
    const float DepthBufferValue = -1.0;
    const bool VariableSampleCount = false;
    const bool MieRayPhase = true;
    SingleScatteringResult ss = IntegrateScatteredLuminance(pixPos, WorldPos, WorldDir, SunDir, Atmosphere, ground, SampleCountIni, DepthBufferValue, VariableSampleCount, MieRayPhase, tMaxMax, uAerialPerspectiveTextureSize.xy);


    const float Transmittance = dot(ss.Transmittance, vec3(1.0f / 3.0f, 1.0f / 3.0f, 1.0f / 3.0f));

    imageStore(imgArialPerspective, texelCoord, vec4(ss.L, 1.0 - Transmittance));
}
#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec4 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#define SCATTERING_TEXTURE_MU_S_SIZE uScatteringTextureSize.x
#define SCATTERING_TEXTURE_MU_SIZE  uScatteringTextureSize.y
#define SCATTERING_TEXTURE_R_SIZE uScatteringTextureSize.z
#define SCATTERING_TEXTURE_NU_SIZE uScatteringTextureSize.w

layout(local_size_x = 8, local_size_y = 8, local_size_z = 8) in;
layout(binding = 0) uniform sampler2D  transmittanceSampler;
layout(binding = 1) uniform sampler3D scatteringDensitySampler;
layout(rgba32f, binding = 2) uniform image3D scatteringImage;
layout(rgba32f, binding = 3) uniform image3D accumulativeScatteringImage;


#include "/definitions.glsl"
#include "/single_scattering/functions.glsl"
#include "/multiple_scattering/functions.glsl"

uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec3 texelCoord = ivec3(gl_GlobalInvocationID.xyz);
    float width = uScatteringTextureSize.x*uScatteringTextureSize.w;
    if (texelCoord.x >= width ||
    texelCoord.y >= uScatteringTextureSize.y ||
    texelCoord.z >= uScatteringTextureSize.z) {
        return;
    }
    float nu;
    vec3 delta_multiple_scattering = ComputeMultipleScatteringTexture(uAtmosphere, transmittanceSampler, scatteringDensitySampler, vec3(texelCoord) + 0.5, nu);

    vec3 delta = delta_multiple_scattering.rgb / RayleighPhaseFunction(nu);
    vec3 previousScattering = imageLoad(accumulativeScatteringImage, texelCoord).xyz;

    imageStore(scatteringImage, texelCoord, vec4(delta, 1));
    imageStore(accumulativeScatteringImage, texelCoord, vec4(delta + previousScattering, 1));
}
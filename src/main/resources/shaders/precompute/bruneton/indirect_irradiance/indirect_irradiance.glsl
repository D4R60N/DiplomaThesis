#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec2 uTransmittanceTextureSize;
uniform ivec2 uIrradianceTextureSize;
uniform ivec4 uScatteringTextureSize;
uniform int order;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#define SCATTERING_TEXTURE_MU_S_SIZE uScatteringTextureSize.x
#define SCATTERING_TEXTURE_MU_SIZE  uScatteringTextureSize.y
#define SCATTERING_TEXTURE_R_SIZE uScatteringTextureSize.z
#define SCATTERING_TEXTURE_NU_SIZE uScatteringTextureSize.w
#define IRRADIANCE_TEXTURE_WIDTH uIrradianceTextureSize.x
#define IRRADIANCE_TEXTURE_HEIGHT uIrradianceTextureSize.y

layout(local_size_x = 8, local_size_y = 8, local_size_z = 8) in;
layout(binding = 0) uniform sampler3D singleRayleighScatteringSampler;
layout(binding = 1) uniform sampler3D singleMieScatteringSampler;
layout(binding = 2) uniform sampler3D multipleScatteringSampler;
layout(rgba32f, binding = 3) uniform image2D indirectIrradianceImage;
layout(binding = 4) uniform sampler2D  transmittanceSampler;
layout(rgba32f, binding = 5) uniform image2D irradianceImage;

#include "/definitions.glsl"
#include "/single_scattering/functions.glsl"
#include "/indirect_irradiance/functions.glsl"

uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uIrradianceTextureSize.x) ||
    texelCoord.y >= int(uIrradianceTextureSize.y)) {
        return;
    }

    vec3 irradiance = ComputeIndirectIrradianceTexture(uAtmosphere, vec2(texelCoord) + 0.5, order);

    vec3 previousIrradiance = imageLoad(irradianceImage, texelCoord).xyz;

    imageStore(indirectIrradianceImage, texelCoord, vec4(irradiance, 1));
    imageStore(irradianceImage, texelCoord, vec4(irradiance + previousIrradiance, 1));
}
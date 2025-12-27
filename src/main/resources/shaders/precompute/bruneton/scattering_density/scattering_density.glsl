#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec4 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform ivec2 uIrradianceTextureSize;
uniform int order;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#define IRRADIANCE_TEXTURE_WIDTH uIrradianceTextureSize.x
#define IRRADIANCE_TEXTURE_HEIGHT uIrradianceTextureSize.y
#define SCATTERING_TEXTURE_MU_SIZE  uScatteringTextureSize.x
#define SCATTERING_TEXTURE_MU_S_SIZE uScatteringTextureSize.y
#define SCATTERING_TEXTURE_R_SIZE uScatteringTextureSize.z
#define SCATTERING_TEXTURE_NU_SIZE uScatteringTextureSize.w

layout(local_size_x = 8, local_size_y = 8, local_size_z = 8) in;
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;
layout(rgba32f, binding = 1) uniform image3D singleScatteringRayleighImage;
layout(rgba32f, binding = 2) uniform image3D singleScatteringMieImage;
layout(rgba32f, binding = 3) uniform image3D scatteringImage;
layout(rgba32f, binding = 4) uniform image3D irradianceImage;
layout(rgba32f, binding = 5) uniform image3D scatteringDensityImage;

#include "/definitions.glsl"
#include "/single_scattering/functions.glsl"
#include "/scattering_density/functions.glsl"

uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec3 texelCoord = ivec3(gl_GlobalInvocationID.xyz);
    float width = uScatteringTextureSize.x*uScatteringTextureSize.w;
    if (texelCoord.x >= width ||
    texelCoord.y >= uScatteringTextureSize.y ||
    texelCoord.z >= uScatteringTextureSize.z) {
        return;
    }
    vec3 density = ComputeScatteringDensityTexture(uAtmosphere, texelCoord, order);

    imageStore(scatteringDensityImage, texelCoord, vec4(density, 1));
}
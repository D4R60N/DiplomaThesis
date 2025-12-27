#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec2 uTransmittanceTextureSize;
uniform ivec2 uIrradianceTextureSize;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#define IRRADIANCE_TEXTURE_WIDTH uIrradianceTextureSize.x
#define IRRADIANCE_TEXTURE_HEIGHT uIrradianceTextureSize.y

layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;
layout(rgba32f, binding = 1) uniform image2D directIrradianceImage;

#include "/definitions.glsl"
#include "/direct_irradiance/functions.glsl"

uniform AtmosphereParameters uAtmosphere;

void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uIrradianceTextureSize.x) ||
    texelCoord.y >= int(uIrradianceTextureSize.y)) {
        return;
    }

    vec3 irradiance = ComputeDirectIrradianceTexture(uAtmosphere, texelCoord);

    imageStore(directIrradianceImage, texelCoord, vec4(irradiance, 1));
}
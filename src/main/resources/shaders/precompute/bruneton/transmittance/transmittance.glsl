#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec2 uTransmittanceTextureSize;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#include "/definitions.glsl"
#include "/transmittance/functions.glsl"

layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;

uniform AtmosphereParameters uAtmosphere;


void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uTransmittanceTextureSize.x) ||
    texelCoord.y >= int(uTransmittanceTextureSize.y)) {
        return;
    }

    vec3 transmittance = ComputeTransmittanceToTopAtmosphereBoundaryTexture(uAtmosphere, vec2(texelCoord) + 0.5);

    imageStore(transmittanceImage, texelCoord, vec4(transmittance, 1.0));
}
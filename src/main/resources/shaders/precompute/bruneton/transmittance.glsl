#version 450
#extension GL_ARB_shading_language_include : require
uniform ivec2 uTransmittanceTextureSize;
const int TRANSMITTANCE_TEXTURE_HEIGHT = uTransmittanceTextureSize.y;
const int TRANSMITTANCE_TEXTURE_WIDTH = uTransmittanceTextureSize.x;
#include "/definitions.glsl"
#include "/functions.glsl"

layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;

uniform AtmosphereParameters uAtmosphere;


void main() {
    ivec2 texelCoord = ivec2(gl_GlobalInvocationID.xy);

    if (texelCoord.x >= int(uTransmittanceTextureSize.x) ||
    texelCoord.y >= int(uTransmittanceTextureSize.y)) {
        return;
    }

    vec2 fragCoord = (vec2(texelCoord) + 0.5) / uTransmittanceTextureSize;

    vec3 transmittance = ComputeTransmittanceToTopAtmosphereBoundaryTexture(uAtmosphere, fragCoord);

    imageStore(transmittanceImage, texelCoord, vec4(transmittance, 1.0));
}
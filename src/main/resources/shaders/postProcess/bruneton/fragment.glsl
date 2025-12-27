#version 450
#extension GL_ARB_shading_language_include : require
in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2D depthSampler;
uniform sampler2D transmittanceTexture;
uniform sampler2D irradianceTexture;
uniform sampler3D scatteringTexture;
uniform sampler3D singleMieScatteringTexture;

#include "/definitions.glsl"


uniform AtmosphereParameters uAtmosphere;

void main() {
    fragColor = texture(irradianceTexture, fragTexCoord);
}

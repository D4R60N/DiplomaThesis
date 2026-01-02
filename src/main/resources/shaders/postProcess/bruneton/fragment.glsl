#version 450
#extension GL_ARB_shading_language_include : require
in vec3 view_ray;
in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D textureSampler;
uniform sampler2D depthSampler;
uniform sampler2D transmittanceTexture;
uniform sampler2D irradianceTexture;
uniform sampler3D scatteringTexture;
uniform sampler3D singleMieScatteringTexture;

uniform ivec4 uScatteringTextureSize;
uniform ivec2 uTransmittanceTextureSize;
uniform ivec2 uIrradianceTextureSize;
#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
#define SCATTERING_TEXTURE_MU_SIZE  uScatteringTextureSize.x
#define SCATTERING_TEXTURE_MU_S_SIZE uScatteringTextureSize.y
#define SCATTERING_TEXTURE_R_SIZE uScatteringTextureSize.z
#define SCATTERING_TEXTURE_NU_SIZE uScatteringTextureSize.w
#define IRRADIANCE_TEXTURE_WIDTH uIrradianceTextureSize.x
#define IRRADIANCE_TEXTURE_HEIGHT uIrradianceTextureSize.y

uniform vec3 camPos;
uniform float exposure;
uniform vec3 white_point;
uniform vec3 earth_center;
uniform vec3 sun_direction;
uniform vec2 sun_size;

#include "/definitions.glsl"
#include "/functions.glsl"

const vec3 SKY_SPECTRAL_RADIANCE_TO_LUMINANCE = vec3(114974.916437,71305.954816,65310.548555);
const vec3 SUN_SPECTRAL_RADIANCE_TO_LUMINANCE = vec3(98242.786222,69954.398112,66475.012354);

uniform AtmosphereParameters uAtmosphere;


vec3 GetSolarRadiance() {
    return uAtmosphere.solar_irradiance /
    (PI * uAtmosphere.sun_angular_radius * uAtmosphere.sun_angular_radius);
}
vec3 GetSkyRadiance(vec3 camera, vec3 view_ray, float shadow_length, vec3 sun_direction, out vec3 transmittance) {
    return GetSkyRadiance(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture, camera, view_ray, shadow_length, sun_direction, transmittance);
}
vec3 GetSkyRadianceToPoint(vec3 camera, vec3 point, float shadow_length, vec3 sun_direction, out vec3 transmittance) {
    return GetSkyRadianceToPoint(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture, camera, point, shadow_length, sun_direction, transmittance);
}
vec3 GetSunAndSkyIrradiance(vec3 p, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance) {
    return GetSunAndSkyIrradiance(uAtmosphere, transmittanceTexture, irradianceTexture, p, normal, sun_direction, sky_irradiance);
}


vec3 GetSolarLuminance() {
    return uAtmosphere.solar_irradiance /
    (PI * uAtmosphere.sun_angular_radius * uAtmosphere.sun_angular_radius) *
    SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}
vec3 GetSkyLuminance(vec3 camera, vec3 view_ray, Length shadow_length, vec3 sun_direction, out DimensionlessSpectrum transmittance) {
    return GetSkyRadiance(uAtmosphere, transmittanceTexture,
    scatteringTexture, singleMieScatteringTexture,
    camera, view_ray, shadow_length, sun_direction, transmittance) *
    SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}
vec3 GetSkyLuminanceToPoint(vec3 camera, vec3 point, Length shadow_length, vec3 sun_direction, out DimensionlessSpectrum transmittance) {
    return GetSkyRadianceToPoint(uAtmosphere, transmittanceTexture,
    scatteringTexture, singleMieScatteringTexture,
    camera, point, shadow_length, sun_direction, transmittance) *
    SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
}
vec3 GetSunAndSkyIlluminance(Position p, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance) {
    vec3 sun_irradiance = GetSunAndSkyIrradiance(
    uAtmosphere, transmittanceTexture, irradianceTexture, p, normal,
    sun_direction, sky_irradiance);
    sky_irradiance *= SKY_SPECTRAL_RADIANCE_TO_LUMINANCE;
    return sun_irradiance * SUN_SPECTRAL_RADIANCE_TO_LUMINANCE;
}

void main() {
    vec3 viewDir = normalize(view_ray);

    vec4 sceneSample = texture(textureSampler, fragTexCoord);
    vec3 sceneColor = sceneSample.rgb;
    float depth = texture(depthSampler, fragTexCoord).r;

    vec3 transmittance;
    vec3 radiance = vec3(0.0);
    float shadowLength = 0.0;
    vec3 camera = camPos/1000;


    if (depth >= 1) {
        radiance = GetSkyRadiance(camera - earth_center, viewDir, shadowLength, sun_direction, transmittance);

        if (dot(viewDir, sun_direction) > sun_size.y) {
            radiance += transmittance * GetSolarRadiance();
        }

        radiance = pow(vec3(1.0) - exp(-radiance / white_point * exposure), vec3(1.0 / 2.2));
    } else {
        radiance = sceneColor;
    }

    vec3 color = radiance;

    fragColor = vec4(color, 1.0);
}


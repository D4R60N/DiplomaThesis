#version 450
#extension GL_ARB_shading_language_include : require
in vec3 view_ray;
in vec2 fragTexCoord;
layout(location = 0) out vec4 color;

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
uniform float exposure = 10.0;
uniform vec3 white_point = vec3(1.0, 1.0, 1.0);
uniform vec3 earth_center = vec3(0.0, -6360.0, 0.0);
uniform vec3 sun_direction = vec3(0.0, 1.0, 0.0);
uniform vec2 sun_size = vec2(0.00465, 0.00465);

#include "/definitions.glsl"
#include "/functions.glsl"

const vec3 SUN_SPECTRAL_RADIANCE_TO_LUMINANCE = vec3(1);
const vec3 SKY_SPECTRAL_RADIANCE_TO_LUMINANCE = vec3(1);

const vec3 kSphereCenter = vec3(0.0, 0.0, 1.0);
const float kSphereRadius = 1.0;
const vec3 kSphereAlbedo = vec3(0.8);
const vec3 kGroundAlbedo = vec3(0.0, 0.0, 0.04);

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

float GetSunVisibility(vec3 point, vec3 sun_direction) {
    vec3 p = point - kSphereCenter;
    float p_dot_v = dot(p, sun_direction);
    float p_dot_p = dot(p, p);
    float ray_sphere_center_squared_distance = p_dot_p - p_dot_v * p_dot_v;
    float discriminant =
    kSphereRadius * kSphereRadius - ray_sphere_center_squared_distance;
    if (discriminant >= 0.0) {
        float distance_to_intersection = -p_dot_v - sqrt(discriminant);
        if (distance_to_intersection > 0.0) {
            float ray_sphere_distance =
            kSphereRadius - sqrt(ray_sphere_center_squared_distance);
            float ray_sphere_angular_distance = -ray_sphere_distance / p_dot_v;
            return smoothstep(1.0, 0.0, ray_sphere_angular_distance / sun_size.x);
        }
    }
    return 1.0;
}

float GetSkyVisibility(vec3 point) {
    vec3 p = point - kSphereCenter;
    float p_dot_p = dot(p, p);
    return
    1.0 + p.z / sqrt(p_dot_p) * kSphereRadius * kSphereRadius / p_dot_p;
}

void GetSphereShadowInOut(vec3 view_direction, vec3 sun_direction,
out float d_in, out float d_out) {
    vec3 pos = camPos - kSphereCenter;
    float pos_dot_sun = dot(pos, sun_direction);
    float view_dot_sun = dot(view_direction, sun_direction);
    float k = sun_size.x;
    float l = 1.0 + k * k;
    float a = 1.0 - l * view_dot_sun * view_dot_sun;
    float b = dot(pos, view_direction) - l * pos_dot_sun * view_dot_sun -
    k * kSphereRadius * view_dot_sun;
    float c = dot(pos, pos) - l * pos_dot_sun * pos_dot_sun -
    2.0 * k * kSphereRadius * pos_dot_sun - kSphereRadius * kSphereRadius;
    float discriminant = b * b - a * c;
    if (discriminant > 0.0) {
        d_in = max(0.0, (-b - sqrt(discriminant)) / a);
        d_out = (-b + sqrt(discriminant)) / a;
        // The values of d for which delta is equal to 0 and kSphereRadius / k.
        float d_base = -pos_dot_sun / view_dot_sun;
        float d_apex = -(pos_dot_sun + kSphereRadius / k) / view_dot_sun;
        if (view_dot_sun > 0.0) {
            d_in = max(d_in, d_apex);
            d_out = a > 0.0 ? min(d_out, d_base) : d_base;
        } else {
            d_in = a > 0.0 ? max(d_in, d_base) : d_base;
            d_out = min(d_out, d_apex);
        }
    } else {
        d_in = 0.0;
        d_out = 0.0;
    }
}

void main() {
    vec3 view_direction = normalize(view_ray);
    float fragment_angular_size =
    length(dFdx(view_ray) + dFdy(view_ray)) / length(view_ray);

    float shadow_in;
    float shadow_out;
    GetSphereShadowInOut(view_direction, sun_direction, shadow_in, shadow_out);

    float lightshaft_fadein_hack = smoothstep(
    0.02, 0.04, dot(normalize(camPos - earth_center), sun_direction));

    vec3 p = camPos - kSphereCenter;
    float p_dot_v = dot(p, view_direction);
    float p_dot_p = dot(p, p);
    float ray_sphere_center_squared_distance = p_dot_p - p_dot_v * p_dot_v;
    float discriminant = kSphereRadius * kSphereRadius -ray_sphere_center_squared_distance;

    float sphere_alpha = 0.0;
    vec3 sphere_radiance = vec3(0.0);
    if (discriminant >= 0.0) {
        float distance_to_intersection = -p_dot_v - sqrt(discriminant);
        if (distance_to_intersection > 0.0) {
            float ray_sphere_distance = kSphereRadius - sqrt(ray_sphere_center_squared_distance);
            float ray_sphere_angular_distance = -ray_sphere_distance / p_dot_v;
            sphere_alpha = min(ray_sphere_angular_distance / fragment_angular_size, 1.0);

            vec3 point = camPos + view_direction * distance_to_intersection;
            vec3 normal = normalize(point - kSphereCenter);

            vec3 sky_irradiance;
            vec3 sun_irradiance = GetSunAndSkyIrradiance(point - earth_center, normal, sun_direction, sky_irradiance);
            sphere_radiance = kSphereAlbedo * (1.0 / PI) * (sun_irradiance + sky_irradiance);
            float shadow_length =
            max(0.0, min(shadow_out, distance_to_intersection) - shadow_in) * lightshaft_fadein_hack;
            vec3 transmittance;
            vec3 in_scatter = GetSkyRadianceToPoint(camPos - earth_center,
            point - earth_center, shadow_length, sun_direction, transmittance);
            sphere_radiance = sphere_radiance * transmittance + in_scatter;
        }
    }
    p = camPos - earth_center;
    p_dot_v = dot(p, view_direction);
    p_dot_p = dot(p, p);
    float ray_earth_center_squared_distance = p_dot_p - p_dot_v * p_dot_v;
    discriminant = earth_center.z * earth_center.z - ray_earth_center_squared_distance;

    float ground_alpha = 0.0;
    vec3 ground_radiance = vec3(0.0);
    if (discriminant >= 0.0) {
        float distance_to_intersection = -p_dot_v - sqrt(discriminant);
        if (distance_to_intersection > 0.0) {
            vec3 point = camPos + view_direction * distance_to_intersection;
            vec3 normal = normalize(point - earth_center);

            vec3 sky_irradiance;
            vec3 sun_irradiance = GetSunAndSkyIrradiance(
            point - earth_center, normal, sun_direction, sky_irradiance);
            ground_radiance = kGroundAlbedo * (1.0 / PI) * (
            sun_irradiance * GetSunVisibility(point, sun_direction) +
            sky_irradiance * GetSkyVisibility(point));

            float shadow_length =
            max(0.0, min(shadow_out, distance_to_intersection) - shadow_in) *
            lightshaft_fadein_hack;
            vec3 transmittance;
            vec3 in_scatter = GetSkyRadianceToPoint(camPos - earth_center,
            point - earth_center, shadow_length, sun_direction, transmittance);
            ground_radiance = ground_radiance * transmittance + in_scatter;
            ground_alpha = 1.0;
        }
    }
    float shadow_length = max(0.0, shadow_out - shadow_in) * lightshaft_fadein_hack;
    vec3 transmittance;
    vec3 radiance = GetSkyRadiance(camPos - earth_center, view_direction, shadow_length, sun_direction, transmittance);

    if (dot(view_direction, sun_direction) > sun_size.y) {
        radiance = radiance + transmittance * GetSolarRadiance();
    }
    radiance = mix(radiance, ground_radiance, ground_alpha);
    radiance = mix(radiance, sphere_radiance, sphere_alpha);
    color.rgb =pow(vec3(1.0) - exp(-radiance / white_point * exposure), vec3(1.0 / 2.2));
    color.a = 1.0;
}


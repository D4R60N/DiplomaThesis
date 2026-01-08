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

        float dotViewSun = dot(viewDir, sun_direction);
        float sunAlpha = smoothstep(sun_size.y - 0.0001, sun_size.y + 0.0001, dotViewSun);
        if (sunAlpha > 0.0) {
            radiance += sunAlpha * transmittance * GetSolarRadiance();
        }
        radiance = pow(vec3(1.0) - exp(-radiance / white_point * exposure), vec3(1.0 / 2.2));
    } else {
        radiance = sceneColor;
    }

    vec3 color = radiance;

    fragColor = vec4(color, 1.0);
}

//#version 450
//#extension GL_ARB_shading_language_include : require
//in vec3 view_ray;
//in vec2 fragTexCoord;
//out vec4 fragColor;
//
//uniform sampler2D textureSampler;
//uniform sampler2D depthSampler;
//uniform sampler2D transmittanceTexture;
//uniform sampler2D irradianceTexture;
//uniform sampler3D scatteringTexture;
//uniform sampler3D singleMieScatteringTexture;
//
//uniform ivec4 uScatteringTextureSize;
//uniform ivec2 uTransmittanceTextureSize;
//uniform ivec2 uIrradianceTextureSize;
//#define TRANSMITTANCE_TEXTURE_WIDTH  uTransmittanceTextureSize.x
//#define TRANSMITTANCE_TEXTURE_HEIGHT uTransmittanceTextureSize.y
//#define SCATTERING_TEXTURE_MU_SIZE  uScatteringTextureSize.x
//#define SCATTERING_TEXTURE_MU_S_SIZE uScatteringTextureSize.y
//#define SCATTERING_TEXTURE_R_SIZE uScatteringTextureSize.z
//#define SCATTERING_TEXTURE_NU_SIZE uScatteringTextureSize.w
//#define IRRADIANCE_TEXTURE_WIDTH uIrradianceTextureSize.x
//#define IRRADIANCE_TEXTURE_HEIGHT uIrradianceTextureSize.y
//
//uniform vec3 camPos;
//uniform float exposure;
//uniform vec3 white_point;
//uniform vec3 earth_center;
//uniform vec3 sun_direction;
//uniform vec2 sun_size;
//uniform mat4 invViewProj;
//
//#include "/definitions.glsl"
//#include "/functions.glsl"
//
//uniform AtmosphereParameters uAtmosphere;
//
//
//vec3 GetSolarRadiance() {
//    return uAtmosphere.solar_irradiance /
//    (PI * uAtmosphere.sun_angular_radius * uAtmosphere.sun_angular_radius);
//}
//vec3 GetSkyRadiance(vec3 camera, vec3 view_ray, float shadow_length, vec3 sun_direction, out vec3 transmittance) {
//    return GetSkyRadiance(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture, camera, view_ray, shadow_length, sun_direction, transmittance);
//}
//vec3 GetSkyRadianceToPoint(vec3 camera, vec3 point, float shadow_length, vec3 sun_direction, out vec3 transmittance) {
//    return GetSkyRadianceToPoint(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture, camera, point, shadow_length, sun_direction, transmittance);
//}
//vec3 GetSunAndSkyIrradiance(vec3 p, vec3 normal, vec3 sun_direction, out vec3 sky_irradiance) {
//    return GetSunAndSkyIrradiance(uAtmosphere, transmittanceTexture, irradianceTexture, p, normal, sun_direction, sky_irradiance);
//}
//vec3 WorldPosFromDepth(float depth) {
//    vec4 clip = vec4(fragTexCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
//    vec4 world = invViewProj * clip;
//    return world.xyz / world.w;
//}
//void main() {
//    vec3 viewDir = normalize(view_ray);
//    float depth = texture(depthSampler, fragTexCoord).r;
//    vec3 sceneColor = texture(textureSampler, fragTexCoord).rgb;
//
//    // Convert camera to Kilometers
//    vec3 camera_km = (camPos / 1000.0) - earth_center;
//
//    vec3 transmittance;
//    vec3 inscatteredRadiance = vec3(0.0);
//
//    if (depth >= 1.0) {
//        // --- SKY RENDERING ---
//        inscatteredRadiance = GetSkyRadiance(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture,
//        camera_km, viewDir, 0.0, sun_direction, transmittance);
//
//        // Render Sun Disk
//        float dotViewSun = dot(viewDir, sun_direction);
//        // Soften the edge of the sun disk for better anti-aliasing
//        float sunAlpha = smoothstep(sun_size.y - 0.0001, sun_size.y + 0.0001, dotViewSun);
//        if (sunAlpha > 0.0) {
//            inscatteredRadiance += sunAlpha * transmittance * GetSolarRadiance();
//        }
//    } else {
//        // --- AERIAL PERSPECTIVE (FOGGING) ---
//        // 1. Get world position of the object
//        vec3 worldPos = WorldPosFromDepth(depth);
//        vec3 worldPos_km = (worldPos / 1000.0) - earth_center;
//
//        // 2. Compute scattering between camera and object
//        inscatteredRadiance = GetSkyRadianceToPoint(uAtmosphere, transmittanceTexture, scatteringTexture, singleMieScatteringTexture,
//        camera_km, worldPos_km, 0.0, sun_direction, transmittance);
//
//        // 3. Apply aerial perspective: scene * visibility + atmosphere
//        // Scene color is usually already tone-mapped or in LDR;
//        // ideally sceneColor should be in HDR for this math to be 100% correct.
//        sceneColor = sceneColor * transmittance + inscatteredRadiance;
//        inscatteredRadiance = sceneColor;
//    }
//
//    // --- TONE MAPPING & GAMMA ---
//    // Physical Exposure
//    vec3 finalColor = inscatteredRadiance * exposure;
//
//    // Reinhard or Exponential Tone Mapping
//    finalColor = vec3(1.0) - exp(-finalColor / white_point);
//
//    // Gamma correction
//    finalColor = pow(finalColor, vec3(1.0 / 2.2));
//
//    fragColor = vec4(finalColor, 1.0);
//}
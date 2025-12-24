#version 450
layout(local_size_x = 8, local_size_y = 8, local_size_z = 8) in;
layout(binding = 0) uniform sampler2D transmittanceImage;
layout(rgba32f, binding = 1) uniform image3D singleScatteringImage;

struct AtmosphereParameters {
    float bottom_radius;
    float top_radius;
    vec3 rayleigh_scattering;
    float rayleigh_scale_height;
    float mie_scattering;
    float mie_extinction;
    float mie_scale_height;
    float mie_phase_function_g;
};

uniform AtmosphereParameters uAtmosphere;
uniform vec3 uSunDirection;

// --- BRUNETON PHASE FUNCTIONS ---

float RayleighPhase(float cos_theta) {
    return 3.0 / (16.0 * 3.14159265359) * (1.0 + cos_theta * cos_theta);
}

float MiePhase(float cos_theta, float g) {
    float g2 = g * g;
    float denom = 1.0 + g2 - 2.0 * g * cos_theta;
    return 3.0 / (8.0 * 3.14159265359) * ((1.0 - g2) * (1.0 + cos_theta * cos_theta)) / ((2.0 + g2) * pow(denom, 1.5));
}

// --- UTILS ---

// Helper to sample our previously computed Transmittance LUT
vec3 GetTransmittance(float r, float mu) {
    // You would use the inverse of GetRMuFromTransmittanceTextureUv here
    // to find the correct UV coordinate for the LUT.
    // (Mapping logic omitted for brevity, usually found in Bruneton's 'GetTransmittanceUV')
    return texture(transmittance_sampler, vec2(mu, r)).rgb;
}


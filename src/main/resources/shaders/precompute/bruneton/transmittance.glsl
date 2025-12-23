#version 450
layout(local_size_x = 8, local_size_y = 8, local_size_z = 1) in;
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;

const float PI = 3.14159265359;

struct AtmosphereParameters {
    float bottom_radius;
    float top_radius;
    vec3 rayleigh_scattering;
    float rayleigh_scale_height;
    float mie_scattering;
    float mie_extinction;
    float mie_scale_height;
};

uniform AtmosphereParameters uAtmosphere;
uniform vec2 uTransmittanceTextureSize;

// --- POMOCNÉ FUNKCE -----------------------------------------------------

float DistanceToTopAtmosphereBoundary(float r, float mu) {
    float discriminant = r * r * (mu * mu - 1.0) +
    ATMOSPHERE.top_radius * ATMOSPHERE.top_radius;
    return max(-r * mu + sqrt(max(discriminant, 0.0)), 0.0);
}

vec2 GetDensityProfile(float altitude) {
    return vec2(
    exp(-altitude / ATMOSPHERE.rayleigh_scale_height),
    exp(-altitude / ATMOSPHERE.mie_scale_height)
    );
}

void GetRMuFromTransmittanceTextureUv(in vec2 uv, out float r, out float mu) {
    float x_mu = uv.x;
    float x_r = uv.y;

    float H = sqrt(ATMOSPHERE.top_radius * ATMOSPHERE.top_radius -
    ATMOSPHERE.bottom_radius * ATMOSPHERE.bottom_radius);

    float rho = H * x_r;
    r = sqrt(rho * rho + ATMOSPHERE.bottom_radius * ATMOSPHERE.bottom_radius);

    float d_min = ATMOSPHERE.top_radius - r;
    float d_max = rho + H;
    float d = d_min + x_mu * (d_max - d_min);

    mu = (d == 0.0) ? 1.0 : (H * H - rho * rho - d * d) / (2.0 * r * d);

    mu = clamp(mu, -1.0, 1.0);
}

// --- VÝPOČET -----------------------------------------------

vec3 ComputeTransmittanceToTopAtmosphereBoundary(float r, float mu) {
    float dist_to_top = DistanceToTopAtmosphereBoundary(r, mu);

    const int SAMPLE_COUNT = 500;
    float dx = dist_to_top / float(SAMPLE_COUNT);

    vec3 optical_depth = vec3(0.0);
    float mie_optical_depth = 0.0;

    for (int i = 0; i <= SAMPLE_COUNT; ++i) {
        float d_i = float(i) * dx;

        float r_i = sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r);

        vec2 densities = GetDensityProfile(r_i - ATMOSPHERE.bottom_radius);

        float weight = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;

        optical_depth += densities.x * weight;
        mie_optical_depth += densities.y * weight;
    }

    vec3 rayleigh = optical_depth * dx * ATMOSPHERE.rayleigh_scattering;
    vec3 mie = vec3(mie_optical_depth * dx * ATMOSPHERE.mie_extinction);

    return exp(-(rayleigh + mie));
}

void main() {
    ivec2 pixel_coord = ivec2(gl_GlobalInvocationID.xy);

    if (pixel_coord.x >= int(TRANSMITTANCE_TEXTURE_SIZE.x) ||
    pixel_coord.y >= int(TRANSMITTANCE_TEXTURE_SIZE.y)) {
        return;
    }

    vec2 uv = (vec2(pixel_coord) + 0.5) / TRANSMITTANCE_TEXTURE_SIZE;

    float r, mu;
    GetRMuFromTransmittanceTextureUv(uv, r, mu);

    vec3 transmittance = ComputeTransmittanceToTopAtmosphereBoundary(r, mu);

    imageStore(transmittanceImage, pixel_coord, vec4(transmittance, 1.0));
}
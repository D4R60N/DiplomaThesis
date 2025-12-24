float ClampCosine(float mu) {
    return clamp(mu, float(-1.0), float(1.0));
}

float ClampDistance(float d) {
    return max(d, 0.0 * m);
}

float ClampRadius(AtmosphereParameters atmosphere, float r) {
    return clamp(r, atmosphere.bottom_radius, atmosphere.top_radius);
}

float SafeSqrt(Area a) {
    return sqrt(max(a, 0.0 * m2));
}

float DistanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, float r, float mu) {
    Area discriminant = r * r * (mu * mu - 1.0) +
    atmosphere.top_radius * atmosphere.top_radius;
    return ClampDistance(-r * mu + SafeSqrt(discriminant));
}

float DistanceToBottomAtmosphereBoundary(AtmosphereParameters atmosphere, float r, float mu) {
    Area discriminant = r * r * (mu * mu - 1.0) +
    atmosphere.bottom_radius * atmosphere.bottom_radius;
    return ClampDistance(-r * mu - SafeSqrt(discriminant));
}

bool RayIntersectsGround(AtmosphereParameters atmosphere, float r, float mu) {
    return mu < 0.0 && r * r * (mu * mu - 1.0) +
    atmosphere.bottom_radius * atmosphere.bottom_radius >= 0.0 * m2;
}

float GetLayerDensity(DensityProfileLayer layer, float altitude) {
    float density = layer.exp_term * exp(layer.exp_scale * altitude) +
    layer.linear_term * altitude + layer.constant_term;
    return clamp(density, float(0.0), float(1.0));
}

float GetProfileDensity(DensityProfile profile, float altitude) {
    return altitude < profile.layers[0].width ?
    GetLayerDensity(profile.layers[0], altitude) :
    GetLayerDensity(profile.layers[1], altitude);
}

float ComputeOpticalLengthToTopAtmosphereBoundary(AtmosphereParameters atmosphere, DensityProfile profile, float r, float mu) {
    const int SAMPLE_COUNT = 500;
    float dx = DistanceToTopAtmosphereBoundary(atmosphere, r, mu) / float(SAMPLE_COUNT);
    float result = 0.0 * m;
    for (int i = 0; i <= SAMPLE_COUNT; ++i) {
        float d_i = float(i) * dx;
        float r_i = sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r);
        float y_i = GetProfileDensity(profile, r_i - atmosphere.bottom_radius);
        float weight_i = i == 0 || i == SAMPLE_COUNT ? 0.5 : 1.0;
        result += y_i * weight_i * dx;
    }
    return result;
}

vec3 ComputeTransmittanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, float r, float mu) {
    return vec3(ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.rayleigh_density, r, mu));
//    return exp(-(
//    atmosphere.rayleigh_scattering * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.rayleigh_density, r, mu)
//    + atmosphere.mie_extinction * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.mie_density, r, mu)
//    + atmosphere.absorption_extinction * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.absorption_density, r, mu)));
}

float GetTextureCoordFromUnitRange(float x, int texture_size) {
    return 0.5 / float(texture_size) + x * (1.0 - 1.0 / float(texture_size));
}

float GetUnitRangeFromTextureCoord(float u, int texture_size) {
    return (u - 0.5 / float(texture_size)) / (1.0 - 1.0 / float(texture_size));
}

vec2 GetTransmittanceTextureUvFromRMu(AtmosphereParameters atmosphere, float r, float mu) {
    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius -
    atmosphere.bottom_radius * atmosphere.bottom_radius);
    float rho =
    SafeSqrt(r * r - atmosphere.bottom_radius * atmosphere.bottom_radius);

    float d = DistanceToTopAtmosphereBoundary(atmosphere, r, mu);
    float d_min = atmosphere.top_radius - r;
    float d_max = rho + H;
    float x_mu = (d - d_min) / (d_max - d_min);
    float x_r = rho / H;
    return vec2(GetTextureCoordFromUnitRange(x_mu, TRANSMITTANCE_TEXTURE_WIDTH),
    GetTextureCoordFromUnitRange(x_r, TRANSMITTANCE_TEXTURE_HEIGHT));
}

void GetRMuFromTransmittanceTextureUv(AtmosphereParameters atmosphere, vec2 uv, inout float r, inout float mu) {
    float x_mu = GetUnitRangeFromTextureCoord(uv.x, TRANSMITTANCE_TEXTURE_WIDTH);
    float x_r = GetUnitRangeFromTextureCoord(uv.y, TRANSMITTANCE_TEXTURE_HEIGHT);

    float H = sqrt(atmosphere.top_radius * atmosphere.top_radius -
    atmosphere.bottom_radius * atmosphere.bottom_radius);

    float rho = H * x_r;
    r = sqrt(rho * rho + atmosphere.bottom_radius * atmosphere.bottom_radius);
    float d_min = atmosphere.top_radius - r;
    float d_max = rho + H;
    float d = d_min + x_mu * (d_max - d_min);
    mu = d == 0.0 * m ? float(1.0) : (H * H - rho * rho - d * d) / (2.0 * r * d);
    mu = ClampCosine(mu);
}

vec3 ComputeTransmittanceToTopAtmosphereBoundaryTexture(AtmosphereParameters atmosphere, vec2 frag_coord) {
    float r;
    float mu;
    GetRMuFromTransmittanceTextureUv(atmosphere, frag_coord, r, mu);
    float altitude = (r - atmosphere.bottom_radius) / (atmosphere.top_radius - atmosphere.bottom_radius);
    return ComputeTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu);
}

vec3 GetTransmittanceToTopAtmosphereBoundary(
AtmosphereParameters atmosphere,
TransmittanceTexture transmittance_texture,
float r, float mu) {
    vec2 uv = GetTransmittanceTextureUvFromRMu(atmosphere, r, mu);
    return vec3(texture(transmittance_texture, uv));
}

vec3 GetTransmittance(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture, float r, float mu, float d, bool ray_r_mu_intersects_ground) {
    float r_d = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    float mu_d = ClampCosine((r * mu + d) / r_d);

    if (ray_r_mu_intersects_ground) {
        return min(
        GetTransmittanceToTopAtmosphereBoundary(
        atmosphere, transmittance_texture, r_d, -mu_d) /
        GetTransmittanceToTopAtmosphereBoundary(
        atmosphere, transmittance_texture, r, -mu),
        vec3(1.0));
    } else {
        return min(
        GetTransmittanceToTopAtmosphereBoundary(
        atmosphere, transmittance_texture, r, mu) /
        GetTransmittanceToTopAtmosphereBoundary(
        atmosphere, transmittance_texture, r_d, mu_d),
        vec3(1.0));
    }
}

vec3 GetTransmittanceToSun(
AtmosphereParameters atmosphere,
TransmittanceTexture transmittance_texture,
float r, float mu_s) {
    float sin_theta_h = atmosphere.bottom_radius / r;
    float cos_theta_h = -sqrt(max(1.0 - sin_theta_h * sin_theta_h, 0.0));
    return GetTransmittanceToTopAtmosphereBoundary(
    atmosphere, transmittance_texture, r, mu_s) *
    smoothstep(-sin_theta_h * atmosphere.sun_angular_radius / rad,
    sin_theta_h * atmosphere.sun_angular_radius / rad,
    mu_s - cos_theta_h);
}
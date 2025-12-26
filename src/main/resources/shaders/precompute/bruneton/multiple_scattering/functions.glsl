struct RayleighMieScattering {
    AbstractSpectrum rayleigh;
    AbstractSpectrum mie;
};

RayleighMieScattering GetSingleScattering(AtmosphereParameters atmosphere,
Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    Number tex_coord_x = uvwz.x * Number(SCATTERING_TEXTURE_NU_SIZE - 1);
    Number tex_x = floor(tex_coord_x);
    Number lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);

    vec3 size = vec3(imageSize(singleScatteringRayleighImage));
    ivec3 texel0 = ivec3(uvw0 * size);
    ivec3 texel1 = ivec3(uvw1 * size);
    vec4 t0 = imageLoad(singleScatteringRayleighImage, texel0);
    vec4 t1 = imageLoad(singleScatteringRayleighImage, texel1);

    vec3 size = vec3(imageSize(singleScatteringRayleighImage));
    ivec3 texel2 = ivec3(uvw0 * size);
    ivec3 texel3 = ivec3(uvw1 * size);
    vec4 t2 = imageLoad(singleScatteringRayleighImage, texel2);
    vec4 t3 = imageLoad(singleScatteringRayleighImage, texel3);


    return RayleighMieScattering(AbstractSpectrum(t0 * (1.0 - lerp) + t1 * lerp), AbstractSpectrum(t2 * (1.0 - lerp) + t3 * lerp));
}

AbstractSpectrum GetMultipleScattering(AtmosphereParameters atmosphere,
Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    Number tex_coord_x = uvwz.x * Number(SCATTERING_TEXTURE_NU_SIZE - 1);
    Number tex_x = floor(tex_coord_x);
    Number lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);

    vec3 size = vec3(imageSize(scatteringImage));
    ivec3 texel0 = ivec3(uvw0 * size);
    ivec3 texel1 = ivec3(uvw1 * size);
    vec4 t0 = imageLoad(scatteringImage, texel0);
    vec4 t1 = imageLoad(scatteringImage, texel1);


    return AbstractSpectrum(t0 * (1.0 - lerp) + t1 * lerp);
}

AbstractSpectrum GetScatteringDensity(AtmosphereParameters atmosphere,
Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    Number tex_coord_x = uvwz.x * Number(SCATTERING_TEXTURE_NU_SIZE - 1);
    Number tex_x = floor(tex_coord_x);
    Number lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE), uvwz.z, uvwz.w);

    vec3 size = vec3(imageSize(scatteringImage));
    ivec3 texel0 = ivec3(uvw0 * size);
    ivec3 texel1 = ivec3(uvw1 * size);
    vec4 t0 = imageLoad(scatteringImage, texel0);
    vec4 t1 = imageLoad(scatteringImage, texel1);


    return AbstractSpectrum(t0 * (1.0 - lerp) + t1 * lerp);
}


RadianceSpectrum GetScattering(AtmosphereParameters atmosphere, Length r, Number mu, Number mu_s, Number nu,
bool ray_r_mu_intersects_ground, int scattering_order) {
    if (scattering_order == 1) {
        RayleighMieScattering single_scattering = GetSingleScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
        AbstractSpectrum rayleigh = single_scattering.rayleigh;
        AbstractSpectrum mie = single_scattering.mie;
        return rayleigh * RayleighPhaseFunction(nu) + mie * MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
    } else {
        return GetMultipleScattering(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    }
}

RadianceDensitySpectrum ComputeScatteringDensity(
AtmosphereParameters atmosphere,
TransmittanceTexture transmittance_texture,
ReducedScatteringTexture single_rayleigh_scattering_texture,
ReducedScatteringTexture single_mie_scattering_texture,
ScatteringTexture multiple_scattering_texture,
IrradianceTexture irradiance_texture,
Length r, Number mu, Number mu_s, Number nu, int scattering_order) {
    vec3 zenith_direction = vec3(0.0, 0.0, 1.0);
    vec3 omega = vec3(sqrt(1.0 - mu * mu), 0.0, mu);
    Number sun_dir_x = omega.x == 0.0 ? 0.0 : (nu - mu * mu_s) / omega.x;
    Number sun_dir_y = sqrt(max(1.0 - sun_dir_x * sun_dir_x - mu_s * mu_s, 0.0));
    vec3 omega_s = vec3(sun_dir_x, sun_dir_y, mu_s);

    const int SAMPLE_COUNT = 16;
    const Angle dphi = pi / Number(SAMPLE_COUNT);
    const Angle dtheta = pi / Number(SAMPLE_COUNT);
    RadianceDensitySpectrum rayleigh_mie =
    RadianceDensitySpectrum(0.0 * watt_per_cubic_meter_per_sr_per_nm);
    for (int l = 0; l < SAMPLE_COUNT; ++l) {
        Angle theta = (Number(l) + 0.5) * dtheta;
        Number cos_theta = cos(theta);
        Number sin_theta = sin(theta);
        bool ray_r_theta_intersects_ground =
        RayIntersectsGround(atmosphere, r, cos_theta);

        Length distance_to_ground = 0.0 * m;
        DimensionlessSpectrum transmittance_to_ground = DimensionlessSpectrum(0.0);
        DimensionlessSpectrum ground_albedo = DimensionlessSpectrum(0.0);
        if (ray_r_theta_intersects_ground) {
            distance_to_ground =
            DistanceToBottomAtmosphereBoundary(atmosphere, r, cos_theta);
            transmittance_to_ground =
            GetTransmittance(atmosphere, r, cos_theta, distance_to_ground, true);
            ground_albedo = atmosphere.ground_albedo;
        }

        for (int m = 0; m < 2 * SAMPLE_COUNT; ++m) {
            Angle phi = (Number(m) + 0.5) * dphi;
            vec3 omega_i =
            vec3(cos(phi) * sin_theta, sin(phi) * sin_theta, cos_theta);
            SolidAngle domega_i = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;

            Number nu1 = dot(omega_s, omega_i);
            RadianceSpectrum incident_radiance = GetScattering(atmosphere, r, omega_i.z, mu_s, nu1,
            ray_r_theta_intersects_ground, scattering_order - 1);

            vec3 ground_normal =
            normalize(zenith_direction * r + omega_i * distance_to_ground);
            IrradianceSpectrum ground_irradiance = GetIrradiance(
            atmosphere, irradiance_texture, atmosphere.bottom_radius,
            dot(ground_normal, omega_s));
            incident_radiance += transmittance_to_ground *
            ground_albedo * (1.0 / (PI * sr)) * ground_irradiance;

            Number nu2 = dot(omega, omega_i);
            Number rayleigh_density = GetProfileDensity(
            atmosphere.rayleigh_density, r - atmosphere.bottom_radius);
            Number mie_density = GetProfileDensity(
            atmosphere.mie_density, r - atmosphere.bottom_radius);
            rayleigh_mie += incident_radiance * (
            atmosphere.rayleigh_scattering * rayleigh_density *
            RayleighPhaseFunction(nu2) +
            atmosphere.mie_scattering * mie_density *
            MiePhaseFunction(atmosphere.mie_phase_function_g, nu2)) *
            domega_i;
        }
    }
    return rayleigh_mie;
}

RadianceSpectrum ComputeMultipleScattering(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture, ScatteringDensityTexture scattering_density_texture,
Length r, Number mu, Number mu_s, Number nu,
bool ray_r_mu_intersects_ground) {
    const int SAMPLE_COUNT = 50;
    Length dx = DistanceToNearestAtmosphereBoundary(
    atmosphere, r, mu, ray_r_mu_intersects_ground) /
    Number(SAMPLE_COUNT);
    RadianceSpectrum rayleigh_mie_sum = RadianceSpectrum(0.0 * watt_per_square_meter_per_sr_per_nm);
    for (int i = 0; i <= SAMPLE_COUNT; ++i) {
        Length d_i = Number(i) * dx;

        Length r_i =
        ClampRadius(atmosphere, sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r));
        Number mu_i = ClampCosine((r * mu + d_i) / r_i);
        Number mu_s_i = ClampCosine((r * mu_s + d_i * nu) / r_i);

        RadianceSpectrum rayleigh_mie_i =
        GetScatteringDensity(atmosphere, r_i, mu_i, mu_s_i, nu, ray_r_mu_intersects_ground) *
        GetTransmittance(atmosphere, r, mu, d_i, ray_r_mu_intersects_ground) * dx;
        Number weight_i = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;
        rayleigh_mie_sum += rayleigh_mie_i * weight_i;
    }
    return rayleigh_mie_sum;
}

void GetRMuMuSNuFromScatteringTextureFragCoord(AtmosphereParameters atmosphere, vec3 frag_coord, out Length r, out Number mu, out Number mu_s, out Number nu,
out bool ray_r_mu_intersects_ground) {
    const vec4 SCATTERING_TEXTURE_SIZE = vec4(SCATTERING_TEXTURE_NU_SIZE - 1, SCATTERING_TEXTURE_MU_S_SIZE, SCATTERING_TEXTURE_MU_SIZE, SCATTERING_TEXTURE_R_SIZE);
    Number frag_coord_nu = floor(frag_coord.x / Number(SCATTERING_TEXTURE_MU_S_SIZE));
    Number frag_coord_mu_s = mod(frag_coord.x, Number(SCATTERING_TEXTURE_MU_S_SIZE));
    vec4 uvwz = vec4(frag_coord_nu, frag_coord_mu_s, frag_coord.y, frag_coord.z) / SCATTERING_TEXTURE_SIZE;
    GetRMuMuSNuFromScatteringTextureUvwz(atmosphere, uvwz, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    nu = clamp(nu, mu * mu_s - sqrt((1.0 - mu * mu) * (1.0 - mu_s * mu_s)), mu * mu_s + sqrt((1.0 - mu * mu) * (1.0 - mu_s * mu_s)));
}

RadianceDensitySpectrum ComputeScatteringDensityTexture(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture, ReducedScatteringTexture single_rayleigh_scattering_texture,
ReducedScatteringTexture single_mie_scattering_texture, ScatteringTexture multiple_scattering_texture, IrradianceTexture irradiance_texture,
vec3 frag_coord, int scattering_order) {
    Length r;
    Number mu;
    Number mu_s;
    Number nu;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord,
    r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    return ComputeScatteringDensity(atmosphere, transmittance_texture,
    single_rayleigh_scattering_texture, single_mie_scattering_texture,
    multiple_scattering_texture, irradiance_texture, r, mu, mu_s, nu,
    scattering_order);
}

RadianceSpectrum ComputeMultipleScatteringTexture(
AtmosphereParameters atmosphere,
TransmittanceTexture transmittance_texture,
ScatteringDensityTexture scattering_density_texture,
vec3 frag_coord, out Number nu) {
    Length r;
    Number mu;
    Number mu_s;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord,
    r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    return ComputeMultipleScattering(atmosphere, transmittance_texture,
    scattering_density_texture, r, mu, mu_s, nu,
    ray_r_mu_intersects_ground);
}
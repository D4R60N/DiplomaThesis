vec2 GetIrradianceTextureUvFromRMuS(AtmosphereParameters atmosphere, Length r, Number mu_s) {
    Number x_r = (r - atmosphere.bottom_radius) /
    (atmosphere.top_radius - atmosphere.bottom_radius);
    Number x_mu_s = mu_s * 0.5 + 0.5;
    return vec2(GetTextureCoordFromUnitRange(x_mu_s, IRRADIANCE_TEXTURE_WIDTH),
    GetTextureCoordFromUnitRange(x_r, IRRADIANCE_TEXTURE_HEIGHT));
}

IrradianceSpectrum GetIrradiance(AtmosphereParameters atmosphere, Length r, Number mu_s) {
    vec2 uv = GetIrradianceTextureUvFromRMuS(atmosphere, r, mu_s);
    vec4 t = texture(irradianceSampler, uv);
    return IrradianceSpectrum(t);
}

AbstractSpectrum GetScattering(AtmosphereParameters atmosphere, AbstractScatteringTexture scattering_texture,
Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    Number tex_coord_x = uvwz.x * Number(SCATTERING_TEXTURE_NU_SIZE - 1);
    Number tex_x = floor(tex_coord_x);
    Number lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE),
    uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE),
    uvwz.z, uvwz.w);
    return AbstractSpectrum(texture(scattering_texture, uvw0) * (1.0 - lerp) + texture(scattering_texture, uvw1) * lerp);
}

RadianceSpectrum GetScattering(AtmosphereParameters atmosphere, ReducedScatteringTexture single_rayleigh_scattering_texture,
ReducedScatteringTexture single_mie_scattering_texture, ScatteringTexture multiple_scattering_texture, Length r, Number mu, Number mu_s, Number nu,
bool ray_r_mu_intersects_ground,
int scattering_order) {
    if (scattering_order == 1) {
        IrradianceSpectrum rayleigh = GetScattering(atmosphere, single_rayleigh_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
        IrradianceSpectrum mie = GetScattering(atmosphere, single_mie_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
        return rayleigh * RayleighPhaseFunction(nu) +mie * MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
    } else {
        return GetScattering(atmosphere, multiple_scattering_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    }
}

RadianceSpectrum GetScattering(AtmosphereParameters atmosphere, Length r, Number mu, Number mu_s, Number nu,
bool ray_r_mu_intersects_ground, int scattering_order) {
    return GetScattering(atmosphere, singleRayleighScatteringSampler, singleMieScatteringSampler, multipleScatteringSampler, r, mu, mu_s, nu, ray_r_mu_intersects_ground, scattering_order);
}

RadianceDensitySpectrum ComputeScatteringDensity(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture, ReducedScatteringTexture single_rayleigh_scattering_texture,
ReducedScatteringTexture single_mie_scattering_texture, ScatteringTexture multiple_scattering_texture, IrradianceTexture irradiance_texture,
Length r, Number mu, Number mu_s, Number nu, int scattering_order) {

    vec3 zenith_direction = vec3(0.0, 0.0, 1.0);
    vec3 omega = vec3(sqrt(1.0 - mu * mu), 0.0, mu);
    Number sun_dir_x = omega.x == 0.0 ? 0.0 : (nu - mu * mu_s) / omega.x;
    Number sun_dir_y = sqrt(max(1.0 - sun_dir_x * sun_dir_x - mu_s * mu_s, 0.0));
    vec3 omega_s = vec3(sun_dir_x, sun_dir_y, mu_s);

    const int SAMPLE_COUNT = 16;
    const Angle dphi = pi / Number(SAMPLE_COUNT);
    const Angle dtheta = pi / Number(SAMPLE_COUNT);
    RadianceDensitySpectrum rayleigh_mie = RadianceDensitySpectrum(0.0 * watt_per_cubic_meter_per_sr_per_nm);

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
            GetTransmittance(atmosphere, r, cos_theta,
            distance_to_ground, true);
            ground_albedo = atmosphere.ground_albedo;
        }

        for (int m = 0; m < 2 * SAMPLE_COUNT; ++m) {
            Angle phi = (Number(m) + 0.5) * dphi;
            vec3 omega_i =
            vec3(cos(phi) * sin_theta, sin(phi) * sin_theta, cos_theta);
            SolidAngle domega_i = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;

            Number nu1 = dot(omega_s, omega_i);
            RadianceSpectrum incident_radiance = GetScattering(atmosphere,
            single_rayleigh_scattering_texture, single_mie_scattering_texture,
            multiple_scattering_texture, r, omega_i.z, mu_s, nu1,
            ray_r_theta_intersects_ground, scattering_order - 1);

            vec3 ground_normal =
            normalize(zenith_direction * r + omega_i * distance_to_ground);
            IrradianceSpectrum ground_irradiance = GetIrradiance(
            atmosphere, atmosphere.bottom_radius,
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

RadianceDensitySpectrum ComputeScatteringDensityTexture(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture, ReducedScatteringTexture single_rayleigh_scattering_texture,
ReducedScatteringTexture single_mie_scattering_texture, ScatteringTexture multiple_scattering_texture, IrradianceTexture irradiance_texture, vec3 frag_coord, int scattering_order) {
    Length r;
    Number mu;
    Number mu_s;
    Number nu;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    return ComputeScatteringDensity(atmosphere, transmittance_texture, single_rayleigh_scattering_texture, single_mie_scattering_texture, multiple_scattering_texture, irradiance_texture, r, mu, mu_s, nu, scattering_order);
}


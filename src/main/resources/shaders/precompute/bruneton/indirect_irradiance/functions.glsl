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

    size = vec3(imageSize(singleScatteringRayleighImage));
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

IrradianceSpectrum ComputeIndirectIrradiance(AtmosphereParameters atmosphere, Length r, Number mu_s, int scattering_order) {
    const int SAMPLE_COUNT = 32;
    const Angle dphi = pi / Number(SAMPLE_COUNT);
    const Angle dtheta = pi / Number(SAMPLE_COUNT);

    IrradianceSpectrum result = IrradianceSpectrum(0.0 * watt_per_square_meter_per_nm);
    vec3 omega_s = vec3(sqrt(1.0 - mu_s * mu_s), 0.0, mu_s);
    for (int j = 0; j < SAMPLE_COUNT / 2; ++j) {
        Angle theta = (Number(j) + 0.5) * dtheta;
        for (int i = 0; i < 2 * SAMPLE_COUNT; ++i) {
            Angle phi = (Number(i) + 0.5) * dphi;
            vec3 omega =
            vec3(cos(phi) * sin(theta), sin(phi) * sin(theta), cos(theta));
            SolidAngle domega = (dtheta / rad) * (dphi / rad) * sin(theta) * sr;

            Number nu = dot(omega, omega_s);
            result += GetScattering(atmosphere,
            r, omega.z, mu_s, nu, false,
            scattering_order) *
            omega.z * domega;
        }
    }
    return result;
}

void GetRMuSFromIrradianceTextureUv(AtmosphereParameters atmosphere, vec2 uv, out Length r, out Number mu_s) {
    Number x_mu_s = GetUnitRangeFromTextureCoord(uv.x, IRRADIANCE_TEXTURE_WIDTH);
    Number x_r = GetUnitRangeFromTextureCoord(uv.y, IRRADIANCE_TEXTURE_HEIGHT);
    r = atmosphere.bottom_radius +
    x_r * (atmosphere.top_radius - atmosphere.bottom_radius);
    mu_s = ClampCosine(2.0 * x_mu_s - 1.0);
}

IrradianceSpectrum ComputeIndirectIrradianceTexture(AtmosphereParameters atmosphere, vec2 frag_coord, int scattering_order) {
    Length r;
    Number mu_s;
    vec2 IRRADIANCE_TEXTURE_SIZE = vec2(IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
    GetRMuSFromIrradianceTextureUv(atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeIndirectIrradiance(atmosphere, r, mu_s, scattering_order);
}
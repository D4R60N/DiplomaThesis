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

RadianceSpectrum ComputeMultipleScattering(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture,
ScatteringDensityTexture scattering_density_texture, Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {

    const int SAMPLE_COUNT = 50;
    Length dx =
    DistanceToNearestAtmosphereBoundary(
    atmosphere, r, mu, ray_r_mu_intersects_ground) /
    Number(SAMPLE_COUNT);
    RadianceSpectrum rayleigh_mie_sum =
    RadianceSpectrum(0.0 * watt_per_square_meter_per_sr_per_nm);
    for (int i = 0; i <= SAMPLE_COUNT; ++i) {
        Length d_i = Number(i) * dx;

        Length r_i =
        ClampRadius(atmosphere, sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r));
        Number mu_i = ClampCosine((r * mu + d_i) / r_i);
        Number mu_s_i = ClampCosine((r * mu_s + d_i * nu) / r_i);

        RadianceSpectrum rayleigh_mie_i =
        GetScattering(atmosphere, scattering_density_texture, r_i, mu_i, mu_s_i, nu, ray_r_mu_intersects_ground) *
        GetTransmittance(atmosphere, r, mu, d_i, ray_r_mu_intersects_ground) * dx;
        Number weight_i = (i == 0 || i == SAMPLE_COUNT) ? 0.5 : 1.0;
        rayleigh_mie_sum += rayleigh_mie_i * weight_i;
    }
    return rayleigh_mie_sum;
}

RadianceSpectrum ComputeMultipleScatteringTexture(AtmosphereParameters atmosphere,TransmittanceTexture transmittance_texture,ScatteringDensityTexture scattering_density_texture,
vec3 frag_coord, out Number nu) {
    Length r;
    Number mu;
    Number mu_s;
    bool ray_r_mu_intersects_ground;
    GetRMuMuSNuFromScatteringTextureFragCoord(atmosphere, frag_coord, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    return ComputeMultipleScattering(atmosphere, transmittance_texture, scattering_density_texture, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
}
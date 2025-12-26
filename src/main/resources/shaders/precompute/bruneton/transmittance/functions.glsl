Length ComputeOpticalLengthToTopAtmosphereBoundary(AtmosphereParameters atmosphere, DensityProfile profile, Length r, Number mu) {
    const int SAMPLE_COUNT = 500;
    Length dx =
    DistanceToTopAtmosphereBoundary(atmosphere, r, mu) / Number(SAMPLE_COUNT);
    Length result = 0.0 * m;
    for (int i = 0; i <= SAMPLE_COUNT; ++i) {
        Length d_i = Number(i) * dx;
        Length r_i = sqrt(d_i * d_i + 2.0 * r * mu * d_i + r * r);
        Number y_i = GetProfileDensity(profile, r_i - atmosphere.bottom_radius);
        Number weight_i = i == 0 || i == SAMPLE_COUNT ? 0.5 : 1.0;
        result += y_i * weight_i * dx;
    }
    return result;
}

DimensionlessSpectrum ComputeTransmittanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, Length r, Number mu) {
    return exp(-(
    atmosphere.rayleigh_scattering * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.rayleigh_density, r, mu)
    + atmosphere.mie_extinction * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.mie_density, r, mu) +
    atmosphere.absorption_extinction * ComputeOpticalLengthToTopAtmosphereBoundary(atmosphere, atmosphere.absorption_density, r, mu)));
}

void GetRMuFromTransmittanceTextureUv(AtmosphereParameters atmosphere, vec2 uv, inout Length r, inout Number mu) {
    Number x_mu = GetUnitRangeFromTextureCoord(uv.x, TRANSMITTANCE_TEXTURE_WIDTH);
    Number x_r = GetUnitRangeFromTextureCoord(uv.y, TRANSMITTANCE_TEXTURE_HEIGHT);

    Length H = sqrt(atmosphere.top_radius * atmosphere.top_radius -
    atmosphere.bottom_radius * atmosphere.bottom_radius);

    Length rho = H * x_r;
    r = sqrt(rho * rho + atmosphere.bottom_radius * atmosphere.bottom_radius);
    Length d_min = atmosphere.top_radius - r;
    Length d_max = rho + H;
    Length d = d_min + x_mu * (d_max - d_min);
    mu = d == 0.0 * m ? Number(1.0) : (H * H - rho * rho - d * d) / (2.0 * r * d);
    mu = ClampCosine(mu);
}

DimensionlessSpectrum ComputeTransmittanceToTopAtmosphereBoundaryTexture(AtmosphereParameters atmosphere, vec2 frag_coord) {
    Length r;
    Number mu;
    GetRMuFromTransmittanceTextureUv(atmosphere, frag_coord, r, mu);
    return ComputeTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu);
}

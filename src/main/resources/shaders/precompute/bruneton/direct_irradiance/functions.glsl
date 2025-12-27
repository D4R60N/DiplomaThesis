vec2 GetTransmittanceTextureUvFromRMu(AtmosphereParameters atmosphere, Length r, Number mu) {
    Length H = sqrt(atmosphere.top_radius * atmosphere.top_radius -
    atmosphere.bottom_radius * atmosphere.bottom_radius);
    Length rho =
    SafeSqrt(r * r - atmosphere.bottom_radius * atmosphere.bottom_radius);

    Length d = DistanceToTopAtmosphereBoundary(atmosphere, r, mu);
    Length d_min = atmosphere.top_radius - r;
    Length d_max = rho + H;
    Number x_mu = (d - d_min) / (d_max - d_min);
    Number x_r = rho / H;
    return vec2(GetTextureCoordFromUnitRange(x_mu, TRANSMITTANCE_TEXTURE_WIDTH),
    GetTextureCoordFromUnitRange(x_r, TRANSMITTANCE_TEXTURE_HEIGHT));
}

DimensionlessSpectrum GetTransmittanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, Length r, Number mu) {
    vec2 uv = GetTransmittanceTextureUvFromRMu(atmosphere, r, mu);

    ivec2 size = imageSize(transmittanceImage);
    ivec2 texel = ivec2(uv * vec2(size));
    vec4 t = imageLoad(transmittanceImage, texel);

    return DimensionlessSpectrum(t);
}

IrradianceSpectrum ComputeDirectIrradiance(AtmosphereParameters atmosphere, Length r, Number mu_s) {

    Number alpha_s = atmosphere.sun_angular_radius / rad;
    Number average_cosine_factor = mu_s < -alpha_s ? 0.0 : (mu_s > alpha_s ? mu_s : (mu_s + alpha_s) * (mu_s + alpha_s) / (4.0 * alpha_s));

    return atmosphere.solar_irradiance * GetTransmittanceToTopAtmosphereBoundary(atmosphere, r, mu_s) * average_cosine_factor;
}

vec2 GetIrradianceTextureUvFromRMuS(AtmosphereParameters atmosphere, Length r, Number mu_s) {
    Number x_r = (r - atmosphere.bottom_radius) /
    (atmosphere.top_radius - atmosphere.bottom_radius);
    Number x_mu_s = mu_s * 0.5 + 0.5;
    return vec2(GetTextureCoordFromUnitRange(x_mu_s, IRRADIANCE_TEXTURE_WIDTH),
    GetTextureCoordFromUnitRange(x_r, IRRADIANCE_TEXTURE_HEIGHT));
}

void GetRMuSFromIrradianceTextureUv(AtmosphereParameters atmosphere, vec2 uv, out Length r, out Number mu_s) {
    Number x_mu_s = GetUnitRangeFromTextureCoord(uv.x, IRRADIANCE_TEXTURE_WIDTH);
    Number x_r = GetUnitRangeFromTextureCoord(uv.y, IRRADIANCE_TEXTURE_HEIGHT);
    r = atmosphere.bottom_radius +
    x_r * (atmosphere.top_radius - atmosphere.bottom_radius);
    mu_s = ClampCosine(2.0 * x_mu_s - 1.0);
}

IrradianceSpectrum ComputeDirectIrradianceTexture(AtmosphereParameters atmosphere, vec2 frag_coord) {
    Length r;
    Number mu_s;
    vec2 IRRADIANCE_TEXTURE_SIZE = vec2(IRRADIANCE_TEXTURE_WIDTH, IRRADIANCE_TEXTURE_HEIGHT);
    GetRMuSFromIrradianceTextureUv(
    atmosphere, frag_coord / IRRADIANCE_TEXTURE_SIZE, r, mu_s);
    return ComputeDirectIrradiance(atmosphere, r, mu_s);
}
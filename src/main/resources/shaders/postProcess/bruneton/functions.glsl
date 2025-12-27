IrradianceSpectrum GetIrradiance(AtmosphereParameters atmosphere, IrradianceTexture irradiance_texture, Length r, Number mu_s);

DimensionlessSpectrum GetTransmittanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, Length r, Number mu) {
    vec2 uv = GetTransmittanceTextureUvFromRMu(atmosphere, r, mu);

    ivec2 size = imageSize(transmittanceImage);
    ivec2 texel = ivec2(uv * vec2(size));
    vec4 t = imageLoad(transmittanceImage, texel);

    return DimensionlessSpectrum(t);
}

InverseSolidAngle RayleighPhaseFunction(Number nu) {
    InverseSolidAngle k = 3.0 / (16.0 * PI * sr);
    return k * (1.0 + nu * nu);
}

InverseSolidAngle MiePhaseFunction(Number g, Number nu) {
    InverseSolidAngle k = 3.0 / (8.0 * PI * sr) * (1.0 - g * g) / (2.0 + g * g);
    return k * (1.0 + nu * nu) / pow(1.0 + g * g - 2.0 * g * nu, 1.5);
}

vec4 GetScatteringTextureUvwzFromRMuMuSNu(AtmosphereParameters atmosphere, Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground) {
    Length H = sqrt(atmosphere.top_radius * atmosphere.top_radius - atmosphere.bottom_radius * atmosphere.bottom_radius);
    Length rho = SafeSqrt(r * r - atmosphere.bottom_radius * atmosphere.bottom_radius);
    Number u_r = GetTextureCoordFromUnitRange(rho / H, SCATTERING_TEXTURE_R_SIZE);
    Length r_mu = r * mu;
    Area discriminant = r_mu * r_mu - r * r + atmosphere.bottom_radius * atmosphere.bottom_radius;
    Number u_mu;
    if (ray_r_mu_intersects_ground) {
        Length d = -r_mu - SafeSqrt(discriminant);
        Length d_min = r - atmosphere.bottom_radius;
        Length d_max = rho;
        u_mu = 0.5 - 0.5 * GetTextureCoordFromUnitRange(d_max == d_min ? 0.0 :
        (d - d_min) / (d_max - d_min), SCATTERING_TEXTURE_MU_SIZE / 2);
    } else {
        Length d = -r_mu + SafeSqrt(discriminant + H * H);
        Length d_min = atmosphere.top_radius - r;
        Length d_max = rho + H;
        u_mu = 0.5 + 0.5 * GetTextureCoordFromUnitRange(
        (d - d_min) / (d_max - d_min), SCATTERING_TEXTURE_MU_SIZE / 2);
    }

    Length d = DistanceToTopAtmosphereBoundary(
    atmosphere, atmosphere.bottom_radius, mu_s);
    Length d_min = atmosphere.top_radius - atmosphere.bottom_radius;
    Length d_max = H;
    Number a = (d - d_min) / (d_max - d_min);
    Length D = DistanceToTopAtmosphereBoundary(
    atmosphere, atmosphere.bottom_radius, atmosphere.mu_s_min);
    Number A = (D - d_min) / (d_max - d_min);
    Number u_mu_s = GetTextureCoordFromUnitRange(
    max(1.0 - a / A, 0.0) / (1.0 + a), SCATTERING_TEXTURE_MU_S_SIZE);

    Number u_nu = (nu + 1.0) / 2.0;
    return vec4(u_nu, u_mu_s, u_mu, u_r);
}

IrradianceSpectrum GetCombinedScattering(AtmosphereParameters atmosphere, ReducedScatteringTexture scattering_texture, ReducedScatteringTexture single_mie_scattering_texture, Length r, Number mu, Number mu_s, Number nu, bool ray_r_mu_intersects_ground,
out IrradianceSpectrum single_mie_scattering) {
    vec4 uvwz = GetScatteringTextureUvwzFromRMuMuSNu(
    atmosphere, r, mu, mu_s, nu, ray_r_mu_intersects_ground);
    Number tex_coord_x = uvwz.x * Number(SCATTERING_TEXTURE_NU_SIZE - 1);
    Number tex_x = floor(tex_coord_x);
    Number lerp = tex_coord_x - tex_x;
    vec3 uvw0 = vec3((tex_x + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE),
    uvwz.z, uvwz.w);
    vec3 uvw1 = vec3((tex_x + 1.0 + uvwz.y) / Number(SCATTERING_TEXTURE_NU_SIZE),
    uvwz.z, uvwz.w);
    IrradianceSpectrum scattering = IrradianceSpectrum(
    texture(scattering_texture, uvw0) * (1.0 - lerp) +
    texture(scattering_texture, uvw1) * lerp);
    single_mie_scattering = IrradianceSpectrum(
    texture(single_mie_scattering_texture, uvw0) * (1.0 - lerp) +
    texture(single_mie_scattering_texture, uvw1) * lerp);
    return scattering;
}

RadianceSpectrum GetSkyRadiance(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture,
ReducedScatteringTexture scattering_texture, ReducedScatteringTexture single_mie_scattering_texture,
Position camera, Direction view_ray, Length shadow_length, Direction sun_direction, out DimensionlessSpectrum transmittance) {
    Length r = length(camera);
    Length rmu = dot(camera, view_ray);
    Length distance_to_top_atmosphere_boundary = -rmu -sqrt(rmu * rmu - r * r + atmosphere.top_radius * atmosphere.top_radius);
    if (distance_to_top_atmosphere_boundary > 0.0 * m) {
        camera = camera + view_ray * distance_to_top_atmosphere_boundary;
        r = atmosphere.top_radius;
        rmu += distance_to_top_atmosphere_boundary;
    } else if (r > atmosphere.top_radius) {
        transmittance = DimensionlessSpectrum(1.0);
        return RadianceSpectrum(0.0 * watt_per_square_meter_per_sr_per_nm);
    }
    Number mu = rmu / r;
    Number mu_s = dot(camera, sun_direction) / r;
    Number nu = dot(view_ray, sun_direction);
    bool ray_r_mu_intersects_ground = RayIntersectsGround(atmosphere, r, mu);

    transmittance = ray_r_mu_intersects_ground ? DimensionlessSpectrum(0.0) :
    GetTransmittanceToTopAtmosphereBoundary(
    atmosphere, transmittance_texture, r, mu);
    IrradianceSpectrum single_mie_scattering;
    IrradianceSpectrum scattering;
    if (shadow_length == 0.0 * m) {
        scattering = GetCombinedScattering(
        atmosphere, scattering_texture, single_mie_scattering_texture,
        r, mu, mu_s, nu, ray_r_mu_intersects_ground,
        single_mie_scattering);
    } else {
        Length d = shadow_length;
        Length r_p =
        ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
        Number mu_p = (r * mu + d) / r_p;
        Number mu_s_p = (r * mu_s + d * nu) / r_p;

        scattering = GetCombinedScattering(
        atmosphere, scattering_texture, single_mie_scattering_texture,
        r_p, mu_p, mu_s_p, nu, ray_r_mu_intersects_ground,
        single_mie_scattering);
        DimensionlessSpectrum shadow_transmittance =
        GetTransmittance(atmosphere, transmittance_texture,
        r, mu, shadow_length, ray_r_mu_intersects_ground);
        scattering = scattering * shadow_transmittance;
        single_mie_scattering = single_mie_scattering * shadow_transmittance;
    }
    return scattering * RayleighPhaseFunction(nu) + single_mie_scattering *
    MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
}

RadianceSpectrum GetSkyRadianceToPoint(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture,
ReducedScatteringTexture scattering_texture, ReducedScatteringTexture single_mie_scattering_texture,
Position camera, Position point, Length shadow_length, Direction sun_direction, out DimensionlessSpectrum transmittance) {
    Direction view_ray = normalize(point - camera);
    Length r = length(camera);
    Length rmu = dot(camera, view_ray);
    Length distance_to_top_atmosphere_boundary = -rmu -
    sqrt(rmu * rmu - r * r + atmosphere.top_radius * atmosphere.top_radius);

    if (distance_to_top_atmosphere_boundary > 0.0 * m) {
        camera = camera + view_ray * distance_to_top_atmosphere_boundary;
        r = atmosphere.top_radius;
        rmu += distance_to_top_atmosphere_boundary;
    }

    Number mu = rmu / r;
    Number mu_s = dot(camera, sun_direction) / r;
    Number nu = dot(view_ray, sun_direction);
    Length d = length(point - camera);
    bool ray_r_mu_intersects_ground = RayIntersectsGround(atmosphere, r, mu);

    transmittance = GetTransmittance(atmosphere, transmittance_texture,
    r, mu, d, ray_r_mu_intersects_ground);

    IrradianceSpectrum single_mie_scattering;
    IrradianceSpectrum scattering = GetCombinedScattering(
    atmosphere, scattering_texture, single_mie_scattering_texture,
    r, mu, mu_s, nu, ray_r_mu_intersects_ground,
    single_mie_scattering);

    d = max(d - shadow_length, 0.0 * m);
    Length r_p = ClampRadius(atmosphere, sqrt(d * d + 2.0 * r * mu * d + r * r));
    Number mu_p = (r * mu + d) / r_p;
    Number mu_s_p = (r * mu_s + d * nu) / r_p;

    IrradianceSpectrum single_mie_scattering_p;
    IrradianceSpectrum scattering_p = GetCombinedScattering(
    atmosphere, scattering_texture, single_mie_scattering_texture,
    r_p, mu_p, mu_s_p, nu, ray_r_mu_intersects_ground,
    single_mie_scattering_p);

    DimensionlessSpectrum shadow_transmittance = transmittance;
    if (shadow_length > 0.0 * m) {
        shadow_transmittance = GetTransmittance(atmosphere, transmittance_texture,
        r, mu, d, ray_r_mu_intersects_ground);
    }
    scattering = scattering - shadow_transmittance * scattering_p;
    single_mie_scattering =
    single_mie_scattering - shadow_transmittance * single_mie_scattering_p;

    single_mie_scattering = single_mie_scattering *
    smoothstep(Number(0.0), Number(0.01), mu_s);

    return scattering * RayleighPhaseFunction(nu) + single_mie_scattering *
    MiePhaseFunction(atmosphere.mie_phase_function_g, nu);
}

IrradianceSpectrum GetSunAndSkyIrradiance(AtmosphereParameters atmosphere, TransmittanceTexture transmittance_texture,
IrradianceTexture irradiance_texture, Position point, Direction normal, Direction sun_direction, out IrradianceSpectrum sky_irradiance) {
Length r = length(point);
Number mu_s = dot(point, sun_direction) / r;

sky_irradiance = GetIrradiance(atmosphere, irradiance_texture, r, mu_s) *
(1.0 + dot(normal, point) / r) * 0.5;

return atmosphere.solar_irradiance *
GetTransmittanceToSun(
atmosphere, transmittance_texture, r, mu_s) *
max(dot(normal, sun_direction), 0.0);
}
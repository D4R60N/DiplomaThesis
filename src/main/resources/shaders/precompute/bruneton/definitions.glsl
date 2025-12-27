#define Length float
#define Wavelength float
#define Angle float
#define SolidAngle float
#define Power float
#define LuminousPower float

#define Number float
#define InverseLength float
#define Area float
#define Volume float
#define NumberDensity float
#define Irradiance float
#define Radiance float
#define SpectralPower float
#define SpectralIrradiance float
#define SpectralRadiance float
#define SpectralRadianceDensity float
#define ScatteringCoefficient float
#define InverseSolidAngle float
#define LuminousIntensity float
#define Luminance float
#define Illuminance float

#define AbstractSpectrum vec3
#define DimensionlessSpectrum vec3
#define PowerSpectrum vec3
#define IrradianceSpectrum vec3
#define RadianceSpectrum vec3
#define RadianceDensitySpectrum vec3
#define ScatteringSpectrum vec3

#define Position vec3
#define Direction vec3
#define Luminance3 vec3
#define Illuminance3 vec3

#define TransmittanceTexture image2D
#define AbstractScatteringTexture image3D
#define ReducedScatteringTexture image3D
#define ScatteringTexture image3D
#define ScatteringDensityTexture image3D
#define IrradianceTexture image2D

const Length m = 1.0;
const Wavelength nm = 1.0;
const Angle rad = 1.0;
const SolidAngle sr = 1.0;
const Power watt = 1.0;
const LuminousPower lm = 1.0;

const float PI = 3.14159265358979323846;

const Length km = 1000.0 * m;
const Area m2 = m * m;
const Volume m3 = m * m * m;
const Angle pi = PI * rad;
const Angle deg = pi / 180.0;
const Irradiance watt_per_square_meter = watt / m2;
const Radiance watt_per_square_meter_per_sr = watt / (m2 * sr);
const SpectralIrradiance watt_per_square_meter_per_nm = watt / (m2 * nm);
const SpectralRadiance watt_per_square_meter_per_sr_per_nm =
watt / (m2 * sr * nm);
const SpectralRadianceDensity watt_per_cubic_meter_per_sr_per_nm =
watt / (m3 * sr * nm);
const LuminousIntensity cd = lm / sr;
const LuminousIntensity kcd = 1000.0 * cd;
const Luminance cd_per_square_meter = cd / m2;
const Luminance kcd_per_square_meter = kcd / m2;

struct DensityProfileLayer {
Length width;
    Number exp_term;
    InverseLength exp_scale;
    InverseLength linear_term;
    Number constant_term;
};

struct DensityProfile {
    DensityProfileLayer layers[2];
};

struct AtmosphereParameters {
IrradianceSpectrum solar_irradiance;
    Angle sun_angular_radius;
    Length bottom_radius;
    Length top_radius;
    DensityProfile rayleigh_density;
    ScatteringSpectrum rayleigh_scattering;
    DensityProfile mie_density;
    ScatteringSpectrum mie_scattering;
    ScatteringSpectrum mie_extinction;
    Number mie_phase_function_g;
    DensityProfile absorption_density;
    ScatteringSpectrum absorption_extinction;
    DimensionlessSpectrum ground_albedo;
    Number mu_s_min;
};

Number ClampCosine(Number mu) {
    return clamp(mu, Number(-1.0), Number(1.0));
}

Length ClampDistance(Length d) {
    return max(d, 0.0 * m);
}

Length ClampRadius(AtmosphereParameters atmosphere, Length r) {
    return clamp(r, atmosphere.bottom_radius, atmosphere.top_radius);
}

Length SafeSqrt(Area a) {
    return sqrt(max(a, 0.0 * m2));
}

Length DistanceToTopAtmosphereBoundary(AtmosphereParameters atmosphere, Length r, Number mu) {
    Area discriminant = r * r * (mu * mu - 1.0) +
    atmosphere.top_radius * atmosphere.top_radius;
    return ClampDistance(-r * mu + SafeSqrt(discriminant));
}

Length DistanceToBottomAtmosphereBoundary(AtmosphereParameters atmosphere, Length r, Number mu) {
    Area discriminant = r * r * (mu * mu - 1.0) +
    atmosphere.bottom_radius * atmosphere.bottom_radius;
    return ClampDistance(-r * mu - SafeSqrt(discriminant));
}

bool RayIntersectsGround(AtmosphereParameters atmosphere, Length r, Number mu) {
    return mu < 0.0 && r * r * (mu * mu - 1.0) +
    atmosphere.bottom_radius * atmosphere.bottom_radius >= 0.0 * m2;
}

Number GetLayerDensity(DensityProfileLayer layer, Length altitude) {
    Number density = layer.exp_term * exp(layer.exp_scale * altitude) +
    layer.linear_term * altitude + layer.constant_term;
    return clamp(density, Number(0.0), Number(1.0));
}

Number GetProfileDensity(DensityProfile profile, Length altitude) {
    return altitude < profile.layers[0].width ?
    GetLayerDensity(profile.layers[0], altitude) :
    GetLayerDensity(profile.layers[1], altitude);
}
Number GetTextureCoordFromUnitRange(Number x, int texture_size) {
    return 0.5 / Number(texture_size) + x * (1.0 - 1.0 / Number(texture_size));
}

Number GetUnitRangeFromTextureCoord(Number u, int texture_size) {
    return (u - 0.5 / Number(texture_size)) / (1.0 - 1.0 / Number(texture_size));
}

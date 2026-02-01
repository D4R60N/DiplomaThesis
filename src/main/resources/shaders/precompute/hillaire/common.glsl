#define PI 3.1415926535897932384626433832795f
#define NONLINEARSKYVIEWLUT 1
#define ILLUMINANCE_IS_ONE 1
#define PLANET_RADIUS_OFFSET 0.01f
#define GU_TO_KM 1000f
const float MultiScatteringLUTRes = 32.0;

struct AtmosphereParameters {
    float BottomRadius;
    float TopRadius;
    float RayleighDensityExpScale;
    vec3 RayleighScattering;
    float MieDensityExpScale;
    vec3 MieScattering;
    vec3 MieExtinction;
    vec3 MieAbsorption;
    float MiePhaseG;
    float AbsorptionDensity0LayerWidth;
    float AbsorptionDensity0ConstantTerm;
    float AbsorptionDensity0LinearTerm;
    float AbsorptionDensity1ConstantTerm;
    float AbsorptionDensity1LinearTerm;
    vec3 AbsorptionExtinction;
    vec3 GroundAlbedo;
};

struct Ray
{
    vec3 o;
    vec3 d;
};

Ray createRay(in vec3 p, in vec3 d)
{
    Ray r;
    r.o = p;
    r.d = d;
    return r;
}

float saturate(float val) {
    return clamp(val, 0.0, 1.0);
}
vec2 saturate(vec2 val) {
    return clamp(val, vec2(0.0), vec2(1.0));
}
vec3 saturate(vec3 val) {
    return clamp(val, vec3(0.0), vec3(1.0));
}

float raySphereIntersectNearest(vec3 r0, vec3 rd, vec3 s0, float sR)
{
    float a = dot(rd, rd);
    vec3 s0_r0 = r0 - s0;
    float b = 2.0 * dot(rd, s0_r0);
    float c = dot(s0_r0, s0_r0) - (sR * sR);
    float delta = b * b - 4.0*a*c;
    if (delta < 0.0 || a == 0.0)
    {
        return -1.0;
    }
    float sol0 = (-b - sqrt(delta)) / (2.0*a);
    float sol1 = (-b + sqrt(delta)) / (2.0*a);
    if (sol0 < 0.0 && sol1 < 0.0)
    {
        return -1.0;
    }
    if (sol0 < 0.0)
    {
        return max(0.0, sol1);
    }
    else if (sol1 < 0.0)
    {
        return max(0.0, sol0);
    }
    return max(0.0, min(sol0, sol1));
}

void LutTransmittanceParamsToUv(AtmosphereParameters Atmosphere, in float viewHeight, in float viewZenithCosAngle, out vec2 uv)
{
    float H = sqrt(max(0.0f, Atmosphere.TopRadius * Atmosphere.TopRadius - Atmosphere.BottomRadius * Atmosphere.BottomRadius));
    float rho = sqrt(max(0.0f, viewHeight * viewHeight - Atmosphere.BottomRadius * Atmosphere.BottomRadius));

    float discriminant = viewHeight * viewHeight * (viewZenithCosAngle * viewZenithCosAngle - 1.0) + Atmosphere.TopRadius * Atmosphere.TopRadius;
    float d = max(0.0, (-viewHeight * viewZenithCosAngle + sqrt(discriminant)));

    float d_min = Atmosphere.TopRadius - viewHeight;
    float d_max = rho + H;
    float x_mu = (d - d_min) / (d_max - d_min);
    float x_r = rho / H;

    uv = vec2(x_mu, x_r);
    //uv = vec2(fromUnitToSubUvs(uv.x, TRANSMITTANCE_TEXTURE_WIDTH), fromUnitToSubUvs(uv.y, TRANSMITTANCE_TEXTURE_HEIGHT)); // No real impact so off
}

float fromUnitToSubUvs(float u, float resolution) { return (u + 0.5f / resolution) * (resolution / (resolution + 1.0f)); }
float fromSubUvsToUnit(float u, float resolution) { return (u - 0.5f / resolution) * (resolution / (resolution - 1.0f)); }

void UvToLutTransmittanceParams(AtmosphereParameters Atmosphere, out float viewHeight, out float viewZenithCosAngle, in vec2 uv)
{
    //uv = vec2(fromSubUvsToUnit(uv.x, TRANSMITTANCE_TEXTURE_WIDTH), fromSubUvsToUnit(uv.y, TRANSMITTANCE_TEXTURE_HEIGHT)); // No real impact so off
    float x_mu = uv.x;
    float x_r = uv.y;

    float H = sqrt(Atmosphere.TopRadius * Atmosphere.TopRadius - Atmosphere.BottomRadius * Atmosphere.BottomRadius);
    float rho = H * x_r;
    viewHeight = sqrt(rho * rho + Atmosphere.BottomRadius * Atmosphere.BottomRadius);

    float d_min = Atmosphere.TopRadius - viewHeight;
    float d_max = rho + H;
    float d = d_min + x_mu * (d_max - d_min);
    viewZenithCosAngle = d == 0.0 ? 1.0f : (H * H - rho * rho - d * d) / (2.0 * viewHeight * d);
    viewZenithCosAngle = clamp(viewZenithCosAngle, -1.0, 1.0);
}

void UvToSkyViewLutParams(AtmosphereParameters Atmosphere, out float viewZenithCosAngle, out float lightViewCosAngle, in float viewHeight, in vec2 uv)
{
    // Constrain uvs to valid sub texel range (avoid zenith derivative issue making LUT usage visible)
    uv = vec2(fromSubUvsToUnit(uv.x, uSkyViewTextureSize.x), fromSubUvsToUnit(uv.y, uSkyViewTextureSize.y));

    float Vhorizon = sqrt(viewHeight * viewHeight - Atmosphere.BottomRadius * Atmosphere.BottomRadius);
    float CosBeta = Vhorizon / viewHeight;				// GroundToHorizonCos
    float beta = acos(CosBeta);
    float zenithHorizonAngle = PI - beta;

    if (uv.y < 0.5f)
    {
        float coord = 2.0*uv.y;
        coord = 1.0 - coord;
        #if NONLINEARSKYVIEWLUT
        coord *= coord;
        #endif
        coord = 1.0 - coord;
        viewZenithCosAngle = cos(zenithHorizonAngle * coord);
    }
    else
    {
        float coord = uv.y*2.0 - 1.0;
        #if NONLINEARSKYVIEWLUT
        coord *= coord;
        #endif
        viewZenithCosAngle = cos(zenithHorizonAngle + beta * coord);
    }

    float coord = uv.x;
    coord *= coord;
    lightViewCosAngle = -(coord*2.0 - 1.0);
}

void SkyViewLutParamsToUv(AtmosphereParameters Atmosphere, in bool IntersectGround, in float viewZenithCosAngle, in float lightViewCosAngle, in float viewHeight, out vec2 uv)
{
    float Vhorizon = sqrt(viewHeight * viewHeight - Atmosphere.BottomRadius * Atmosphere.BottomRadius);
    float CosBeta = Vhorizon / viewHeight;				// GroundToHorizonCos
    float beta = acos(CosBeta);
    float zenithHorizonAngle = PI - beta;

    if (!IntersectGround)
    {
        float coord = acos(viewZenithCosAngle) / zenithHorizonAngle;
        coord = 1.0 - coord;
        #if NONLINEARSKYVIEWLUT
        coord = sqrt(coord);
        #endif
        coord = 1.0 - coord;
        uv.y = coord * 0.5f;
    }
    else
    {
        float coord = (acos(viewZenithCosAngle) - zenithHorizonAngle) / beta;
        #if NONLINEARSKYVIEWLUT
        coord = sqrt(coord);
        #endif
        uv.y = coord * 0.5f + 0.5f;
    }

    {
        float coord = -lightViewCosAngle * 0.5f + 0.5f;
        coord = sqrt(coord);
        uv.x = coord;
    }
    
    uv = vec2(fromUnitToSubUvs(uv.x, uSkyViewTextureSize.x), fromUnitToSubUvs(uv.y, uSkyViewTextureSize.y));
}

float getAlbedo(float scattering, float extinction)
{
    return scattering / max(0.001, extinction);
}
vec3 getAlbedo(vec3 scattering, vec3 extinction)
{
    return scattering / max(vec3(0.001), extinction);
}

struct MediumSampleRGB
{
    vec3 scattering;
    vec3 absorption;
    vec3 extinction;

    vec3 scatteringMie;
    vec3 absorptionMie;
    vec3 extinctionMie;

    vec3 scatteringRay;
    vec3 absorptionRay;
    vec3 extinctionRay;

    vec3 scatteringOzo;
    vec3 absorptionOzo;
    vec3 extinctionOzo;

    vec3 albedo;
};

MediumSampleRGB sampleMediumRGB(in vec3 WorldPos, in AtmosphereParameters Atmosphere)
{
    const float viewHeight = length(WorldPos) - Atmosphere.BottomRadius;

    const float densityMie = exp(Atmosphere.MieDensityExpScale * viewHeight);
    const float densityRay = exp(Atmosphere.RayleighDensityExpScale * viewHeight);
    const float densityOzo = saturate(viewHeight < Atmosphere.AbsorptionDensity0LayerWidth ?
    Atmosphere.AbsorptionDensity0LinearTerm * viewHeight + Atmosphere.AbsorptionDensity0ConstantTerm :
    Atmosphere.AbsorptionDensity1LinearTerm * viewHeight + Atmosphere.AbsorptionDensity1ConstantTerm);

    MediumSampleRGB s;

    s.scatteringMie = densityMie * Atmosphere.MieScattering;
    s.absorptionMie = densityMie * Atmosphere.MieAbsorption;
    s.extinctionMie = densityMie * Atmosphere.MieExtinction;

    s.scatteringRay = densityRay * Atmosphere.RayleighScattering;
    s.absorptionRay = vec3(0.0f);
    s.extinctionRay = s.scatteringRay + s.absorptionRay;

    s.scatteringOzo = vec3(0.0f);
    s.absorptionOzo = densityOzo * Atmosphere.AbsorptionExtinction;
    s.extinctionOzo = s.scatteringOzo + s.absorptionOzo;

    s.scattering = s.scatteringMie + s.scatteringRay + s.scatteringOzo;
    s.absorption = s.absorptionMie + s.absorptionRay + s.absorptionOzo;
    s.extinction = s.extinctionMie + s.extinctionRay + s.extinctionOzo;
    s.albedo = getAlbedo(s.scattering, s.extinction);

    return s;
}

vec3 getUniformSphereSample(float zetaX, float zetaY)
{
    float phi = 2.0f * 3.14159f * zetaX;
    float theta = 2.0f * acos(sqrt(1.0f - zetaY));
    vec3 dir = vec3(sin(theta)*cos(phi), cos(theta), sin(theta)*sin(phi));
    return dir;
}

float infiniteTransmittanceIS(float extinction, float zeta)
{
    return -log(1.0f - zeta) / extinction;
}

float infiniteTransmittancePDF(float extinction, float transmittance)
{
    return extinction * transmittance;
}

float rangedTransmittanceIS(float extinction, float transmittance, float zeta)
{
    return -log(1.0f - zeta * (1.0f - transmittance)) / extinction;
}

float RayleighPhase(float cosTheta)
{
    float factor = 3.0f / (16.0f * PI);
    return factor * (1.0f + cosTheta * cosTheta);
}

float CornetteShanksMiePhaseFunction(float g, float cosTheta)
{
    float k = 3.0 / (8.0 * PI) * (1.0 - g * g) / (2.0 + g * g);
    return k * (1.0 + cosTheta * cosTheta) / pow(1.0 + g * g - 2.0 * g * -cosTheta, 1.5);
}

float hgPhase(float g, float cosTheta)
{
    return CornetteShanksMiePhaseFunction(g, cosTheta);
}

float dualLobPhase(float g0, float g1, float w, float cosTheta)
{
    return mix(hgPhase(g0, cosTheta), hgPhase(g1, cosTheta), w);
}

float uniformPhase()
{
    return 1.0f / (4.0f * PI);
}

void CreateOrthonormalBasis(in vec3 n, out vec3 b1, out vec3 b2)
{
    float sign = n.z >= 0.0f ? 1.0f : -1.0f; // copysignf(1.0f, n.z);
    const float a = -1.0f / (sign + n.z);
    const float b = n.x * n.y * a;
    b1 = vec3(1.0f + sign * n.x * n.x * a, sign * b, -sign * n.x);
    b2 = vec3(b, sign + n.y * n.y * a, -n.y);
}

float mean(vec3 v)
{
    return dot(v, vec3(1.0f / 3.0f, 1.0f / 3.0f, 1.0f / 3.0f));
}

float whangHashNoise(uint u, uint v, uint s)
{
    uint seed = (u * 1664525u + v) + s;
    seed = (seed ^ 61u) ^ (seed >> 16u);
    seed *= 9u;
    seed = seed ^ (seed >> 4u);
    seed *= uint(0x27d4eb2d);
    seed = seed ^ (seed >> 15u);
    float value = float(seed) / (4294967296.0);
    return value;
}

bool MoveToTopAtmosphere(inout vec3 WorldPos, in vec3 WorldDir, in float AtmosphereTopRadius)
{
    float viewHeight = length(WorldPos);
    if (viewHeight > AtmosphereTopRadius)
    {
        float tTop = raySphereIntersectNearest(WorldPos, WorldDir, vec3(0.0f, 0.0f, 0.0f), AtmosphereTopRadius);
        if (tTop >= 0.0f)
        {
            vec3 UpVector = WorldPos / viewHeight;
            vec3 UpOffset = UpVector * -PLANET_RADIUS_OFFSET;
            WorldPos = WorldPos + WorldDir * tTop + UpOffset;
        }
        else
        {
            // Ray is not intersecting the atmosphere
            return false;
        }
    }
    return true; // ok to start tracing
}

vec3 GetSunLuminance(vec3 WorldPos, vec3 WorldDir, float PlanetRadius)
{
    if (RENDER_SUN_DISK) {
        if (dot(WorldDir, sunDirection) > cos(0.5*0.505*3.14159 / 180.0))
        {
            float t = raySphereIntersectNearest(WorldPos, WorldDir, vec3(0.0f, 0.0f, 0.0f), PlanetRadius);
            if (t < 0.0f)
            {
                const vec3 SunLuminance = vec3(1000000.0);
                return SunLuminance;
            }
        }
    }
    return vec3(0.0f);
}

vec3 GetMultipleScattering(AtmosphereParameters Atmosphere, vec3 scattering, vec3 extinction, vec3 worlPos, float viewZenithCosAngle)
{
    vec2 uv = saturate(vec2(viewZenithCosAngle*0.5f + 0.5f, (length(worlPos) - Atmosphere.BottomRadius) / (Atmosphere.TopRadius - Atmosphere.BottomRadius)));
    uv = vec2(fromUnitToSubUvs(uv.x, MultiScatteringLUTRes), fromUnitToSubUvs(uv.y, MultiScatteringLUTRes));

    return textureLod(multiScatteringTexture, uv, 0.0).rgb;
}

struct SingleScatteringResult
{
    vec3 L;
    vec3 OpticalDepth;
    vec3 Transmittance;
    vec3 MultiScatAs1;

    vec3 NewMultiScatStep0Out;
    vec3 NewMultiScatStep1Out;
};

SingleScatteringResult IntegrateScatteredLuminance(
in vec2 pixPos, in vec3 WorldPos, in vec3 WorldDir, in vec3 SunDir, in AtmosphereParameters Atmosphere,
in bool ground, in float SampleCountIni, in float DepthBufferValue, in bool VariableSampleCount,
in bool MieRayPhase, in float tMaxMax, vec2 resolution)
{
    SingleScatteringResult result = SingleScatteringResult(
        vec3(0.0f),
        vec3(0.0f),
        vec3(1.0f),
        vec3(0.0f),

        vec3(0.0f),
        vec3(0.0f)
    );

    vec3 ClipSpace = vec3((pixPos / resolution)*vec2(2.0, -2.0) - vec2(1.0, -1.0), 1.0);

    vec3 earthO = vec3(0.0f, 0.0f, 0.0f);
    float tBottom = raySphereIntersectNearest(WorldPos, WorldDir, earthO, Atmosphere.BottomRadius);
    float tTop = raySphereIntersectNearest(WorldPos, WorldDir, earthO, Atmosphere.TopRadius);
    float tMax = 0.0f;
    if (tBottom < 0.0f)
    {
        if (tTop < 0.0f)
        {
            tMax = 0.0f;
            return result;
        }
        else
        {
            tMax = tTop;
        }
    }
    else
    {
        if (tTop > 0.0f)
        {
            tMax = min(tTop, tBottom);
        }
    }

    if (DepthBufferValue >= 0.0f)
    {
        ClipSpace.z = DepthBufferValue;
        if (ClipSpace.z < 1.0f)
        {
            vec4 DepthBufferWorldPos = invViewProj * vec4(ClipSpace, 1.0);
            DepthBufferWorldPos /= DepthBufferWorldPos.w;

            float tDepth = length(DepthBufferWorldPos.xyz - (WorldPos + vec3(0.0,  -Atmosphere.BottomRadius, 0.0)));
            tDepth *= 0.001f; // m to km
            if (tDepth < tMax)
            {
                tMax = tDepth;
            }
        }
        //		if (VariableSampleCount && ClipSpace.z == 1.0f)
        //			return result;
    }
    tMax = min(tMax, tMaxMax);

    float SampleCount = SampleCountIni;
    float SampleCountFloor = SampleCountIni;
    float tMaxFloor = tMax;
    if (VariableSampleCount)
    {
        SampleCount = mix(RayMarchMinMaxSPP.x, RayMarchMinMaxSPP.y, saturate(tMax*0.01));
        SampleCountFloor = floor(SampleCount);
        tMaxFloor = tMax * SampleCountFloor / SampleCount;
    }
    float dt = tMax / SampleCount;

    // Phase functions
    const float uniformPhase = 1.0 / (4.0 * PI);
    const vec3 wi = SunDir;
    const vec3 wo = WorldDir;
    float cosTheta = dot(wi, wo);
    float MiePhaseValue = hgPhase(Atmosphere.MiePhaseG, -cosTheta);
    float RayleighPhaseValue = RayleighPhase(cosTheta);

    vec3 globalL = sunIlluminance;

    vec3 L = vec3(0.0f);
    vec3 throughput = vec3(1.0f);
    vec3 OpticalDepth = vec3(0.0f);
    float t = 0.0f;
    float tPrev = 0.0;
    const float SampleSegmentT = 0.3f;
    for (float s = 0.0f; s < SampleCount; s += 1.0f)
    {
        if (VariableSampleCount)
        {
            float t0 = (s) / SampleCountFloor;
            float t1 = (s + 1.0f) / SampleCountFloor;
            t0 = t0 * t0;
            t1 = t1 * t1;
            t0 = tMaxFloor * t0;
            if (t1 > 1.0)
            {
                t1 = tMax;
            }
            else
            {
                t1 = tMaxFloor * t1;
            }
            t = t0 + (t1 - t0)*SampleSegmentT;
            dt = t1 - t0;
        }
        else
        {
            float NewT = tMax * (s + SampleSegmentT) / SampleCount;
            dt = NewT - t;
            t = NewT;
        }
        vec3 P = WorldPos + t * WorldDir;

        MediumSampleRGB medium = sampleMediumRGB(P, Atmosphere);
        const vec3 SampleOpticalDepth = medium.extinction * dt;
        const vec3 SampleTransmittance = exp(-SampleOpticalDepth);
        OpticalDepth += SampleOpticalDepth;

        float pHeight = length(P);
        const vec3 UpVector = P / pHeight;
        float SunZenithCosAngle = dot(SunDir, UpVector);
        vec2 uv;
        LutTransmittanceParamsToUv(Atmosphere, pHeight, SunZenithCosAngle, uv);
        vec3 TransmittanceToSun = textureLod(transmittanceTexture, uv, 0.0).rgb;

        vec3 PhaseTimesScattering;
        if (MieRayPhase)
        {
            PhaseTimesScattering = medium.scatteringMie * MiePhaseValue + medium.scatteringRay * RayleighPhaseValue;
        }
        else
        {
            PhaseTimesScattering = medium.scattering * uniformPhase;
        }

        float tEarth = raySphereIntersectNearest(P, SunDir, earthO + PLANET_RADIUS_OFFSET * UpVector, Atmosphere.BottomRadius);
        float earthShadow = tEarth >= 0.0f ? 0.0f : 1.0f;


        vec3 multiScatteredLuminance = vec3(0.0f);
        #if MULTISCATAPPROX_ENABLED
        multiScatteredLuminance = GetMultipleScattering(Atmosphere, medium.scattering, medium.extinction, P, SunZenithCosAngle);
        #endif

        float shadow = 1.0f;

        vec3 S = globalL * (earthShadow * shadow * TransmittanceToSun * PhaseTimesScattering + multiScatteredLuminance * medium.scattering);

        vec3 MS = medium.scattering * 1;
        vec3 MSint = (MS - MS * SampleTransmittance) / medium.extinction;
        result.MultiScatAs1 += throughput * MSint;

        vec3 newMS = earthShadow * TransmittanceToSun * medium.scattering * uniformPhase;
        result.NewMultiScatStep0Out += throughput * (newMS - newMS * SampleTransmittance) / medium.extinction;

        newMS = medium.scattering * uniformPhase * multiScatteredLuminance;
        result.NewMultiScatStep1Out += throughput * (newMS - newMS * SampleTransmittance) / medium.extinction;

        vec3 Sint = (S - S * SampleTransmittance) / medium.extinction;
        L += throughput * Sint;
        throughput *= SampleTransmittance;
    }

    if (ground && tMax == tBottom && tBottom > 0.0)
    {
        vec3 P = WorldPos + tBottom * WorldDir;
        float pHeight = length(P);

        const vec3 UpVector = P / pHeight;
        float SunZenithCosAngle = dot(SunDir, UpVector);
        vec2 uv;
        LutTransmittanceParamsToUv(Atmosphere, pHeight, SunZenithCosAngle, uv);
        vec3 TransmittanceToSun = textureLod(transmittanceTexture, uv, 0.0).rgb;

        const float NdotL = saturate(dot(normalize(UpVector), normalize(SunDir)));
        L += globalL * TransmittanceToSun * throughput * NdotL * Atmosphere.GroundAlbedo / PI;
    }

    result.L = L;
    result.OpticalDepth = OpticalDepth;
    result.Transmittance = throughput;
    return result;
}
SingleScatteringResult IntegrateScatteredLuminance(
in vec2 pixPos, in vec3 WorldPos, in vec3 WorldDir, in vec3 SunDir, in AtmosphereParameters Atmosphere,
in bool ground, in float SampleCountIni, in float DepthBufferValue, in bool VariableSampleCount,
in bool MieRayPhase, vec2 resolution){
    return IntegrateScatteredLuminance(pixPos, WorldPos, WorldDir, SunDir, Atmosphere, ground, SampleCountIni, DepthBufferValue, VariableSampleCount, MieRayPhase, 9000000.0f, resolution);
}
#version 430

// Lokální velikost pracovní skupiny (musí odpovídat nastavení dispatch v Javě)
layout (local_size_x = 8, local_size_y = 4, local_size_z = 1) in;

// --- Vstupní a Výstupní textury (LUTs) ---

// Transmittance Map (P1) - READ_ONLY na Unit 0
layout(rgba32f, binding = 0) uniform image2D transmittanceImage;

// Single Scattering Map (P2) - WRITE_ONLY na Unit 1
layout(rgba32f, binding = 1) uniform image2D singleScatteringImage;

// --- Uniformy (Fyzikální konstanty) ---
uniform float Re;  // Planet Radius
uniform float Ra;  // Atmosphere Radius
uniform float Hr;  // Rayleigh Scale Height
uniform float Hm;  // Mie Scale Height
uniform float g;   // Mie anisotropy factor (e.g., 0.75)

uniform vec3 BetaR; // Rayleigh scattering coefficients (Rx, Ry, Rz)
uniform vec3 BetaM; // Mie scattering coefficients (Mx, My, Mz)

const float NUM_SAMPLES = 50.0;
const float PI = 3.14159265359;

// --- Implementace závislostí ---
// Funkce RaySphereIntersect a GetDensity jsou identické jako v transmittance.glsl
// Pro zjednodušení předpokládáme, že jsou dostupné (např. přes #include nebo zkopírovány).

// [FUNKCE Z TRANSMITTANCE.GLSL BY BYLY ZDE ZKOPÍROVÁNY]

float GetDensity(float altitude, float scaleHeight) {
    float h = altitude - Re;
    if (h < 0.0) return 0.0;
    return exp(-h / scaleHeight);
}

vec2 RaySphereIntersect(vec3 rayOrigin, vec3 rayDir, float radius) {
    float a = dot(rayDir, rayDir);
    float b = 2.0 * dot(rayOrigin, rayDir);
    float c = dot(rayOrigin, rayOrigin) - radius * radius;
    float discriminant = b * b - 4.0 * a * c;

    if (discriminant < 0.0) {
        return vec2(-1.0);
    }

    float sqrtDisc = sqrt(discriminant);
    float t0 = (-b - sqrtDisc) / (2.0 * a);
    float t1 = (-b + sqrtDisc) / (2.0 * a);

    return vec2(t0, t1);
}

// Rayleighova fázová funkce
float PhaseFunctionR(float mu) {
    return (3.0 / (16.0 * PI)) * (1.0 + mu * mu);
}

// Mieova fázová funkce (Henyey-Greenstein)
float PhaseFunctionM(float mu) {
    float g2 = g * g;
    float denom = 1.0 + g2 - 2.0 * g * mu;
    return (3.0 / (8.0 * PI)) * ((1.0 - g2) * (1.0 + mu * mu)) / (denom * sqrt(denom));
}

// Funkce pro získání Transmittance z LUT (P1)
// Zde je potřeba namapovat fyzikální parametry na souřadnice UV textury.
vec3 SampleTransmittance(float r, float mu) {
    // 1. Mapování R (vzdálenost od středu) na U
    float u = (r - Re) / (Ra - Re);

    // 2. Mapování Mu (cos úhlu k zenitu) na V (mu jde od -1 do 1)
    float v = (mu + 1.0) * 0.5;

    // Musíte použít "imageLoad" pro čtení z Image Unit.
    // POZNÁMKA: V runtime fázi se obvykle použije texture() (Sampler), ale zde je imageLoad korektní.
    return imageLoad(transmittanceImage, ivec2(u * float(imageSize(transmittanceImage).x), v * float(imageSize(transmittanceImage).y))).rgb;
}

// --- Hlavní Logika ---

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 size = imageSize(singleScatteringImage);

    if (coord.x >= size.x || coord.y >= size.y) {
        return;
    }

    // 1. Dekódování souřadnic (identické jako v P1)
    float u = float(coord.x) / float(size.x);
    float r = Re + u * (Ra - Re);
    float v = float(coord.y) / float(size.y);
    float mu = -1.0 + v * 2.0;

    vec3 rayOrigin = vec3(0.0, r, 0.0);
    float sin_phi = sqrt(1.0 - mu * mu);
    vec3 rayDir = vec3(sin_phi, mu, 0.0);

    // Určení dráhy paprsku k hranici atmosféry
    vec2 t_intersect = RaySphereIntersect(rayOrigin, rayDir, Ra);
    float t_max = t_intersect.y;
    // ... (Zajištění ořezu pod Zemí - stejná logika jako v P1) ...
    vec2 t_ground = RaySphereIntersect(rayOrigin, rayDir, Re);
    if (t_ground.x > 0.0 && t_ground.x < t_max) {
        t_max = t_ground.x;
    }

    // Pro Single Scattering potřebujeme znát Směr Slunce (SunDirection)
    // Zde je ZJEDNODUŠENÍ: Předpokládáme, že úhel mezi pohledem a Sluncem (cos_theta)
    // je také parametrizován v textuře, NEBO že je Slunce pevně ve směru (0, 1, 0).
    // Pro jednoduchou 2D LUT to musíme předpokládat. V realitě je to 4D nebo 3D LUT.

    // Pro ukázku předpokládáme, že Slunce je v zenitu (0, 1, 0) a že je to referenční směr
    vec3 SunDir = vec3(0.0, 1.0, 0.0); // Zjednodušení pro 2D LUT

    // --- 2. Numerická Integrace Single Scattering ---

    float stepSize = t_max / NUM_SAMPLES;
    vec3 currentPos = rayOrigin;
    vec3 singleScatteredLight = vec3(0.0);

    for (int i = 0; i < int(NUM_SAMPLES); ++i) {
        // Pozice uprostřed kroku
        vec3 midPos = currentPos + rayDir * (stepSize * 0.5);
        float midAltitude = length(midPos);

        // Hustoty
        float densityR = GetDensity(midAltitude, Hr);
        float densityM = GetDensity(midAltitude, Hm);

        // Transmittance od Slunce k midPos (pomocí precomputed T_Map)
        // Toto je složitá část: musíte určit směr Slunce (SunDir) a úhel od midPos k Ra
        // Zde se zjednodušuje na vzdálenost a úhel k SunDir.

        // Zjištění úhlu od midPos k SunDir (potřebné pro LUT Transmittance)
        float cos_zenith_sun = dot(normalize(midPos), SunDir);
        vec3 T_Sun = SampleTransmittance(midAltitude, cos_zenith_sun);

        // Úhel rozptylu mezi paprskem pohledu a paprskem Slunce
        float cos_theta = dot(rayDir, SunDir);

        // Fázové funkce
        float PR = PhaseFunctionR(cos_theta);
        float PM = PhaseFunctionM(cos_theta);

        // Množství světla rozptýleného v tomto bodě do kamery
        vec3 scatteringAmount = (BetaR * densityR * PR + BetaM * densityM * PM) * stepSize;

        // Transmittance od midPos ke kameře (musí se počítat inkrementálně!)
        // U Nishita modelu se tento útlum k pozorovateli často zanedbává/zjednodušuje v P2,
        // ale pro přesnost by se měl použít.
        // Zde použijeme Transmittance z P1 pro úsek od currentPos k rayOrigin (Pozorovateli)
        float cos_zenith_view = dot(normalize(currentPos), rayDir);
        vec3 T_View = SampleTransmittance(length(currentPos), cos_zenith_view);

        // Akumulace In-Scattering
        // (Světlo ze Slunce * Rozptýlená dávka * Útlum zpět k pozorovateli)
        singleScatteredLight += T_Sun * scatteringAmount; // * T_View (pro zjednodušení T_View vynecháno)

        currentPos += rayDir * stepSize;
    }

    // 3. Zápis výsledku
    // Výsledná hodnota Single Scattering
    imageStore(singleScatteringImage, coord, vec4(0,0,1, 1.0));
}
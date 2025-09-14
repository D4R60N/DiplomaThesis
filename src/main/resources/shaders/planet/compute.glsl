#version 460 core
layout(local_size_x = 8, local_size_y = 4, local_size_z = 1) in;
layout(r32f, binding = 0) uniform image2D heightMap;
layout(rgba32f, binding = 1) uniform image2D biomeMap;

uniform vec3 seed;
uniform vec2 offset;
uniform int octaves;
uniform float elevFrequency = 0.008;
uniform float moistFrequency = 0.02;
uniform float amplitude = 100.0;
uniform int planetType = 0; // 0: Temprate, 1: Frosty, 2: Vulcanic

const float PI = 3.14159265359;

// Biome color constants
// Temprate
vec4 ROCKY     = vec4(0.45, 0.42, 0.40, 0.3); // earthy rock brown-gray
vec4 ALPINE    = vec4(0.55, 0.60, 0.65, 0.3); // cool gray-blue alpine stone
vec4 SNOW      = vec4(0.95, 0.97, 1.00, 0.4); // bright snow, slightly blue
vec4 DESERT    = vec4(0.92, 0.82, 0.55, 0.3); // sandy golden desert
vec4 FOREST    = vec4(0.15, 0.45, 0.20, 0.3); // deep green forest
vec4 TAIGA     = vec4(0.90, 0.90, 0.90, 0.3);
vec4 BEACH     = vec4(0.94, 0.88, 0.65, 0.3); // pale golden sand
vec4 PLAINS    = vec4(0.65, 0.80, 0.45, 0.3); // fresh grasslands
vec4 ICE       = vec4(0.80, 0.90, 0.95, 0.5); // icy light blue-white


// Frosty
vec4 FROSTY_MOUNTAINS = vec4(0.70, 0.82, 0.90, 0.3); // cool light blue-gray
vec4 SNOWY_MOUNTAINS  = vec4(0.85, 0.90, 0.95, 0.4); // almost white with a hint of blue
vec4 ICY_PEAKS        = vec4(0.92, 0.96, 1.00, 0.5); // crisp icy white-blue
vec4 FROSTY_DESERT    = vec4(0.70, 0.72, 0.80, 0.3); // pale beige-gray (cold desert sand)
vec4 SNOWY_PLANES     = vec4(0.88, 0.88, 0.85, 0.4); // off-white with a hint of warmth
vec4 ICE_PLANES       = vec4(0.80, 0.90, 0.95, 0.5); // soft icy cyan tint
vec4 BEDROCK          = vec4(0.35, 0.38, 0.40, 0.3); // solid cool gray stone
vec4 FROZEN_OCEAN     = vec4(0.40, 0.65, 0.80, 0.5); // teal-blue frozen waters
vec4 OCEAN            = vec4(0.05, 0.25, 0.55, 0.3); // deep cold ocean blue


// Volcanic
vec4 BARREN_MOUNTAINS = vec4(0.35, 0.32, 0.32, 0.2); // dry, ashy gray-brown
vec4 BASALT_PEAKS     = vec4(0.15, 0.15, 0.17, 0.3); // dark basalt black-gray
vec4 VOLCANOES        = vec4(0.90, 0.20, 0.05, 0.6); // intense lava red
vec4 VOLCANIC_DESERT  = vec4(0.65, 0.48, 0.28, 0.3); // scorched sandy, warm ochre
vec4 ASH_PLANES       = vec4(0.50, 0.45, 0.40, 0.2); // dusty volcanic ash
vec4 SCORCHED         = vec4(0.25, 0.12, 0.05, 0.6); // charred earth, deep brown-red
vec4 OBSIDIAN_FIELDS  = vec4(0.08, 0.08, 0.10, 0.3); // glossy black obsidian
vec4 COLD_LAVA_LAKES  = vec4(0.30, 0.05, 0.05, 0.6); // cooled lava, dark red-black
vec4 LAVA_LAKES       = vec4(1.00, 0.40, 0.00, 1.0); // glowing orange molten lava





vec4 permute(vec4 x) {
    return mod(((x * 34.0) + 1.0) * x, 289.0);
}

vec4 taylorInvSqrt(vec4 r) {
    return 1.79284291400159 - 0.85373472095314 * r;
}

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    // First corner
    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    // Other corners
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    // Offsets for corners
    vec3 x1 = x0 - i1 + 1.0 * C.xxx;
    vec3 x2 = x0 - i2 + 2.0 * C.xxx;
    vec3 x3 = x0 - 1.0 + 3.0 * C.xxx;

    // Permutations
    i = mod(i, 289.0);
    vec4 p = permute(
    permute(
    permute(i.z + vec4(0.0, i1.z, i2.z, 1.0))
    + i.y + vec4(0.0, i1.y, i2.y, 1.0))
    + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    // Gradients
    float n_ = 1.0 / 7.0; // N=7
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    // Normalise gradients
    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    // Mix final noise value
    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}

float fbm(vec3 p, float freq, float amp) {
    p += seed; // seed offset
    float total = 0.0;
    float maxValue = 0.0;
    float persistence = 0.4;
    for (int i = 0; i < octaves; i++) {
        total += snoise(p * freq) * amp;
        maxValue += amp;
        freq *= 2.0;
        amp *= persistence;
    }
    return total / maxValue;
}


float easeInQuint(float x) {
    return x * x * x * x * x;
}

vec4 getBiome(float elevation, float temperature, float lat) {
    if (planetType == 0) { // Temprate
        if (temperature > 0.6) {
            return (elevation > 0.45) ? SNOW :
            (elevation > 0.3) ? ALPINE :
            (elevation > 0.2) ? TAIGA : ICE;
        } else if (temperature > 0.35) {
            return (elevation > 0.5) ? SNOW :
            (elevation > 0.4) ? ALPINE :
            (elevation > 0.3) ? FOREST : PLAINS;
        } else {
            return (elevation > 0.4) ? ROCKY :
            (elevation > 0.3) ? DESERT : BEACH;
        }
    } else if (planetType == 1) { // Frosty
        if (temperature < 0.5) {
            return (elevation > 0.45) ? ICY_PEAKS :
                   (elevation > 0.3) ? FROSTY_MOUNTAINS :
                   (elevation > 0.2) ? OCEAN : FROZEN_OCEAN;
        } else if (temperature < 0.6) {
            return (elevation > 0.65) ? ICY_PEAKS :
                   (elevation > 0.4) ? FROSTY_MOUNTAINS :
                   (elevation > 0.3) ? FROSTY_DESERT : BEDROCK;
        } else {
            return (elevation > 0.7) ? ICY_PEAKS :
                   (elevation > 0.3) ? SNOWY_PLANES : ICE_PLANES;
        }
    } else { // Vulcanic
        if (temperature > 0.7) {
            return (elevation > 0.7) ? VOLCANOES :
                   (elevation > 0.5) ? BARREN_MOUNTAINS :
                   (elevation > 0.3) ? VOLCANIC_DESERT : OBSIDIAN_FIELDS;
        } else if (temperature > 0.5) {
            return (elevation > 0.65) ? VOLCANOES :
                   (elevation > 0.4) ? BASALT_PEAKS :
                   (elevation > 0.3) ? ASH_PLANES : COLD_LAVA_LAKES;
        } else {
            return (elevation > 0.6) ? VOLCANOES :
                   (elevation > 0.4) ? SCORCHED : LAVA_LAKES;
        }
        }
}

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    ivec2 textureSize = imageSize(heightMap)-1;

    // Normalize pixel coordinates to [0,1]
    vec2 uv = (vec2(coord) + offset) / vec2(textureSize*4);

    // Map UV to spherical coordinates
    float theta = fract(uv.x) * 2.0 * PI;
    float phi = uv.y * PI;

    // Convert spherical coords to unit sphere 3D position
    vec3 spherePos = vec3(
    sin(phi) * cos(theta),
    cos(phi),
    sin(phi) * sin(theta)
    );
    spherePos = spherePos;

    float lat = abs(cos(uv.x * PI/4));
    spherePos *= 1-easeInQuint(lat);

    //ELEVATION
    float roughness = fbm(spherePos*0.2, 0.4, 20.0);
    //roughness = floor(roughness * 4.0) / 3.0;
    float localAmplitude = mix(10.0, amplitude, roughness);
    float localFrequency = mix(elevFrequency * 0.25, elevFrequency, roughness);

    // Generate elevation and temperature values
    float elevation = fbm(spherePos * localFrequency, localFrequency, localAmplitude);
    elevation = (elevation + 1) * 0.5;
    elevation = pow(elevation, 2); // Sharper terrain
    //-----------------------------

    //temperature

    // Roughness noise for flatness control
    roughness = fbm(spherePos*0.4, 0.2, 10.0);
    //roughness = floor(roughness * 4.0) / 3.0;
    localAmplitude = mix(20.0, amplitude, roughness);
    localFrequency = mix(elevFrequency * 0.25, elevFrequency, roughness);

    float noiseMoisture = fbm(spherePos * moistFrequency, octaves, 1.0);
    noiseMoisture = (noiseMoisture + 1.0) * 0.5;
    float temperature = mix(noiseMoisture, lat, 0.5);
    //-----------------------------

    // Biome selection based on elevation + temperature
    vec4 biome = getBiome(elevation, temperature, lat);

    elevation*=2;
    imageStore(heightMap, coord, vec4(elevation, 0.0, 0.0, 1.0));
    imageStore(biomeMap, coord, biome);
}
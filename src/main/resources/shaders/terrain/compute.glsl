#version 460 core
layout(local_size_x = 8, local_size_y = 4, local_size_z = 1) in;
layout(r32f, binding = 0) uniform image2D heightMap;
layout(rgba32f, binding = 1) uniform image2D biomeMap;

uniform vec2 pos;
uniform int octaves;
uniform float elevFrequency = 0.008;
uniform float moistFrequency = 0.02;
uniform float amplitude = 100.0;

const float PI = 3.14159265359;

// Noise helpers
float rand(vec2 c){
    return fract(sin(dot(c.xy, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 p, float freq){
    float unit = 128.0 / freq;
    vec2 ij = floor(p / unit);
    vec2 xy = mod(p, unit) / unit;
    xy = 0.5 * (1.0 - cos(PI * xy));

    float a = rand(ij + vec2(0, 0));
    float b = rand(ij + vec2(1, 0));
    float c = rand(ij + vec2(0, 1));
    float d = rand(ij + vec2(1, 1));

    float x1 = mix(a, b, xy.x);
    float x2 = mix(c, d, xy.x);
    return mix(x1, x2, xy.y);
}

float fbm(vec2 x, float freq, float amp) {
    float total = 0.0;
    float maxValue = 0.0;
    float persistence = 0.4;
    for (int i = 0; i < octaves; i++) {
        total += noise(x, freq) * amp;
        maxValue += amp;
        freq *= 2.0;
        amp *= persistence;
    }
    return total / maxValue;
}

// Biome color constants
vec4 ROCKY     = vec4(0.5, 0.5, 0.5, 1.0);
vec4 ALPINE    = vec4(0.4, 0.4, 0.5, 1.0);
vec4 SNOW      = vec4(1.0, 1.0, 1.0, 1.0);
vec4 DESERT    = vec4(0.9, 0.8, 0.5, 1.0);
vec4 GRASSLAND = vec4(0.45, 0.6, 0.3, 1.0);
vec4 FOREST    = vec4(0.1, 0.4, 0.1, 1.0);
vec4 BEACH     = vec4(0.9, 0.85, 0.6, 1.0);
vec4 MARSH     = vec4(0.3, 0.4, 0.3, 1.0);
vec4 PLAINS    = vec4(0.6, 0.8, 0.4, 1.0);

void main() {
    ivec2 coord = ivec2(gl_GlobalInvocationID.xy);
    vec2 worldPos = vec2(coord) + pos;

    // Roughness noise for flatness control
    float roughness = fbm(worldPos*0.2, 0.2, 100.0);
    //roughness = floor(roughness * 4.0) / 3.0;
    float localAmplitude = mix(20.0, amplitude, roughness);
    float localFrequency = mix(elevFrequency * 0.25, elevFrequency, roughness);

    // Generate elevation and moisture values
    float elevation = fbm(worldPos * localFrequency, localFrequency, localAmplitude);
    elevation = pow(elevation, 2); // Sharper terrain

    float moisture = fbm(worldPos * moistFrequency, moistFrequency, 1.0);

    // Write height

    // Biome selection based on elevation + moisture
    vec4 biome;
//    if (moisture < 0.6) {
//        biome = (elevation > 0.7) ? SNOW :
//        (elevation > 0.5) ? ALPINE :
//        (elevation > 0.3) ? FOREST : PLAINS;
//    } else if (moisture < 0.3) {
        biome = (elevation > 0.65) ? SNOW :
        (elevation > 0.4) ? ALPINE :
        (elevation > 0.3) ? GRASSLAND : MARSH;
//    } else {
//        biome = (elevation > 0.7) ? ROCKY :
//        (elevation > 0.3) ? DESERT : BEACH;
//    }

    elevation*=2;
    imageStore(heightMap, coord, vec4(elevation, 0.0, 0.0, 1.0));
    imageStore(biomeMap, coord, biome);
}
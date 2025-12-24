#version 330 core
in vec3 vViewDir; // normalized view direction
out vec4 FragColor;

// Sun direction
uniform vec3 sunDir;

// coefficients
uniform vec3 A, B, C, D, E, F, G, H, I;

// Zenith luminance/chromaticity
uniform vec3 Z;

uniform vec3 sunColor;
uniform float sunAngularRadius;
uniform float glowRadius;

const float PI = 3.14159265359;

vec3 chi(vec3 g, float cosAlfa) {
    return (1.0 + cosAlfa * cosAlfa) / pow(1.0 + g * g - 2.0 * g * cosAlfa, vec3(1.5));
}

// extended Perez function
vec3 extendedPerez(vec3 A, vec3 B, vec3 C, vec3 D, vec3 E, vec3 F, vec3 G, vec3 H, vec3 I, float theta, float gamma) {
    float cosTheta = max(cos(theta), 0.0001);
    float cosGamma = cos(gamma);
    return (vec3(1.0) + A * exp(B / (cosTheta + 0.01))) *
    (C + D * exp(E * gamma) + F * (cosGamma * cosGamma) + G * chi(H, cosGamma) + I * sqrt(cosTheta));
}

void main() {
    vec3 view = normalize(vViewDir);

    float theta = acos(clamp(view.y, -1.0, 1.0));
    float gamma = acos(clamp(dot(view, sunDir), -1.0, 1.0));

    vec3 perez = extendedPerez(A, B, C, D, E, F, G, H, I, theta, gamma);

    vec3 L = Z * perez;

//    vec3 rgb = L;


    // Additional sun disc and glow
    //todo maybe remove
    float sunCosAngle = dot(normalize(vViewDir), normalize(sunDir));
    sunCosAngle = clamp(sunCosAngle, -1.0, 1.0);
    float sunAngle = acos(sunCosAngle);

    float sunDisc = smoothstep(sunAngularRadius, 0.0, sunAngle);

    float glow = smoothstep(glowRadius, sunAngularRadius, sunAngle);

    vec3 linearColor = L + (sunColor * (sunDisc + 0.2 * glow));

    // Tonemapping (exponential)
    float exposure = 0.1;
    vec3 mapped = vec3(1.0) - exp(-linearColor * exposure);

    // Gamma correction
    vec3 rgb = pow(mapped, vec3(1.0/2.2));

    rgb = max(rgb, vec3(0.0));

    FragColor = vec4(rgb, 1.0);
}
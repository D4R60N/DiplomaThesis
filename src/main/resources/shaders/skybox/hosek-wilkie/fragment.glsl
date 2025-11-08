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
const mat3 CIEtoRGB = mat3(
3.2406, -0.9689,  0.0557,
-1.5372,  1.8758, -0.2040,
-0.4986,  0.0415,  1.0570
);

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
    view = vec3(view.x, max(view.y, 0.0), view.z);

    float theta = acos(clamp(view.y, -1.0, 1.0));
    float gamma = acos(clamp(dot(view, sunDir), -1.0, 1.0));

    vec3 perez = extendedPerez(A, B, C, D, E, F, G, H, I, theta, gamma);

    vec3 L = Z * perez;
//
//    float exposure = 0.05;
//    vec3 mapped = vec3(1.0) - exp(-L * exposure);
//
//
//    vec3 rgb = pow(mapped, vec3(1.0/2.2));

    vec3 rgb = L;

//
//    float Y = L.z;
//    float x = L.x;
//    float y = max(L.y, 1e-4);
//    float X = (x*Y)/y;
//    float Zc = ((1-x-y)*Y)/y;
//    vec3 rgb = CIEtoRGB * vec3(X, Y, Zc);


    // Additional sun disc and glow
    //todo maybe remove
//    float sunCosAngle = dot(normalize(vViewDir), normalize(sunDir));
//    sunCosAngle = clamp(sunCosAngle, -1.0, 1.0);
//    float sunAngle = acos(sunCosAngle);
//
//    float sunDisc = smoothstep(sunAngularRadius, 0.0, sunAngle);
//
//    float glow = smoothstep(glowRadius, sunAngularRadius, sunAngle);
//
//    vec3 sunContribution = sunColor * (sunDisc + 0.2 * glow);
//
//    rgb += sunContribution;

    rgb = max(rgb, vec3(0.0));

    FragColor = vec4(rgb, 1.0);
}
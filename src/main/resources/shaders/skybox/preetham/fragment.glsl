#version 330 core
in vec3 vViewDir; // normalized view direction
out vec4 FragColor;

// Sun direction
uniform vec3 sunDir;

// Preetham Perez coefficients
uniform vec3 A, B, C, D, E;

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

// Perez function
vec3 perez(vec3 A, vec3 B, vec3 C, vec3 D, vec3 E, float theta, float gamma) {
    float cosTheta = max(cos(theta), 0.0001);
    float cosGamma = cos(gamma);
    return (vec3(1.0) + A * exp(B / cosTheta)) *
    (vec3(1.0) + C * exp(D * gamma) + E * (cosGamma * cosGamma));
}

void main() {
    vec3 view = normalize(vViewDir);

    float theta = acos(clamp(view.y, -1.0, 1.0));
    float gamma = acos(clamp(dot(view, sunDir), -1.0, 1.0));
    float thetaS = acos(clamp(sunDir.y, -1.0, 1.0));

    vec3 F = perez(A, B, C, D, E, theta, gamma);
    vec3 Fz = perez(A, B, C, D, E, 0.0, thetaS);

    Fz = max(Fz, vec3(1e-5));

    vec3 L = Z * (F / Fz);

    float Y = L.x;
    float x = L.y;
    float y = max(L.z, 1e-4);

    float X = (x / y) * Y;
    float Zc = ((1.0 - x - y) / y) * Y;

    vec3 rgb = CIEtoRGB * vec3(X, Y, Zc);

    // Additional sun disc and glow
    //todo maybe removeew
    float sunCosAngle = dot(normalize(vViewDir), normalize(sunDir));
    sunCosAngle = clamp(sunCosAngle, -1.0, 1.0);
    float sunAngle = acos(sunCosAngle);

    float sunDisc = smoothstep(sunAngularRadius, 0.0, sunAngle);

    float glow = smoothstep(glowRadius, sunAngularRadius, sunAngle);

    vec3 sunContribution = sunColor * (sunDisc + 0.2 * glow);

    rgb += sunContribution;

    rgb = max(rgb, vec3(0.0));

    FragColor = vec4(rgb, 1.0);
}
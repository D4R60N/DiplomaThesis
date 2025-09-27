#version 330 core
in vec3 vViewDir;
out vec4 fragColor;

uniform vec3 sunDir;
uniform vec3 betaR;
uniform vec3 betaM;
uniform float HR;
uniform float HM;
uniform float g;

const float PI = 3.14159265359;

float rayleighPhase(float cosTheta) {
    return (3.0 / (16.0 * PI)) * (1.0 + cosTheta * cosTheta);
}

float miePhase(float cosTheta, float g) {
    return (3.0 / (8.0 * PI)) * ((1.0 - g*g) * (1.0 + cosTheta*cosTheta)) /
    pow((1.0 + g*g - 2.0*g*cosTheta), 1.5);
}

void main() {
    vec3 view = normalize(vViewDir);
    float cosTheta = dot(view, normalize(sunDir));

    // Approximate optical depth
    float mu = dot(view, vec3(0.0, 1.0, 0.0));
    float rayleighDepth = exp(-mu / HR);
    float mieDepth = exp(-mu / HM);

    vec3 rayleigh = betaR * rayleighPhase(cosTheta) * rayleighDepth;
    vec3 mie = betaM * miePhase(cosTheta, g) * mieDepth;

    vec3 color = rayleigh + mie;

    // Add sun glow
    float sunIntensity = max(dot(view, sunDir), 0.0);
    color += vec3(1.0, 0.9, 0.6) * pow(sunIntensity, 200.0);

    // Tone mapping
    color = 1.0 - exp(-color * 10.0);

    fragColor = vec4(color, 1.0);
}

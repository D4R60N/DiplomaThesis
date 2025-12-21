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

// In your GLSL Shader
float density(float altitude, float scaleHeight) {
    // Altitude is distance from planet center - Planet Radius
    return exp(-(altitude - R_e) / scaleHeight);
}

// Numerical Integration (e.g., using steps for simplicity)
float opticalDepth(vec3 start, vec3 end, float scaleHeight) {
    float opticalDepth = 0.0;
    vec3 delta = (end - start) / float(NUM_SAMPLES);
    vec3 current = start;

    for (int i = 0; i < NUM_SAMPLES; i++) {
        vec3 next = current + delta;
        float h_curr = length(current); // Distance from center
        float h_next = length(next);

        // Use average density over the step
        float avg_density = 0.5 * (Density(h_curr, scaleHeight) + Density(h_next, scaleHeight));

        opticalDepth += avg_density * length(delta);
        current = next;
    }
    return opticalDepth;
}

void main() {

}

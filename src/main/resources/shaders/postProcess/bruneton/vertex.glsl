#version 450

in vec3 position;
uniform mat4 viewMatInv;
uniform mat4 projMatInv;

out vec3 view_ray;
out vec2 fragTexCoord;

void main() {
    vec4 vertex = vec4(position, 1.0);
    view_ray = (viewMatInv * vec4((projMatInv * vertex).xyz, 0.0)).xyz;
    gl_Position = vertex;
    fragTexCoord = position.xy * 0.5 + 0.5;
}
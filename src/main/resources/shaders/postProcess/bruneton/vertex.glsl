#version 450

in vec3 position;
uniform mat4 viewMatInv;
uniform mat4 projMatInv;
uniform vec3 camPos;

out vec3 view_ray;
out vec2 fragTexCoord;

void main() {
    vec4 vertex = vec4(position, 1.0);
    view_ray = (inverse(viewMatInv) * vec4((inverse(projMatInv) * vertex).xyz, 0.0)).xyz;
    gl_Position = vertex;
    fragTexCoord = position.xy * 0.5 + 0.5;
}
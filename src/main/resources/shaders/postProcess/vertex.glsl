#version 400 core

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out vec2 fragTexCoord;

void main() {
    gl_Position = vec4(position, 1);
    fragTexCoord = texCoord;
}
#version 330 core
layout (location = 0) in vec3 aPos;

out vec3 vViewDir;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;

void main() {
    vViewDir = aPos;
    vec4 pos = projectionMatrix * viewMatrix * vec4(aPos, 1.0);
    gl_Position = pos.xyww;
}
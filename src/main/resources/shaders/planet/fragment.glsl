#version 400 core

const int MAX_POINT_LIGHTS = 5;
const int MAX_SPOT_LIGHTS = 5;

in vec2 fragTexCoord;
in vec3 fragNormal;
in vec3 fragPosition;
in vec2 texCoordOut;

out vec4 fragColor;

struct Material {
    vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    int hasTexture;
    float reflectance;
};

struct DirectionalLight {
    vec3 direction;
    vec3 color;
    float intensity;
};

struct PointLight {
    vec3 color;
    vec3 position;
    float intensity;
    float constant;
    float linear;
    float exponent;
};

struct SpotLight {
    PointLight pl;
    vec3 coneDirection;
    float cutOff;
};

uniform Material material;
uniform float specularPower;
uniform DirectionalLight directionalLight;
uniform PointLight pointLights[MAX_POINT_LIGHTS];
uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
uniform sampler2D biomeMap;

vec4 ambientC;
vec4 diffuseC;
vec4 specularC;
const float cellSize = 0.01;

void setupColours(Material material, vec2 texCoords) {
    if (material.hasTexture == 0) {
        vec4 sampled = texture(biomeMap, texCoordOut+vec2(0, cellSize)) * 0.25f +
        texture(biomeMap, texCoordOut+vec2(cellSize, 0)) * 0.25f +
        texture(biomeMap, texCoordOut+vec2(0, -cellSize)) * 0.25f +
        texture(biomeMap, texCoordOut+vec2(-cellSize, 0)) * 0.25f;
        ambientC = sampled * 0.1;
        diffuseC = sampled;
        specularC = sampled;
    } else {
        ambientC = material.ambient;
        diffuseC = material.diffuse;
        specularC = material.specular;
    }
}

vec4 calcLightColor(vec3 lightColor, float lightIntensity, vec3 position, vec3 toLightDir, vec3 normal) {
    vec4 diffuseColor = vec4(0);
    vec4 specularColor = vec4(0);

    // Diffuse
    float diff = max(dot(normal, toLightDir), 0.0);
    diffuseColor = diffuseC * vec4(lightColor, 1.0) * lightIntensity * diff;

    // Specular (Phong)
    vec3 cameraDir = normalize(-position);
    vec3 fromLightDir = -toLightDir;
    vec3 reflectedLight = normalize(reflect(fromLightDir, normal));
    float spec = pow(max(dot(cameraDir, reflectedLight), 0.0), specularPower);
    specularColor = specularC * vec4(lightColor, 1.0) * lightIntensity * spec * material.reflectance;

    return diffuseColor + specularColor;
}

vec4 calcPointLight(PointLight light, vec3 normal, vec3 position) {
    vec3 lightDir = light.position - position;
    vec3 toLightDir = normalize(lightDir);
    vec4 lightColor = calcLightColor(light.color, light.intensity, position, toLightDir, normal);

    float distance = length(lightDir);
    float attenuation = light.constant + light.linear * distance + light.exponent * distance * distance;
    return lightColor / attenuation;
}

vec4 calcSpotLight(SpotLight light, vec3 normal, vec3 position) {
    vec3 lightDir = light.pl.position - position;
    vec3 toLightDir = normalize(lightDir);
    float spotAlfa = dot(toLightDir, normalize(-light.coneDirection));

    vec4 color = vec4(0);
    if (spotAlfa > light.cutOff) {
        color = calcPointLight(light.pl, normal, position);
        color *= (1.0 - (1.0 - spotAlfa) / (1.0 - light.cutOff));
    }
    return color;
}

vec4 calcDirectionalLight(DirectionalLight light, vec3 normal, vec3 position) {
    vec3 toLightDir = normalize(-light.direction);
    return calcLightColor(light.color, light.intensity, position, toLightDir, normal);
}

void main() {
    setupColours(material, fragTexCoord);

    // FIX: Normals must be re-normalized in the fragment shader
    vec3 normal = normalize(fragNormal);

    // FIX: Start with Ambient, then ADD other lights
    vec4 totalLight = ambientC*0.01;

    // Directional Light
    totalLight += calcDirectionalLight(directionalLight, normal, fragPosition);

//    // Point Lights
//    for(int i = 0; i < MAX_POINT_LIGHTS; i++) {
//        if (pointLights[i].intensity > 0.0)
//        totalLight += calcPointLight(pointLights[i], normal, fragPosition);
//    }
//
//    // Spot Lights
//    for(int i = 0; i < MAX_SPOT_LIGHTS; i++) {
//        if (spotLights[i].pl.intensity > 0.0)
//        totalLight += calcSpotLight(spotLights[i], normal, fragPosition);
//    }

    fragColor = totalLight;
}
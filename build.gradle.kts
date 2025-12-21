plugins {
    id("java")
}

group = "palecek"
version = "1.0-SNAPSHOT"
var imguiVersion = "1.90.0"
val lwjglVersion = "3.3.3"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("palecek.OpenGL:library:1.2.+")
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengles:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.joml:joml:1.10.5")
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengles:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
}

tasks.test {
    useJUnitPlatform()
}
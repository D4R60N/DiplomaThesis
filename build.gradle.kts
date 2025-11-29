plugins {
    id("java")
}

group = "palecek"
version = "1.0-SNAPSHOT"
var imguiVersion = "1.90.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("palecek.OpenGL:library:1.2.+")
    implementation("org.joml:joml:1.10.5")
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
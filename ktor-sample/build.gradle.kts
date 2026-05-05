plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.10"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
    //dependsOn("jsTest")
}

/*
tasks.register<Exec>("jsTest") {
    dependsOn("npmInstall")
    workingDir = file("src/test/js")
    environment("test_output", file("build/test-results/test"))
    commandLine("cmd", "/c", "npm", "run", "test")
    isIgnoreExitValue = false
}

tasks.register<Exec>("npmInstall") {
    workingDir = file("src/test/js")
    commandLine("cmd", "/c", "npm", "install")
}
*/

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.config.yaml)

    implementation("io.ktor:ktor-server-pebble:2.3.4")
    implementation("org.jetbrains.exposed:exposed-core:0.53.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.53.0")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("org.jetbrains.kotlinx:dataframe:1.0.0-Beta4")
    implementation("org.jetbrains.kotlinx:dataframe-csv:1.0.0-Beta4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-server-websockets:2.3.4")
    implementation("io.ktor:ktor-server-sessions:2.3.4")

    implementation("at.favre.lib:bcrypt:0.10.2")

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

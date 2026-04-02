plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
	kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.11"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    implementation("io.ktor:ktor-server-pebble:3.4.0")
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework:spring-jdbc:6.2.5")
    implementation("org.springframework.security:spring-security-crypto:6.4.4")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("org.springframework.security:spring-security-crypto")

}

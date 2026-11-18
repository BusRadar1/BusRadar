import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    id("busradar.spring-service")
}

dependencies {
    implementation(project(":libs:events"))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.kafka)
    implementation(libs.spring.boot.jdbc)
    implementation(libs.spring.boot.flyway)
    implementation(libs.flyway.database.postgresql)
    compileOnly(platform(SpringBootPlugin.BOM_COORDINATES))
    compileOnly(libs.lombok)
    annotationProcessor(platform(SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor(libs.lombok)
    runtimeOnly(platform(SpringBootPlugin.BOM_COORDINATES))
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.awaitility)
}
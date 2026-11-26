plugins {
    id("busradar.spring-service")
}

dependencies {
    implementation(project(":libs:events"))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.kafka)
    implementation(libs.spring.boot.data.redis)

    testImplementation(libs.spring.boot.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.awaitility)
}
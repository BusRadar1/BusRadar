plugins {
    id("busradar.spring-service")
}

dependencies {
    implementation(project(":libs:events"))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.kafka)
    implementation(libs.spring.boot.restclient)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.wiremock)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

tasks.processTestResources {
    from(rootProject.file("../fixtures/ztm")) {
        include("*.jsonl.gz")
        into("ztm")
    }
}

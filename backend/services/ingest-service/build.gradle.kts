plugins {
    id("busradar.spring-service")
}

dependencies {
    implementation(project(":libs:events"))
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.actuator)
    implementation(libs.spring.boot.restclient)
    testImplementation(libs.spring.boot.test)
    testImplementation(libs.wiremock)
}

tasks.processTestResources {
    from(rootProject.file("../fixtures/ztm")) {
        include("*.jsonl.gz")
        into("ztm")
    }
}

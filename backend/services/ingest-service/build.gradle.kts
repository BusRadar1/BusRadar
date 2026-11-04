plugins {
    id("busradar.spring-service")
}

dependencies {
    implementation(libs.spring.boot.webmvc)
    implementation(libs.spring.boot.actuator)
    testImplementation(libs.spring.boot.test)
}

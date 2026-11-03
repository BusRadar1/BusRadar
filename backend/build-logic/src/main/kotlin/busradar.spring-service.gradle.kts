import org.gradle.api.tasks.SourceSetContainer
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("busradar.java-library")
    id("org.springframework.boot")
}

val mainSourceSet = extensions.getByType<SourceSetContainer>().named("main")

tasks.named<BootJar>("bootJar") {
    onlyIf("Service does not contain Java sources yet") {
        mainSourceSet.get().allJava.files.isNotEmpty()
    }
}
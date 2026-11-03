import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.spotless))
}

fun DependencyHandlerScope.plugin(
    plugin: Provider<PluginDependency>
) = plugin.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

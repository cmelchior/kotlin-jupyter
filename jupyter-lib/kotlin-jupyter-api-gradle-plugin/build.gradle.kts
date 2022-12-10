import build.CreateResourcesTask
import build.util.defaultVersionCatalog
import build.util.devKotlin

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.plugin.publish)
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("libs.publisher")
}

project.group = "org.jetbrains.kotlin"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Temporary solution until Kotlin 1.4 will be supported in
    // .kts buildscripts, and it will be possible to use
    // kotlinx.serialization in plugin code
    implementation(libs.kotlin.gradle.gradle)
    implementation(libs.gson)
    implementation(libs.plugin.ksp)

    testImplementation(projects.api)
    testImplementation(projects.apiAnnotations)
}

CreateResourcesTask.register(project, "saveVersion", tasks.processResources) {
    addSingleValueFile("VERSION", rootSettings.mavenVersion)
    addSingleValueFile("KOTLIN_VERSION", rootProject.defaultVersionCatalog.versions.devKotlin)
}

java {
    withSourcesJar()
    withJavadocJar()
}

buildSettings {
    withLanguageLevel("1.4")
    withTests()
}

val pluginName = "apiGradlePlugin"

@Suppress("UnstableApiUsage")
gradlePlugin {
    // These settings are set for the whole plugin bundle
    website.set(rootSettings.projectRepoUrl)
    vcsUrl.set(rootSettings.projectRepoUrl)

    plugins {
        create(pluginName) {
            id = "org.jetbrains.kotlin.jupyter.api"
            implementationClass = "org.jetbrains.kotlinx.jupyter.api.plugin.ApiGradlePlugin"
            displayName = "Kotlin Jupyter kernel integration plugin"
            description = "Gradle plugin providing a smooth Jupyter notebooks integration for Kotlin libraries"
            tags.addAll("jupyter", "kernel", "kotlin")
        }
    }
}

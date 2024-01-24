buildscript {
    val jacocoVersion by rootProject.extra("0.8.11")
    val minAndroidVersion by rootProject.extra { 29 }
    val compileAndroidVersion by rootProject.extra { 34 }
    val androidBuildToolsVersion by rootProject.extra { "34.0.0" }
    val composeKotlinCompilerVersion by rootProject.extra { "1.5.0" }
    val configDir by rootProject.extra { "$rootDir/config" }
    val baseNamespace by rootProject.extra { "uk.gov.android.ui" }

    val localProperties = java.util.Properties()
    if (rootProject.file("local.properties").exists()) {
        println(localProperties)
        localProperties.load(java.io.FileInputStream(rootProject.file("local.properties")))
    }

    fun findPackageVersion(): String {
        var version = "1.0.0"

        println(localProperties)
        if (rootProject.hasProperty("packageVersion")) {
            version = rootProject.property("packageVersion") as String
        } else if (localProperties.getProperty("packageVersion") != null) {
            version = localProperties.getProperty("packageVersion") as String
        }

        return version
    }

    val packageVersion by rootProject.extra { findPackageVersion() }

    dependencies {
        classpath("org.jacoco", "org.jacoco.core", jacocoVersion)
        classpath("org.jacoco", "org.jacoco.ant", jacocoVersion)
        classpath("org.jacoco", "org.jacoco.report", jacocoVersion)
        classpath("org.jacoco", "org.jacoco.agent", jacocoVersion)
    }
}

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    id("org.sonarqube") version "4.4.1.3373"
    id("uk.gov.network.sonarqube-root-config")
    alias(libs.plugins.kotlin.serialization) apply false
}

apply {
    from("$rootDir/config/styles/tasks.gradle")
}

tasks.register("check") {
    description = "Run the Vale linting check"
    group = JavaBasePlugin.VERIFICATION_GROUP
    dependsOn("vale")
}
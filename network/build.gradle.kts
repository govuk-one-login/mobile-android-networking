import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    id("jacoco")
    id("kotlin-parcelize")
    id("maven-publish")
    id("uk.gov.network.jvm-toolchains")
    // id("uk.gov.network.sonarqube-module-config")
    id("uk.gov.network.jacoco-module-config")
}

apply(from = "${rootProject.extra["configDir"]}/detekt/config.gradle")
apply(from = "${rootProject.extra["configDir"]}/ktlint/config.gradle")

android {
    namespace = "uk.gov.android.network"
    compileSdk = (rootProject.extra["compileAndroidVersion"] as kotlin.Int)

    defaultConfig {
        minSdk = (rootProject.extra["minAndroidVersion"] as kotlin.Int)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
    }

    lint {
        abortOnError = true
        absolutePaths = true
        baseline = File("${rootProject.extra["configDir"]}/android/baseline.xml")
        checkAllWarnings = true
        checkDependencies = false
        checkGeneratedSources = false
        checkReleaseBuilds = true
        disable.addAll(
            setOf(
                "ConvertToWebp",
                "UnusedIds",
                "VectorPath"
            )
        )
        explainIssues = true
        htmlReport = true
        ignoreTestSources = true
        ignoreWarnings = false
        lintConfig = File("${rootProject.extra["configDir"]}/android/lint.xml")
        noLines = false
        quiet = false
        showAll = true
        textReport = true
        warningsAsErrors = true
        xmlReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.all {
            it.testLogging {
                events = setOf(
                    org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
                )
            }
        }
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    listOf(
        libs.core.ktx,
        libs.appcompat,
        libs.material
    ).forEach { dependency ->
        implementation(dependency)
    }

    testImplementation(libs.junit)

    listOf(
        libs.ext.junit,
        libs.espresso.core
    ).forEach { dependency ->
        androidTestImplementation(dependency)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "uk.gov.android"
            version = rootProject.extra["packageVersion"] as String

            artifact("$buildDir/outputs/aar/${project.name}-release.aar")
        }
    }
    repositories {
        maven("https://maven.pkg.github.com/govuk-one-login/mobile-android-network") {
            if (file("${rootProject.projectDir.path}/github.properties").exists()) {
                val propsFile = File("${rootProject.projectDir.path}/github.properties")
                val props = Properties().also { it.load(FileInputStream(propsFile)) }
                val ghUsername = props["username"] as String?
                val ghToken = props["token"] as String?

                credentials {
                    username = ghUsername
                    password = ghToken
                }
            } else {
                credentials {
                    username = System.getenv("USERNAME")
                    password = System.getenv("TOKEN")
                }
            }
        }
    }
}

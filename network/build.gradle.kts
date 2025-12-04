import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import uk.gov.pipelines.config.ApkConfig

plugins {
    id("uk.gov.pipelines.android-lib-config")
    alias(libs.plugins.kotlin.serialization)
}

android {
    defaultConfig {
        val apkConfig: ApkConfig by project.rootProject.extra
        namespace = apkConfig.applicationId
        compileSdk = apkConfig.sdkVersions.compile
        minSdk = apkConfig.sdkVersions.minimum
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "KTOR_VERSION",
            "\"${libs.versions.ktor.get()}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
    buildFeatures {
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.all {
            it.useJUnitPlatform()
            it.testLogging {
                events =
                    setOf(
                        TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
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
        libs.ktor.client.android,
        libs.ktor.client.core,
        libs.ktor.client.contentnegotiation,
        libs.ktor.client.logging,
        libs.ktor.serialization.kotlinx.json,
        libs.logging,
    ).forEach { dependency ->
        implementation(dependency)
    }

    listOf(
        platform(libs.junit.bom),
        libs.junit.jupiter,
        libs.junit.jupiter.params,
        libs.ktor.client.mock,
        libs.mockito.core,
        libs.mockito.kotlin,
    ).forEach { dependency ->
        testImplementation(dependency)
    }

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    listOf(
        libs.ext.junit,
        libs.espresso.core,
    ).forEach { dependency ->
        androidTestImplementation(dependency)
    }
}

mavenPublishingConfig {
    mavenConfigBlock {
        name.set(
            "Mobile Android Networking",
        )
        description.set(
            """
            A Gradle module with support for API calls including certificate pinning to AWS and setting user agent
            """.trimIndent(),
        )
    }
}

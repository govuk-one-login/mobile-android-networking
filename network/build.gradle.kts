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

    testFixtures {
        enable = true
    }

    packaging {
        resources.excludes += "META-INF/LICENSE*"
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
        platform(libs.kotlin.bom),
        libs.kotlinx.coroutines,
        libs.core.ktx,
        libs.ktor.client.android,
        libs.ktor.client.core,
        libs.ktor.client.contentnegotiation,
        libs.ktor.client.logging,
        libs.ktor.serialization.kotlinx.json,
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
        libs.kotlinx.coroutines.test,
    ).forEach { dependency ->
        testImplementation(dependency)
    }

    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter)

    listOf(
        libs.ext.junit,
        libs.espresso.core,
        libs.mockwebserver,
        libs.okhttp.tls,
        libs.kotlinx.coroutines.test,
    ).forEach { dependency ->
        androidTestImplementation(dependency)
    }

    androidTestUtil(libs.androidx.test.orchestrator)
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

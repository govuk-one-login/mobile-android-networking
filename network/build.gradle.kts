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

    lint {
        val configDir = "${rootProject.projectDir}/config"

        abortOnError = true
        absolutePaths = true
        baseline = File("$configDir/android/baseline.xml")
        checkAllWarnings = true
        checkDependencies = false
        checkGeneratedSources = false
        checkReleaseBuilds = true
        disable.addAll(
            setOf(
                "ConvertToWebp",
                "UnusedIds",
                "VectorPath",
            ),
        )
        explainIssues = true
        htmlReport = true
        ignoreTestSources = true
        ignoreWarnings = false
        lintConfig = File("$configDir/android/lint.xml")
        noLines = false
        quiet = false
        showAll = true
        textReport = true
        warningsAsErrors = true
        xmlReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
        unitTests.all {
            it.testLogging {
                events = setOf(
                    org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED,
                    org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED,
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
        libs.ktor.client.android,
        libs.ktor.client.core,
        libs.ktor.client.contentnegotiation,
        libs.ktor.client.logging,
        libs.ktor.serialization.kotlinx.json,
    ).forEach { dependency ->
        implementation(dependency)
    }

    listOf(
        libs.junit.jupiter,
        libs.junit.jupiter.params,
        libs.ktor.client.mock,
        platform(libs.junit.bom),
        libs.mockito.core,
        libs.mockito.kotlin,
    ).forEach { dependency ->
        testImplementation(dependency)
    }

    testRuntimeOnly(libs.junit.jupiter.engine)

    listOf(
        libs.ext.junit,
        libs.espresso.core,
    ).forEach { dependency ->
        androidTestImplementation(dependency)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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

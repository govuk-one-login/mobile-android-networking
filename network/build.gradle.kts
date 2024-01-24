import org.gradle.api.internal.provider.MissingValueException

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    id("jacoco")
    id("kotlin-parcelize")
    id("maven-publish")
    id("uk.gov.network.jvm-toolchains")
    id("uk.gov.network.sonarqube-module-config")
    id("uk.gov.network.jacoco-module-config")
    alias(libs.plugins.kotlin.serialization)
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
        libs.material,
        libs.ktor.client.android,
        libs.ktor.client.core,
        libs.ktor.client.contentnegotiation,
        libs.ktor.client.logging,
        libs.ktor.serialization.kotlinx.json,
        libs.logback.classic
    ).forEach { dependency ->
        implementation(dependency)
    }

    listOf(
        libs.junit.jupiter,
        libs.junit.jupiter.params,
        libs.ktor.client.mock,
        platform(libs.junit.bom),
        libs.mockito.core,
        libs.mockito.kotlin
    ).forEach { dependency ->
        testImplementation(dependency)
    }

    testRuntimeOnly(libs.junit.jupiter.engine)

    listOf(
        libs.ext.junit,
        libs.espresso.core
    ).forEach { dependency ->
        androidTestImplementation(dependency)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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
        maven(
            "https://maven.pkg.github.com/govuk-one-login/mobile-android-network",
            setupGithubCredentials()
        )
    }
}

fun setupGithubCredentials(): MavenArtifactRepository.() -> Unit = {
    val (credUser, credToken) = fetchGithubCredentials()
    credentials {
        username = credUser
        password = credToken
    }
}

fun fetchGithubCredentials(): Pair<String, String> {
    val gprUser = providers.gradleProperty("gpr.user")
    val gprToken = providers.gradleProperty("gpr.token")

    return try {
        gprUser.get() to gprToken.get()
    } catch (exception: MissingValueException) {
        logger.warn(
            "Could not find 'Github Package Registry' properties. Refer to the proceeding " +
                "location for instructions:\n\n" +
                "${rootDir.path}/docs/developerSetup/github-authentication.md\n",
            exception
        )

        System.getenv("USERNAME") to System.getenv("TOKEN")
    }
}

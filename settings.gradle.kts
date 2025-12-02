@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    includeBuild("${rootProject.projectDir}/mobile-android-pipelines/buildLogic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven(
            "https://maven.pkg.github.com/govuk-one-login/mobile-android-logging",
            setGithubCredentials()
        )
    }
}

rootProject.name = "mobile-android-networking"
include(":network")

fun setGithubCredentials(): MavenArtifactRepository.() -> Unit = {
    credentials {
        username = providers.gradleProperty("gpr.user").get()
        password = providers.gradleProperty("gpr.token").get()
    }
}

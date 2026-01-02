pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Kunci versi plugin biar konsisten (opsional tapi membantu)
    plugins {
        id("com.android.application") version "8.7.2"
        id("org.jetbrains.kotlin.android") version "1.9.25"
        id("org.jetbrains.kotlin.plugin.parcelize") version "1.9.25"
        id("com.google.gms.google-services") version "4.4.2"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Jagafakta"
include(":app")

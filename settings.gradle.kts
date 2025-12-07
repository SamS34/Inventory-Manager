// In your C:\Garage\AndroidStudioCode\settings.gradle.kts file

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // THIS IS CRITICAL
        maven("https://jitpack.io")
    }
}

rootProject.name = "Inventory Manager"
include(":app")
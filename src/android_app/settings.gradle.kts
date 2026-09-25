// Workaround for conflicting ANDROID_PREFS_ROOT and ANDROID_USER_HOME env vars
try {
    val processEnvironment = Class.forName("java.lang.ProcessEnvironment")
    val theEnvironmentField = processEnvironment.getDeclaredField("theEnvironment").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val env = theEnvironmentField.get(null) as? MutableMap<String, String>
    env?.remove("ANDROID_PREFS_ROOT")
    val theCaseInsensitiveEnvironmentField = processEnvironment.getDeclaredField("theCaseInsensitiveEnvironment").apply { isAccessible = true }
    @Suppress("UNCHECKED_CAST")
    val cienv = theCaseInsensitiveEnvironmentField.get(null) as? MutableMap<String, String>
    cienv?.remove("ANDROID_PREFS_ROOT")
} catch (e: Throwable) {
    // Fallback if reflection is restricted
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android_app"
include(":app")

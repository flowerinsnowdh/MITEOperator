pluginManagement {
    plugins {
        repositories {
            System.getenv("GRADLE_PLUGIN_PORTAL_MIRROR")?.let {
                maven(it)
            }
            gradlePluginPortal()
        }
    }
}
rootProject.name = "miteoperator"
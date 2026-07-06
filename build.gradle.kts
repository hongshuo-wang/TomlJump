plugins {
    kotlin("jvm") version "2.0.21" apply false
    id("org.jetbrains.intellij.platform") apply false
}

allprojects {
    group = "com.tomljump"
    version = providers.gradleProperty("pluginVersion").get()
}

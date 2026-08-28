buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:8.5.35")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.asset.pack) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

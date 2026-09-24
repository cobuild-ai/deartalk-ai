import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.play.publisher)
}

android {
    namespace = "ai.deartalk.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "ai.deartalk.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "1.1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    assetPacks += ":gemma_asset_pack"

    signingConfigs {
        create("release") {
            val keystorePropertiesFile = file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val properties = Properties()
                keystorePropertiesFile.inputStream().use { properties.load(it) }
                storeFile = file(properties.getProperty("RELEASE_STORE_FILE") ?: "deartalk.keystore")
                storePassword = properties.getProperty("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = properties.getProperty("RELEASE_KEY_ALIAS") ?: ""
                keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            val keystorePropertiesFile = file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Google LiteRT-LM (On-Device Gemma .litertlm Inference Engine)
    implementation(libs.litertlm.android)
    // Google MediaPipe Tasks GenAI
    implementation(libs.mediapipe.tasks.genai)
    // Google Play Asset Delivery
    implementation(libs.play.asset.delivery)
    implementation(libs.play.asset.delivery.ktx)

    // Google ML Kit On-Device Translation (Track 1 Ultra-fast Draft 50ms)
    implementation(libs.mlkit.translate)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}

play {
    val serviceAccountFile = rootProject.file("play-service-account.json")
    val localSaFile = file("play-service-account.json")
    when {
        serviceAccountFile.exists() -> serviceAccountCredentials.set(serviceAccountFile)
        localSaFile.exists() -> serviceAccountCredentials.set(localSaFile)
    }
    track.set("internal")
    releaseStatus.set(com.github.triplet.gradle.androidpublisher.ReleaseStatus.DRAFT)
    resolutionStrategy.set(com.github.triplet.gradle.androidpublisher.ResolutionStrategy.AUTO_OFFSET)
}


import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.scribe.caligrafia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.scribe.caligrafia"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val propsFile = rootProject.file("keystore/keystore.properties")
            val props = Properties()
            if (propsFile.exists()) {
                FileInputStream(propsFile).use { props.load(it) }
            }

            val keystorePath = props.getProperty("RELEASE_STORE_FILE")
                ?: (project.findProperty("RELEASE_STORE_FILE") as? String)
                ?: System.getenv("RELEASE_STORE_FILE")
                ?: "keystore/scribe-release.jks"
            val keystoreFile = rootProject.file(keystorePath)

            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
                    ?: (project.findProperty("RELEASE_KEYSTORE_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
                    ?: ""
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
                    ?: (project.findProperty("RELEASE_KEY_ALIAS") as? String)
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                    ?: "scribe_release_key"
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
                    ?: (project.findProperty("RELEASE_KEY_PASSWORD") as? String)
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                    ?: ""
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile?.exists() == true && !releaseSigning.storePassword.isNullOrBlank()) {
                signingConfig = releaseSigning
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = false
        }
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Android Ink API (M0 — Stylus Lab)
    implementation(libs.androidx.ink.authoring)
    implementation(libs.androidx.ink.strokes)
    implementation(libs.androidx.ink.rendering)
    implementation(libs.androidx.ink.brush)
    implementation(libs.androidx.ink.geometry)

    testImplementation(libs.junit)


    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    binaryResultsDirectory.set(layout.buildDirectory.dir("test-bin/${name}-${System.currentTimeMillis()}"))
}


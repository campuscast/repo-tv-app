plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val releaseStoreFileProp = providers.gradleProperty("CAMPUSCAST_RELEASE_STORE_FILE")
val releaseStorePasswordProp = providers.gradleProperty("CAMPUSCAST_RELEASE_STORE_PASSWORD")
val releaseKeyAliasProp = providers.gradleProperty("CAMPUSCAST_RELEASE_KEY_ALIAS")
val releaseKeyPasswordProp = providers.gradleProperty("CAMPUSCAST_RELEASE_KEY_PASSWORD")
val hasCustomReleaseSigning = releaseStoreFileProp.isPresent &&
    releaseStorePasswordProp.isPresent &&
    releaseKeyAliasProp.isPresent &&
    releaseKeyPasswordProp.isPresent

android {
    namespace = "com.campuscast.tvplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.campuscast.tvplayer"
        minSdk = 28
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasCustomReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFileProp.get())
                storePassword = releaseStorePasswordProp.get()
                keyAlias = releaseKeyAliasProp.get()
                keyPassword = releaseKeyPasswordProp.get()

                // Keep older and newer Android package verifiers happy for sideload installs.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // By default release becomes installable for Android TV sideloading.
            // Custom release keystore can be provided via Gradle properties.
            signingConfig = if (hasCustomReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // Some Android 9/10 TV firmwares fail to install APKs with extractNativeLibs=false.
            // Legacy packaging keeps install behavior stable on older boxes.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // 1.1.7 includes the Android artifact ProGuard fix for datastore-preferences-core.
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")

    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("androidx.media3:media3-common:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

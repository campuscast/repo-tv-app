plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

tasks.register<Copy>("assembleAndroid9PlusSideloadApk") {
    group = "distribution"
    description = "Builds signed release APK for Android 9+ TV devices and copies it into builds/."
    dependsOn(":app:assembleRelease")

    val sourceApk = layout.projectDirectory.file("app/build/outputs/apk/release/app-release.apk")
    from(sourceApk)
    into(layout.projectDirectory.dir("builds"))
    rename { "campuscast-tv-player-android9plus-release.apk" }
}

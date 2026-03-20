# CampusCast Android TV Player (`repo-tv-player`)

Android TV native player implementing the same runtime pipeline as `repo-desktop-app` with TV-adapted UX.

## Android support

- Minimum supported Android version: **Android 9 (API 28)**
- Target Android version: **API 35**
- Validated compatibility intent: **Android 9 / 10 / 11+**

## Stack

- Kotlin
- Jetpack Compose (TV-focused layouts and focusable controls)
- Media3 ExoPlayer
- OkHttp
- DataStore (config persistence)
- File-based manifest/cache persistence
- Coroutines + StateFlow

## Implemented parity pipeline

- Setup + activation (request code + poll credentials)
- Config persistence
- Device revalidation on startup
- Release + manifest fetch
- Last-known-good fallback (current + previous manifest)
- Asset prefetch + cache verify + safe cleanup
- Playback evaluator (slot priority + publication item timing)
- Metadata mapping (transition, trim, mute, loop)
- Heartbeat telemetry
- Offline fallback behavior
- Service/status/settings screens
- D-pad/back/menu navigation

## Project layout

- `app/src/main/kotlin/com/campuscast/tvplayer/app` - app shell, repository, view model
- `app/src/main/kotlin/com/campuscast/tvplayer/core/network` - backend client
- `app/src/main/kotlin/com/campuscast/tvplayer/core/storage` - DataStore and manifest/cache files
- `app/src/main/kotlin/com/campuscast/tvplayer/core/cache` - content cache lifecycle
- `app/src/main/kotlin/com/campuscast/tvplayer/core/playback` - manifest validation, fallback selector, schedule evaluation, metadata mapping
- `app/src/main/kotlin/com/campuscast/tvplayer/core/telemetry` - heartbeat payload/reporting
- `app/src/main/kotlin/com/campuscast/tvplayer/feature/*` - setup/activation/playback/status/settings screens

## Notes

- MQTT release push is intentionally not implemented yet; release sync currently runs by periodic pull (30s) with identical fallback behavior.

## Build installable APK (Android 9+ sideload)

Main command:

```bash
./gradlew assembleAndroid9PlusSideloadApk
```

Output artifact:

- `builds/campuscast-tv-player-android9plus-release.apk` (signed, installable)

The task always builds `:app:assembleRelease` first, then copies the signed APK to `builds/`.

## Signing behavior

- By default, `release` uses the Android debug signing key so the produced APK is installable out of the box for local sideload testing.
- For stable release signing (recommended for repeated updates on real devices), set these Gradle properties:
  - `CAMPUSCAST_RELEASE_STORE_FILE`
  - `CAMPUSCAST_RELEASE_STORE_PASSWORD`
  - `CAMPUSCAST_RELEASE_KEY_ALIAS`
  - `CAMPUSCAST_RELEASE_KEY_PASSWORD`

Example (`~/.gradle/gradle.properties`):

```properties
CAMPUSCAST_RELEASE_STORE_FILE=/absolute/path/to/release.keystore
CAMPUSCAST_RELEASE_STORE_PASSWORD=***
CAMPUSCAST_RELEASE_KEY_ALIAS=campuscast-tv
CAMPUSCAST_RELEASE_KEY_PASSWORD=***
```

## Important install note

- `app-release-unsigned.apk` is not installable on Android devices.
- If device already has the app signed with a different key, uninstall old app once before installing a newly signed build.

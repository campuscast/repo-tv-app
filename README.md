# CampusCast Android TV Player (`repo-tv-player`)

Android TV native player implementing the same runtime pipeline as `repo-desktop-app` with TV-adapted UX.

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
- Gradle wrapper is not included in this workspace snapshot; add wrapper or run with local Gradle toolchain.

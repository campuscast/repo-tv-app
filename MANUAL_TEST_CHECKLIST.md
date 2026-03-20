# Manual Verification Checklist (Android 9 / 10 / 11+ TV)

## 0) Build artifact

1. Run `./gradlew assembleAndroid9PlusSideloadApk`.
2. Confirm artifact exists: `builds/campuscast-tv-player-android9plus-release.apk`.
3. Verify APK is signed (`apksigner verify --verbose builds/campuscast-tv-player-android9plus-release.apk`).

## 1) Android 9 (API 28)

1. Install APK from file manager or `adb install -r`.
2. Confirm app launches from TV launcher.
3. Confirm first launch shows Setup screen.
4. Enter valid Player ID + URLs, continue to Activation.
5. Confirm activation code appears and countdown updates.
6. Confirm transition to Playback after activation.
7. Open service menu with DPAD_UP or MENU.
8. Confirm status screen shows connection/playback/cache/heartbeat data.
9. Trigger `Sync now` and verify release or fallback state.
10. Verify image slot playback.
11. Verify video slot playback.
12. Verify publication custom slide playback.
13. Verify publication video item playback and item sequencing.
14. Disconnect network; verify playback continues from cached content and offline state is visible.
15. Reconnect network; verify sync recovers and cache status updates.
16. From Settings, change API/MQTT URLs and save.
17. Verify BACK from status/settings returns to playback.
18. Verify BACK from playback opens service status.
19. Restart app; verify config + last-known-good manifest + cache persist.

## 2) Android 10 (API 29)

1. Install same APK on Android 10 TV box (`adb install -r` or file manager).
2. Confirm installation succeeds (no "App not installed").
3. Repeat launch, setup/activation, navigation, sync, image/video/custom slide checks from Android 9 list.
4. Confirm no immediate crash during playback start.

## 3) Android 11+ (API 30+)

1. Install same APK on Android 11+ TV device/emulator.
2. Confirm launch and playback path work as on Android 9/10.
3. Confirm DPAD navigation and BACK behavior are unchanged.

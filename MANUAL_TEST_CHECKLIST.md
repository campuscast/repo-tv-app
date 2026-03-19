# Manual Verification Checklist (Android TV / Emulator)

1. First launch shows Setup screen.
2. Enter valid Player ID + URLs, continue to Activation.
3. Activation code appears; expiry countdown updates.
4. After backend activation, app transitions to Playback.
5. Open service menu with DPAD_UP or MENU.
6. Status screen shows connection/playback/cache/heartbeat data.
7. Trigger `Sync now` and verify release or fallback state.
8. Disconnect network; verify playback continues from cached content and offline state is visible.
9. Reconnect network; verify sync recovers and cache status updates.
10. Verify image slot playback.
11. Verify video slot playback.
12. Verify publication custom slide playback.
13. Verify publication video item playback and item sequencing.
14. Verify metadata behavior:
    - fade transition
    - trim in/out
    - mute
    - loop
15. From Settings, change API/MQTT URLs and save.
16. Back navigation:
    - BACK from status/settings returns to playback
    - BACK from playback opens service status
17. Restart app; verify config + last-known-good manifest + cache persist.

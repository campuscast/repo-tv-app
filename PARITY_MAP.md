# Parity Map vs `repo-desktop-app`

## Full parity

- Setup and first launch state machine
- Activation request/poll flow
- Device config persistence
- Device revalidation behavior (exists/missing/unknown)
- Release fetch + manifest fetch
- Manifest payload usability validation
- Last-known-good fallback semantics
- Asset prefetch / verify / cleanup lifecycle
- Slot schedule evaluation with priority
- Publication item timing cycle (`duration_ms`)
- Playback metadata mapping (`transition`, `trim_in/out`, `mute`, `loop`)
- Heartbeat telemetry payload structure
- Status/error/loading/idle/offline UI states

## Partial parity

- Release update trigger:
  - Desktop: MQTT push + periodic fallback poll
  - TV: MQTT push + periodic fallback poll
  - Reason: both runtimes now converge on the same release propagation model; TV still keeps a lightweight pull fallback for missed pushes

- Multi-display:
  - Desktop: control window + per-display playback windows
  - TV: single-display TV runtime
  - Reason: Android TV device model

- Desktop-only settings:
  - Desktop: global shortcut, autolaunch OS integration
  - TV: not applicable in Android TV environment

## Not implemented

- None beyond the intentional MQTT push adaptation.

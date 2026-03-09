# Changelog (Asuka) / 变更记录（Asuka）

## Unreleased / 未发布
- Initial clean-room rewrite scaffold (M0–M4)
- Gestures + controls + overlays
- Persistence (position/speed/zoom/track)
- Queue planning + recent history persistence
- Tests and checklists
- Architecture hardening:
  - `AsukaAppGraph` + `PlaybackCoreGraph` establish a single composition root for playback runtime wiring
  - `PlaybackLaunchCoordinator` and `PlaybackSessionCoordinator` split launch/session responsibilities
  - `PlaybackSessionPlanner` and typed repositories centralize queue/resume policy
  - Queue history persists independently from implicit next/previous queue planning
  - `PlaybackSessionHost` and `MainLibraryScreen` extract orchestration out of oversized activity files
  - `BackgroundPlaybackPolicy` centralizes background/PiP retention rules
- Regression coverage added for launch forwarding, session planning, track restore timing, subtitle-off UI state, and background retention

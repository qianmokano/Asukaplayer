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
- Runtime and UI organization:
  - new `player-runtime` module owns runtime graph, settings repositories, launch parsing, and playback environment adapters
  - media library / settings flow split into `MainLibraryNavHost`, `LibraryPages`, `LibraryChrome`, and focused settings page files
  - playback startup now accepts `ACTION_SEND` / `ACTION_SEND_MULTIPLE` in addition to `ACTION_VIEW` / `ClipData`
  - controller connection failures now surface as retryable UI state instead of a blank screen
  - `PlayerUiStateHolder` progress updates now run only while playback is active instead of keeping a permanent ticker alive
- Regression coverage added for launch forwarding, session planning, track restore timing, subtitle-off UI state, and background retention

# Changelog (Asuka) / 变更记录（Asuka）

## Unreleased / 未发布
- Initial clean-room rewrite scaffold (M0–M4)
- Gestures + controls + overlays
- Persistence (position/speed/zoom/track)
- Queue planning + recent history persistence
- Tests and checklists
- Architecture hardening:
  - `AsukaAppGraph` remains the single graph, while `AsukaAppComponentFactory` + narrow registries inject `MainActivity` / `PlaybackActivity` / `PlaybackService` without `Application` provider lookups
  - `PlaybackLaunchCoordinator` and `PlaybackSessionCoordinator` split launch/session responsibilities
  - `PlaybackSessionPlanner` and typed repositories centralize queue/resume policy
  - Queue history persists independently from implicit next/previous queue planning
  - `PlaybackSessionHost` and `MainLibraryScreen` extract orchestration out of oversized activity files
  - `BackgroundPlaybackPolicy` centralizes background/PiP retention rules
  - `PlaybackStateWriter` is now the single write entry for speed/track playback persistence; UI overlays no longer write store state directly
- Runtime and UI organization:
  - new `player-runtime` module owns runtime graph, settings repositories, launch parsing, and playback environment adapters
  - theme/settings models in `player-runtime` now use pure ARGB/value types instead of Compose `Color`
  - media library now uses `AndroidVideoAccessDataSource` / `AndroidMediaStoreVideoCatalogDataSource` / `PlaybackRecentMediaDataSource` + `MediaLibraryRepository` + dedicated use cases
  - media library / settings flow split into `MainLibraryNavHost`, `LibraryPages`, `LibraryChrome`, and focused settings page files
  - former `ThemeEngine.kt` and `SharedUiComponents.kt` were replaced by focused theme and shared-UI files
  - playback startup now accepts `ACTION_SEND` / `ACTION_SEND_MULTIPLE` in addition to `ACTION_VIEW` / `ClipData`
  - controller connection failures now surface as retryable UI state instead of a blank screen
  - `PlayerUiStateHolder` progress updates now run only while playback is active instead of keeping a permanent ticker alive
- Regression coverage added for launch forwarding, session planning, track restore timing, subtitle-off UI state, background retention, and media library use cases

# Changelog

All notable changes to Asuka Player are documented in this file.

## [0.2.0] - 2026-08-22

### Added

- Remember the device playback volume between playback sessions without changing the system volume outside playback.
- Configure independent seek-forward and seek-backward intervals for playback buttons and gestures.
- Build signed release APKs for individual Android ABIs alongside a universal APK.

### Improved

- Made media-library indexing, incremental refresh, folder pagination, and persistence recovery more reliable.
- Hardened playback queue URI grants, resume fallback, cache recovery, and shutdown-time state persistence.
- Refined the library interface, launcher resources, localized settings, and playback controls.
- Expanded automated coverage for media indexing, settings persistence, playback recovery, gestures, and controls.

### Fixed

- Fixed playback UI state races and transient media URI handling.
- Fixed edge cases in thumbnail extraction, media refresh, queue restoration, and Picture-in-Picture transitions.

## [0.1.2] - 2026-03-16

- First public release of Asuka Player.

[0.2.0]: https://github.com/qianmokano/Asukaplayer/compare/v0.1.2...v0.2.0
[0.1.2]: https://github.com/qianmokano/Asukaplayer/releases/tag/v0.1.2

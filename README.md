# Asuka Player

Asuka Player is an open-source Android local video player built with Jetpack Compose and Media3/ExoPlayer.

It is designed as a modern, local-first playback app: fast library access, polished playback controls, reliable state restore, and a codebase that stays maintainable as the product grows.

Chinese README: [简体中文](docs/README.zh-CN.md)

## Why Asuka Player

- Local-first video playback for files you already own
- Modern Android UI built with Jetpack Compose and Material 3
- Rich playback controls including gestures, track switching, speed control, and PiP
- Indexed media library for faster browsing and incremental sync
- Clean modular architecture instead of one oversized app module

## What You Can Do

### Browse

- Browse local videos by folder
- Page through large media libraries
- View recent playback history
- Keep the library in sync with local media changes automatically

### Play

- Open videos from the in-app library
- Launch playback from `ACTION_VIEW`, `ACTION_SEND`, `ACTION_SEND_MULTIPLE`, and `ClipData`
- Seek with gestures and the progress bar
- Control brightness, volume, zoom, pan, aspect ratio, subtitles, audio tracks, and playback speed
- Continue playback with resume state, restored speed, and restored track selections

### Keep Context

- Resume playback position
- Persist playback speed, audio/subtitle choices, zoom state, and queue history
- Use Picture-in-Picture and background playback retention
- Avoid blocking player callbacks with serialized asynchronous persistence

## Project Status

- Active development
- Versioning currently follows `0.x.y`
- Android targets: `minSdk 23`, `targetSdk 36`, `compileSdk 36`
- Core stack: Kotlin `2.2.10`, Jetpack Compose, Media3 `1.9.1`

## Technical Highlights

- Jetpack Compose UI with a dedicated playback UI module
- Media3 / ExoPlayer integration isolated behind explicit playback ports
- Room-backed local media index for paged library reads
- DataStore-backed app settings
- Room-backed playback state and queue history
- Regression-oriented JVM, Robolectric, and Compose test coverage
- Build-time architecture boundary checks

## Module Layout

```text
app/               App entry, library feature, settings pages, top-level composition
player-contract/   Stable Kotlin contracts, session planning, persistence and playback ports
player-platform/   Android and Media3 bindings, intent adapters, async writers, platform helpers
player-render-api/ Renderer-neutral playback surface contracts
player-renderer/   PlaybackActivity, session assembly, PiP, Media3 render adapters
player-runtime/    Runtime graph, settings repositories, launch orchestration, device/persistence wiring
player-ui/         Pure playback UI and gesture orchestration, no direct Media3 dependency
player-engine/     PlaybackService and Media3 controller implementations
player-domain/     Pure JVM algorithms and state machines
player-data/       Room/DataStore implementations, local media index, migration coverage
```

Primary dependency direction:

`app` -> `player-runtime` / `player-platform` / `player-renderer` / `player-data` / `player-engine`

`player-renderer` -> `player-render-api` / `player-ui` / `player-platform` / `player-contract`

`player-ui` -> `player-render-api` / `player-contract` / `player-domain`

`player-runtime` -> `player-contract` / `player-platform` / `player-data`

`player-engine` -> `player-contract` / `player-platform`

`player-data` -> `player-contract`

## Architecture at a Glance

- `AsuraPlayerApp` is the single composition root
- Playback launch inputs are normalized into one `PlaybackSessionRequest`
- Playback host responsibilities are split into connection, launch driving, and state feeds
- `player-ui` consumes contracts and render APIs only, not raw Media3 types
- Media library reads come from a local Room-backed index instead of direct ad hoc MediaStore scans
- Runtime settings and persistence paths use explicit boundaries and asynchronous write semantics

This structure keeps the app easier to test, easier to refactor, and less likely to accumulate hidden cross-layer coupling.

## Getting Started

### Requirements

- JDK 17 for Android compilation; JVM unit tests use a Java 21 launcher via Gradle toolchains
- Android SDK configured for the repository toolchain
- A local Android development environment capable of running Gradle Android builds

### Build

```bash
./gradlew :app:compileDebugKotlin
```

### Default Local Verification

```bash
./gradlew test
./gradlew :player-ui:compileDebugAndroidTestKotlin
./gradlew verifyArchitectureBoundaries verifySourceFileSizes
./gradlew help
```

### Useful Commands

```bash
# Print the centralized app version
./gradlew printAppVersion

# Lint
./gradlew lintDebug

# Device / emulator UI tests
./gradlew :player-ui:connectedAndroidTest

# Install debug APK
./gradlew :app:installDebug
```

## Development Philosophy

This project intentionally treats architecture and regression safety as part of the product:

- UI behavior should be tested, not just implemented
- playback and persistence paths should be explicit, not hidden behind side effects
- module boundaries should be enforced by the build
- local media handling should remain robust as the app scales

## Documentation

- [Architecture](docs/ARCHITECTURE.md) / [架构说明](docs/ARCHITECTURE.md)
- [Testing](docs/TESTING.md) / [测试说明](docs/TESTING.md)
- [Versioning](docs/VERSIONING.md) / [版本管理](docs/VERSIONING.md)

## Open Source Intent

Asuka Player is not only a playback app project, but also a maintainable Android architecture exercise: local media, playback runtime, rendering, UI, and persistence are separated on purpose so the repository can stay readable and evolvable over time.

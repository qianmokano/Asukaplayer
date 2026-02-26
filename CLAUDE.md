# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Asuka Player is a clean-room Android local video player rewrite built with Jetpack Compose and Media3/ExoPlayer. The project is organized as a multi-module Gradle project with strict separation of concerns across 5 modules.

## Build & Development Commands

```bash
# Compile Kotlin for debug
./gradlew :app:compileDebugKotlin

# Install debug APK on connected device
./gradlew :app:installDebug

# Run all JVM unit tests (fast, no device needed)
./gradlew :player-domain:test :player-core:test

# Run a single test class
./gradlew :player-domain:test --tests "com.asuka.player.domain.GestureAlgorithmsTest"

# Run UI/instrumented tests (requires connected device or emulator)
./gradlew :player-ui:connectedAndroidTest

# Lint
./gradlew lint

# Full check
./gradlew check
```

## Module Architecture

```
app/                  → Library browsing, settings, file picker; launches PlaybackActivity
player-ui/            → Compose UI, gesture orchestration, UI state management
player-core/          → Media3/ExoPlayer integration, MediaSessionService, queue logic
player-domain/        → Pure JVM algorithms (no Android deps) — gesture math & state machines
player-data/          → Persistence abstractions (PlaybackStore interface + InMemory impl)
```

Dependency direction: `app` → `player-ui` → `player-core` → `player-data`; `player-ui` → `player-domain`

`player-domain` and `player-data` are pure JVM libraries with no Android dependencies, making them fast to test with JUnit alone.

## Key Architecture Decisions

**Gesture system is split across two layers:**
- `player-domain/GestureAlgorithms.kt` — pure math (seek delta, zoom, brightness/volume mapping). No side effects, fully unit tested.
- `player-ui/controller/GestureCoordinator.kt` — coordinates gesture types, updates UI state and calls `PlaybackController`.
- `player-ui/controller/GestureStateMachine.kt` (in domain) — state machine that enforces gesture exclusivity.

**Playback control flow:**
1. `PlaybackActivity` connects to `PlaybackService` (a `MediaSessionService`) via `MediaController`.
2. `Media3PlaybackController` wraps `MediaController` and implements the `PlaybackController` interface.
3. `PlayerUiStateHolder` holds all Compose UI state (controls visibility, gesture overlays, etc.).
4. `ControllerBindings` wires UI events to controller methods.

**State persistence:** `PlaybackStateWriter` writes position/speed/track indices to `PlaybackStore` on pause/stop. `PlaybackStateRestorer` reads it back on resume.

**Queue management:** `QueuePlanner` and `IntentQueueReader` handle video queue assembly from intents and `ClipData`.

## Key Source Files

| File | Purpose |
|------|---------|
| `app/…/MainActivity.kt` | Library UI + video scanner + launches playback |
| `player-ui/…/PlaybackActivity.kt` | Playback screen host |
| `player-ui/…/PlayerScreen.kt` | Root Compose composable for player |
| `player-ui/…/controller/GestureCoordinator.kt` | Gesture orchestration |
| `player-ui/…/controller/PlayerUiStateHolder.kt` | All UI state |
| `player-core/…/PlaybackController.kt` | Abstract playback interface |
| `player-core/…/impl/Media3PlaybackController.kt` | ExoPlayer/Media3 implementation |
| `player-core/…/service/PlaybackService.kt` | MediaSessionService |
| `player-domain/…/GestureAlgorithms.kt` | Pure gesture math |
| `player-domain/…/GestureStateMachine.kt` | Gesture state machine |
| `player-data/…/PlaybackStore.kt` | Persistence interface |

## Tech Stack

- **Kotlin** 2.3.0, **JDK** 17
- **Jetpack Compose** (BOM 2026.01.00) + **Material3**
- **Media3** 1.9.1 (ExoPlayer + MediaSession)
- **Coroutines** 1.10.2
- **minSdk** 23, **targetSdk/compileSdk** 36
- **Testing:** JUnit 4, Robolectric 4.12.2 (JVM tests), Compose UI test (instrumented)

## Testing Notes

- `player-domain` and `player-core` unit tests run on JVM — prefer these for logic changes.
- `player-ui` tests are Compose instrumented tests and require a device/emulator.
- Test output (pass/fail/skip events) is configured in the root `build.gradle.kts`.
- Docs with test plans are in `docs/M4_TEST_PLAN.md` and `docs/M4_UI_TEST_PLAN.md`.

## Documentation

`docs/` contains milestone plans, known issues, performance checklists, and a Chinese-language project overview (`PROJECT_OVERVIEW.md`). `docs/STATUS_AND_TODO.md` tracks completion status and remaining work items.

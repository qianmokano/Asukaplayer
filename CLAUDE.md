# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Asuka Player is a clean-room Android local video player rewrite built with Jetpack Compose and Media3/ExoPlayer. The project is organized as a multi-module Gradle project with strict separation of concerns across 5 modules.

## Build & Development Commands

```bash
# Compile Kotlin for debug
./gradlew :app:compileDebugKotlin

# Run all JVM unit tests (fast, no device needed)
./gradlew test

# Run a single test class
./gradlew :player-core:testDebugUnitTest --tests "com.asuka.player.core.PlaybackSessionPlannerTest"

# Lint
./gradlew lintDebug

# Run UI/instrumented tests (requires connected device or emulator)
./gradlew :player-ui:connectedAndroidTest

# Install debug APK on connected device
./gradlew :app:installDebug

# Full local verification
./gradlew test lintDebug
```

## Module Architecture

```
app/                  → Library browsing, settings, file picker; launches PlaybackActivity
player-ui/            → Compose playback UI, gesture orchestration, session coordination
player-core/          → Media3/ExoPlayer integration, MediaSessionService, queue/restore planning
player-domain/        → Pure JVM algorithms (no Android deps) — gesture math & state machines
player-data/          → Persistence abstractions and SharedPreferences-backed stores
```

Dependency direction: `app` → `player-ui` → `player-core` → `player-data`; `player-ui` → `player-domain`

`player-domain` is a pure JVM library. `player-data`, `app`, `player-core`, and `player-ui` all have JVM/Robolectric test coverage.

## Key Architecture Decisions

**Gesture system is split across two layers:**
- `player-domain/GestureAlgorithms.kt` — pure math (seek delta, zoom, brightness/volume mapping). No side effects, fully unit tested.
- `player-ui/controller/GestureCoordinator.kt` — coordinates gesture types, updates UI state and calls `PlaybackController`.
- `player-ui/controller/GestureStateMachine.kt` (in domain) — state machine that enforces gesture exclusivity.

**Application wiring:**
1. `AsuraPlayerApp` builds `AsukaAppGraph`.
2. `AsukaAppGraph` implements `PlaybackCoreGraph`, and `AsuraPlayerApp` installs it into `PlaybackCoreRegistry` so `player-core` can resolve runtime dependencies from the application composition root.

**Playback launch and control flow:**
1. `MainActivity` uses `PlaybackLaunchCoordinator` to resolve the playback URI, forward/remap `ClipData`, and package `PlaybackRuntimeSettings`.
2. `PlaybackActivity` connects to `PlaybackService` (a `MediaSessionService`) via `MediaController`.
3. `PlaybackSessionCoordinator` asks `PlaybackSessionPlanner` for a `PlaybackSessionPlan` and applies it to the controller.
4. `Media3PlaybackController` wraps `MediaController` and implements the `PlaybackController` interface.
5. `PlayerUiStateHolder` holds Compose UI state, while `ControllerBindings` wires UI events to controller methods.

**State persistence:** `PlaybackStateWriter` writes position/speed/stable track IDs to `PlaybackStore`. `PlaybackStateRepository` reads typed resume state back, and `PlaybackSessionPlanner` decides what should actually be restored for the new session.

**Queue management:** `PlaybackLaunchCoordinator` preserves external `ClipData` queue information, `IntentQueueReader` reads launch neighbors, `QueuePlanner` merges those with queue history, and queue history now persists alongside playback resume state.

**Background retention:** `BackgroundPlaybackPolicy` centralizes whether the playback session should remain attached across backgrounding, PiP, and manual background-play requests.

## Key Source Files

| File | Purpose |
|------|---------|
| `app/…/AppGraph.kt` | Application dependency graph and playback runtime composition root |
| `app/…/PlaybackLaunchCoordinator.kt` | Playback intent assembly and URI/ClipData forwarding |
| `app/…/MainActivity.kt` | Library activity shell + launches playback |
| `app/…/MainLibraryScreen.kt` | Library Compose UI |
| `player-ui/…/PlaybackActivity.kt` | Playback screen host |
| `player-ui/…/PlaybackSessionHost.kt` | MediaController/session lifecycle host for playback |
| `player-ui/…/PlayerScreen.kt` | Root Compose composable for player |
| `player-ui/…/controller/PlaybackSessionCoordinator.kt` | Applies planned queue/resume state to `MediaController` |
| `player-ui/…/controller/BackgroundPlaybackPolicy.kt` | Background/PiP retention policy |
| `player-ui/…/controller/GestureCoordinator.kt` | Gesture orchestration |
| `player-ui/…/controller/PlayerUiStateHolder.kt` | All UI state |
| `player-core/…/PlaybackController.kt` | Abstract playback interface |
| `player-core/…/PlaybackSessionPlanner.kt` | Queue + resume + track-restore planning |
| `player-core/…/PlaybackCoreGraph.kt` | Runtime dependency contract exposed from the app graph |
| `player-core/…/impl/Media3PlaybackController.kt` | ExoPlayer/Media3 implementation |
| `player-core/…/service/PlaybackService.kt` | MediaSessionService |
| `player-domain/…/GestureAlgorithms.kt` | Pure gesture math |
| `player-domain/…/GestureStateMachine.kt` | Gesture state machine |
| `player-data/…/PlaybackStore.kt` | Persistence interface and related stores |

## Tech Stack

- **Kotlin** 2.3.0, **JDK** 17
- **Jetpack Compose** (BOM 2026.01.00) + **Material3**
- **Media3** 1.9.1 (ExoPlayer + MediaSession)
- **Coroutines** 1.10.2
- **minSdk** 23, **targetSdk/compileSdk** 36
- **Testing:** JUnit 4, Robolectric 4.12.2 (JVM tests), Compose UI test (instrumented)

## Testing Notes

- `./gradlew test` is the current baseline local verification command and covers all JVM unit test suites in the repo.
- `player-domain` tests are plain JVM tests; `player-data` uses Robolectric-backed unit tests for Android persistence code.
- `app`, `player-core`, and `player-ui` use JUnit/Robolectric for launch/session/controller logic.
- Instrumented tests remain available through `:player-ui:connectedAndroidTest` and still require a device/emulator.
- Test output (pass/fail/skip events) is configured in the root `build.gradle.kts`.
- Docs with test plans are in `docs/M4_TEST_PLAN.md` and `docs/M4_UI_TEST_PLAN.md`.

## Documentation

`docs/` contains milestone plans, known issues, performance checklists, and a Chinese-language project overview (`PROJECT_OVERVIEW.md`). `docs/STATUS_AND_TODO.md` tracks completion status and remaining work items.

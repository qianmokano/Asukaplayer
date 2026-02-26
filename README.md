# Asuka Player（清洁室重写）

This folder contains a clean-room rewrite based on `docs/PLAYER_REWRITE_REPORT.md`.
No code/assets are copied from the original project.

本目录是基于 `docs/PLAYER_REWRITE_REPORT.md` 的清洁室重写实现。
未复制原项目代码或资源。

## Milestones（里程碑）
- M0: base playback skeleton (service + controller + minimal UI)
- M1: gestures + controls state
- M2: full UI layout skeleton
- M3: persistence + queue + track selection + PIP/background behavior
- M4: tests + perf checklist + release docs

## Current Status（当前状态）
- M0 ✅
- M1 ✅
- M2 ✅
- M3 ✅ (core features done; notification foregrounding pending)
- M4 ✅ (initial test coverage + checklists)

## Modules (scaffold)（模块脚手架）
- player-core: playback engine/service interfaces
- player-domain: use cases + gesture algorithms (pure functions)
- player-data: persistence abstractions
- player-ui: Compose UI + interaction layer

## Test（测试）
- Unit tests: player-domain, player-core
- UI tests: player-ui androidTest

## Notes（备注）
- Background playback does not yet promote the service to foreground.

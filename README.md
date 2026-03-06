# Asuka Player

一款 Android 本地视频播放器，基于 Jetpack Compose + Media3/ExoPlayer 构建。

---

## 特性

- 手势控制：左右拖动快进/快退，左侧上下调亮度，右侧上下调音量，双指缩放
- 双击快进/快退，长按加速播放
- 音频轨道 / 字幕轨道选择
- 视频缩放模式切换
- 画中画（PiP）& 后台播放
- 播放状态持久化（位置、速度、音轨、字幕、缩放）
- 队列播放（支持 ClipData 多文件）
- 深色模式完整适配

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material3 |
| 播放引擎 | Media3 / ExoPlayer 1.9.1 |
| 语言 | Kotlin 2.3.0 |
| 最低版本 | Android 6.0（API 23）|
| 目标版本 | Android 16（API 36）|

## 模块结构

```
app/            → 媒体库浏览、文件选择、设置页，启动播放
player-ui/      → Compose 播放页、手势协调、会话编排与 UI 状态管理
player-core/    → Media3/ExoPlayer 封装、MediaSessionService、队列/恢复规划
player-domain/  → 纯 JVM 算法（手势数学、状态机），无 Android 依赖
player-data/    → 持久化抽象与 SharedPreferences 落盘实现（PlaybackStore、QueueHistoryStore、AppSettingsStore）
```

依赖方向：`app` → `player-ui` → `player-core` → `player-data`；`player-ui` → `player-domain`

## 关键运行链路

1. `AsuraPlayerApp` 构建 `AsukaAppGraph`，并通过 `PlaybackCoreRegistry` 向 `player-core` 暴露统一运行时依赖
2. `MainActivity` 通过 `PlaybackLaunchCoordinator` 解析待播放 URI、处理 seek fallback、转发 `ClipData` 队列与运行时设置
3. `PlaybackActivity` 通过 `PlaybackSessionHost` 承载 `MediaController` 生命周期，再由 `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 规划队列、恢复位置/速度/轨道状态并应用到控制器
4. `PlaybackService` 与 `PlaybackStateWriter` 负责 MediaSession 与播放状态写回；`BackgroundPlaybackPolicy` 负责 PiP/后台保活策略

## 构建

```bash
# 编译
./gradlew :app:compileDebugKotlin

# JVM 单元测试（覆盖 app / player-core / player-domain / player-ui / player-data）
./gradlew test

# Lint
./gradlew lintDebug

# UI 测试（需连接设备/模拟器）
./gradlew :player-ui:connectedAndroidTest

# 安装到设备
./gradlew :app:installDebug

# 生成第三方依赖许可/Notices（输出到 build/reports/third-party-notices/）
./gradlew generateThirdPartyNotices
```

## 开发进度

- M0 基础播放骨架 ✅
- M1 手势与控制栏状态 ✅
- M2 完整 UI 布局 ✅
- M3 持久化、队列、轨道选择、PiP ✅
- M4 测试、性能检查、发布文档 ✅

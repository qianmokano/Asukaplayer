# Asuka Player

一款基于 Jetpack Compose + Media3/ExoPlayer 的 Android 本地视频播放器。

## 当前能力

- 本地媒体库浏览、文件夹分组、最近播放
- 外部 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` / `ClipData` 多文件启动
- 手势控制：横向 seek、亮度、音量、双击、长按倍速、缩放、平移
- 音轨 / 字幕切换、比例切换、倍速切换
- PiP、后台播放、播放恢复
- 播放状态持久化：位置、速度、音轨、字幕、缩放、最近历史

## 技术栈

| 层 | 技术 |
|---|---|
| UI | Jetpack Compose + Material 3 |
| 播放引擎 | Media3 / ExoPlayer 1.9.1 |
| 语言 | Kotlin 2.3.0 |
| Android | minSdk 23 / targetSdk 36 / compileSdk 36 |
| 测试 | JUnit 4、Robolectric、Compose UI Test |

## 模块结构

```text
app/            应用入口、component factory、媒体库 data source/repository/use case、设置页、主题/UI 组件
player-runtime/ 设置仓库、运行时 graph、播放启动编排、设备/持久化适配
player-ui/      播放页 UI、会话宿主、UI 侧状态与动作翻译
player-core/    播放抽象、MediaSessionService、队列/恢复规划
player-domain/  纯 JVM 算法与状态机
player-data/    持久化接口与 SharedPreferences 实现
```

依赖方向：`app` → `player-runtime`；`player-runtime` → `player-ui` / `player-core` / `player-data`；`player-ui` → `player-core` / `player-domain`

## 关键架构

1. `AsuraPlayerApp` 构建 `AsukaAppGraph`，组装 `MainActivityDependencies` / `PlaybackActivityDependencies` / `PlaybackServiceDependencies`，并注册到启动 registry。
2. `AsukaAppComponentFactory` 负责实例化 `MainActivity` / `PlaybackActivity` / `PlaybackService`，系统入口不再通过 `Application` 反查 provider。
3. `MainActivity` 和 `PlaybackLaunchCoordinator` 负责解析 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 启动 URI、seek fallback 与显式队列转发。
4. `MainLibraryViewModel` 通过 `MediaLibraryRepository` + use case 读取权限状态、本地媒体库、最近播放和缩略图预热，不再直接访问 MediaStore helper。
5. `PlaybackSessionHost` 使用注入的播放依赖连接 `MediaController`，并把 Media3 状态翻译成 `PlaybackScreenModel` / `PlaybackScreenDependencies` 给 `PlayerScreen`。
6. `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 负责队列、续播位置、倍速和轨道恢复；`PlaybackStateWriter` 作为 speed/track/position 的单一落盘入口。

## 当前代码组织

- 媒体库与设置页已经拆成 feature-oriented 文件：
  - `MainLibraryScreen` 负责状态汇总与 launcher
  - `MainLibraryNavHost` 负责导航装配
  - `MediaLibraryDataSources` / `MediaLibraryRepository` / `MainLibraryViewModel` 形成 data source -> repository -> use case -> view model 的媒体库链路
  - `LibraryPages` / `SettingsPageContent` / `PlayerSettingsPageContent` / `ThemeSettingsScreen` / `MotionSettingsPageContent` 负责具体页面内容
- 主题与共享 UI 已按职责拆分：
  - `AsukaTheme` / `ThemeColorUtils` / `ThemeSwatchComponents` / `CustomThemeEditorSheet`
  - `UiComponentTokens` / `GroupedSurfaceComponents` / `SettingsNavigationRows` / `SettingsControlRows`
- 播放进度刷新不再是 attach 后常驻轮询，而是只在 `player.isPlaying` 时启动短周期 ticker

## 本地验证

```bash
# Kotlin 编译
./gradlew :app:compileDebugKotlin

# 全量 JVM 单元测试
./gradlew test

# AndroidTest 源码编译预检（无设备）
./gradlew :player-ui:compileDebugAndroidTestKotlin

# Lint
./gradlew lintDebug

# 真机 / 模拟器 UI 测试
./gradlew :player-ui:connectedAndroidTest
```

当前无设备默认基线：

- `./gradlew test`
- `./gradlew :player-ui:compileDebugAndroidTestKotlin`

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [测试说明](docs/TESTING.md)
- [路线与待办](docs/ROADMAP.md)
- [变更记录](docs/CHANGELOG.md)

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
app/              应用入口、媒体库 data source/repository/use case、设置页、主题/UI 组件
player-contract/  纯 Kotlin 播放契约、队列/会话规划、设置与持久化接口
player-platform/  Android binding 层、窄依赖接口、Intent/seek fallback、Media3 共享适配器
player-runtime/   设置仓库、运行时 graph、启动编排、设备/持久化实现装配
player-ui/        播放页 UI、会话宿主、UI 侧状态与动作翻译，不直接依赖 engine 实现
player-engine/    PlaybackService、Media3 controller/connector 实现
player-domain/    纯 JVM 算法与状态机
player-data/      DataStore/Room 持久化实现、legacy SharedPreferences 迁移源、schema/compat 测试
```

依赖方向：`app` → `player-runtime` / `player-platform` / `player-ui`；`player-runtime` → `player-contract` / `player-platform` / `player-engine` / `player-data`；`player-ui` → `player-contract` / `player-platform` / `player-domain`；`player-engine` → `player-contract` / `player-platform`；`player-data` → `player-contract`

## 关键架构

1. `AsuraPlayerApp` 现在只做 composition root：构建 `AsukaAppGraph`，再委托 `AppCompositionFactory` 产出 `MainActivityDependencies`、platform 层的 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`。
2. `AsuraPlayerApp` 持有窄依赖 container，`MainActivity` / `PlaybackActivity` / `PlaybackService` 在各自入口内从 `Application` 读取依赖，不再依赖静态 registry。
3. `MainActivity` 和 `PlaybackLaunchCoordinator` 负责解析 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 启动 URI、seek fallback 与显式队列转发。
4. `MainLibraryViewModel` 通过 `MediaLibraryRepository` + use case 读取权限状态、本地媒体库、最近播放和缩略图预热，不再直接访问 MediaStore helper。
5. `PlaybackSessionHost` 通过注入的 `PlaybackControllerConnector` 建连 `MediaController`，`PlaybackLaunchOrchestrator` 负责启动意图、seek fallback 和 runtime policy 编排，再把 Media3 状态翻译成 `PlaybackScreenModel` / `PlaybackScreenDependencies` 给 `PlayerScreen`。
6. `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 负责队列、续播位置、倍速和轨道恢复；`PlaybackStateWriter` / `QueueHistoryWriter` 现在位于 `player-platform`，由 engine service 消费。
7. `AsukaAppGraph` 内部已经拆成 `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 两个 runtime feature installer；app 侧则通过 `MainLibraryFeatureInstaller` / `PlaybackFeatureEntryPointFactory` 组装具体入口依赖。
8. settings 现在默认走 `DataStoreAppSettingsStore`；playback state / queue history 现在默认走 Room-backed store，并在首次启动时从 legacy `SharedPreferences*Store` 导入。

## 当前代码组织

- 组合根与装配已经进一步瘦身：
  - `AsuraPlayerApp` 只持有 `graph` 和 `appComposition`
  - `AppCompositionFactory` 负责把 app 层入口依赖组装成 `AppComposition`
  - `MainLibraryFeatureInstaller` 负责媒体库 feature 的 repository / use case / view model factory 装配
  - `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 负责 runtime graph 内部 feature 构造
- 媒体库与设置页已经拆成 feature-oriented 文件：
  - `MainLibraryScreen` 负责状态汇总与 launcher
  - `MainLibraryNavHost` 负责导航装配
  - `MainLibraryUiState` 负责 library feature 入口状态聚合
  - `MediaLibraryDataSources` / `MediaLibraryRepository` / `MainLibraryViewModel` 形成 data source -> repository -> use case -> view model 的媒体库链路
  - `LibraryHomePage` / `LibraryVideoPages` / `LibraryRecentPage` / `SettingsPageContent` / `PlayerSettingsPageContent` / `ThemeSettingsScreen` / `MotionSettingsPageContent` 负责具体页面内容
- 主题与共享 UI 已按职责拆分：
  - `AsukaTheme` / `ThemeColorUtils` / `ThemeSwatchComponents` / `CustomThemeEditorSheet`
  - `UiComponentTokens` / `GroupedSurfaceComponents` / `SettingsNavigationRows` / `SettingsSliderRows` / `SettingsToggleRows` / `SettingsSelectionRows`
- 播放器和主题设置页内部也继续拆分：
  - `PlayerSettingsPageContent` 现在只做页面状态装配，section 在 `PlayerSettingsSections`，弹窗在 `PlayerSettingsDialogs`
  - `ThemeSettingsScreen` 现在只保留主题页入口逻辑，外观/颜色/显示 section 在 `ThemeSettingsSections`
- 播放进度刷新不再是 attach 后常驻轮询，而是只在 `player.isPlaying` 时启动短周期 ticker

## 当前边界状态

- `player-contract` 只暴露纯业务 API，不再直接暴露 `Parcelable`、`Uri`、`MediaItem`、`Player`、`ComponentName`、`Window` 等平台类型。
- `player-platform` 承接 Android / Media3 binding，包括 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`、Intent/URI helper、track reader、Media3 queue mapper、writer。
- `player-ui` 不再直接依赖 `player-engine` 实现类；controller 建连通过 platform 层 connector 接口完成。
- 代码包前缀已经分离为 `com.asuka.player.app` / `runtime` / `contract` / `platform` / `engine` / `ui`，减少跨模块“同包伪同层”。

## 当前持久化状态

- settings 使用 `AppSettingsSnapshot` 作为单一 schema，默认存储实现为 DataStore。
- playback state / queue history 使用 Room；Room schema 已导出到 `player-data/schemas/`。
- legacy `SharedPreferencesAppSettingsStore`、`SharedPreferencesPlaybackStore`、`SharedPreferencesQueueHistoryStore` 保留为迁移源和兼容测试目标，不再是默认运行路径。
- migration 和 schema compatibility 已有自动化覆盖，新增字段优先改 snapshot / entity / migration，而不是到处补 key 与默认值。

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

# Asuka Player

一款基于 Jetpack Compose + Media3/ExoPlayer 的 Android 本地视频播放器。

## 当前能力

- 本地媒体库浏览、文件夹分组、最近播放
- 本地索引媒体库：分页读取、增量同步、自动感知媒体变更
- 外部 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` / `ClipData` 多文件启动
- 手势控制：横向 seek、亮度、音量、双击、长按倍速、缩放、平移
- 音轨 / 字幕切换、比例切换、倍速切换
- PiP、后台播放、播放恢复
- 播放状态持久化：位置、速度、音轨、字幕、缩放、最近历史
- 播放持久化回写已改为异步串行队列，回调线程不直接阻塞写盘

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
player-platform/  Android / Media3 binding API、Intent/seek fallback、异步 writer、窄依赖接口
player-render-api/ 播放 surface / renderer 中立契约
player-renderer/  PlaybackActivity、session assembly、PIP、Media3 surface/render adapter
player-runtime/   设置仓库、运行时 graph、启动编排、设备/持久化实现装配
player-ui/        纯播放 UI、手势编排、UI 状态与动作翻译，不直接依赖 Media3 / Activity
player-engine/    PlaybackService、Media3 controller/connector 实现
player-domain/    纯 JVM 算法与状态机
player-data/      DataStore/Room 持久化实现、媒体库索引库、legacy SharedPreferences 迁移源、schema/compat 测试
```

依赖方向：`app` → `player-runtime` / `player-platform` / `player-renderer` / `player-data`；`player-renderer` → `player-render-api` / `player-ui` / `player-platform` / `player-contract`；`player-ui` → `player-render-api` / `player-contract` / `player-platform` / `player-domain`；`player-runtime` → `player-contract` / `player-platform` / `player-engine` / `player-data`；`player-engine` → `player-contract` / `player-platform`；`player-data` → `player-contract`

## 关键架构

1. `AsuraPlayerApp` 现在只做 composition root：构建 `AsukaAppGraph`，再委托 `AppCompositionFactory` 产出 `MainActivityDependencies`、platform 层的 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`。
2. `AsuraPlayerApp` 持有窄依赖 container，`MainActivity` / `PlaybackActivity` / `PlaybackService` 在各自入口内从 `Application` 读取依赖，不再依赖静态 registry。
3. `MainActivity` 和 `PlaybackLaunchCoordinator` 负责解析 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` 启动 URI、seek fallback 与显式队列转发。
4. 媒体库现在先同步到本地索引库，再由 `MediaLibraryRepository` + use case 提供分页 folders/videos/recent lookup；`ContentObserver` 负责增量触发同步。
5. `player-renderer` 持有 `PlaybackActivity` / `PlaybackActivitySession` / `PlaybackSessionHost`，负责 `MediaController` 建连、seek fallback、PiP、window side effects 和 surface render adapter，再把窄 UI 模型交给 `player-ui/PlayerScreen`。
6. `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 负责队列、续播位置、倍速和轨道恢复；`PlaybackStateWriter` / `QueueHistoryWriter` 现在位于 `player-platform`，由 engine service 消费。
7. `AsukaAppGraph` 内部已经拆成 `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 两个 runtime feature installer；app 侧则通过 `MainLibraryFeatureInstaller` / `PlaybackFeatureEntryPointFactory` 组装具体入口依赖。
8. settings 默认走 `DataStoreAppSettingsStore`；playback state / queue history 默认走 Room-backed store；媒体库元数据默认走本地 Room 索引，并在应用运行中持续增量同步。

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
- `player-platform` 承接 Android / Media3 binding API，包括 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`、Intent/URI helper、track reader、Media3 queue mapper、异步持久化 writer。
- `player-render-api` 只保留 surface/render 契约，不携带 Media3、Activity 或 app 层实现依赖。
- `player-renderer` 负责播放入口、session assembly、PIP、surface render adapter，不直接依赖 app 层。
- `player-ui` 不再直接依赖 `player-engine`、`Media3` 或 `androidx.activity`；controller 建连通过 platform 层 connector 接口完成，surface render 通过 render-api 契约完成。
- 代码包前缀已经分离为 `com.asuka.player.app` / `runtime` / `contract` / `platform` / `engine` / `ui`，减少跨模块“同包伪同层”。

## 当前持久化状态

- settings 使用 `AppSettingsSnapshot` 作为单一 schema，默认存储实现为 DataStore。
- playback state / queue history 使用 Room；Room schema 已导出到 `player-data/schemas/`。
- 媒体库列表使用本地 Room 索引库，应用通过增量同步维护索引，再从索引做分页查询。
- legacy `SharedPreferencesAppSettingsStore`、`SharedPreferencesPlaybackStore`、`SharedPreferencesQueueHistoryStore` 保留为迁移源和兼容测试目标，不再是默认运行路径。
- migration 和 schema compatibility 已有自动化覆盖，新增字段优先改 snapshot / entity / migration，而不是到处补 key 与默认值。
- playback/history 回写已经走串行异步任务队列；service 销毁时才显式 flush + await，避免播放器事件线程直接阻塞写盘。

## 本地验证

```bash
# Kotlin 编译
./gradlew :app:compileDebugKotlin

# 全量 JVM 单元测试
./gradlew test

# 配置缓存健康检查
./gradlew help

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
- `./gradlew help` 应显示 `Configuration cache entry reused.`

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [测试说明](docs/TESTING.md)
- [路线与待办](docs/ROADMAP.md)
- [变更记录](docs/CHANGELOG.md)

# Asuka Player

一款基于 Jetpack Compose + Media3/ExoPlayer 的 Android 本地视频播放器。

## 当前能力

- 本地媒体库浏览、文件夹分组、最近播放
- 本地索引媒体库：分页读取、增量同步、自动感知媒体变更
- 最近播放会区分可回放 URI 与不可解析来源；不可解析来源仅展示为不可用项，不再伪装成可回放视频
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
player-contract/  纯 Kotlin 播放契约、队列/会话规划、设置与持久化接口、UI/renderer 依赖的播放 port
player-platform/  Android / Media3 binding API、Intent/seek fallback、异步 writer、窄依赖接口与适配器
player-render-api/ 播放 surface / renderer 中立契约
player-renderer/  PlaybackActivity、session assembly、PIP、Media3 surface/render adapter
player-runtime/   设置仓库、运行时 graph、启动编排、设备/持久化实现装配
player-ui/        纯播放 UI、手势编排、UI 状态与动作翻译，不直接依赖 Media3 / Activity
player-engine/    PlaybackService、Media3 controller/connector 实现
player-domain/    纯 JVM 算法与状态机
player-data/      DataStore/Room 持久化实现、媒体库索引库、legacy SharedPreferences 迁移源、schema/compat 测试
```

依赖方向：`app` → `player-runtime` / `player-platform` / `player-renderer` / `player-data` / `player-engine`；`player-renderer` → `player-render-api` / `player-ui` / `player-platform` / `player-contract`；`player-ui` → `player-render-api` / `player-contract` / `player-domain`；`player-runtime` → `player-contract` / `player-platform` / `player-data`；`player-engine` → `player-contract` / `player-platform`；`player-data` → `player-contract`

## 关键架构

1. `AsuraPlayerApp` 是唯一组合根：构建 `AsukaAppGraph`（注入 engine 绑定），再委托 `AppComposition` 使用内联匿名对象产出 `MainActivityDependencies`、platform 层的 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`。
2. `MainActivity` / `PlaybackActivity` / `PlaybackService` 通过 `Provider.from(application)` 集中式查找读取窄依赖，架构边界检查在构建期验证 Application 实现了所需 provider 接口。
3. `IncomingPlaybackIntentReader` + `PlaybackSessionRequestCodec` 负责把 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE` / `ClipData` 归一成单一 `PlaybackSessionRequest`；`PlaybackLaunchCoordinator` 只负责把当前项解析为实际 playback URI 并生成启动 intent。
4. 媒体库现在先同步到本地索引库，再由 `MediaLibraryRepository` + use case 提供分页 folders/videos/recent lookup；`ContentObserver` 负责增量触发同步。
5. `player-renderer` 持有 `PlaybackActivity` / `PlaybackViewModel` / `PlaybackSessionHost`，其中 `PlaybackViewModel` 作为 `AndroidViewModel` 持有 session host 和 host 状态，跨 configuration change 存活；host 已拆成 controller connection、launch driver、state feeds 三个协作者；`PlaybackHostState`（低频）和 `PlayerUiState`（20Hz 进度）分离为独立 `StateFlow`，`PlayerScreen` 内部收集高频流以避免 Activity 级重组。
6. `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 负责队列、续播位置、倍速和轨道恢复；`PlaybackStateWriter` / `QueueHistoryWriter` 现在位于 `player-platform`，由 engine service 消费；`PlaybackController.release()` 负责清理 listener。
7. `AsukaAppGraph` 内部已经拆成 `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 两个 runtime feature installer；engine 具体实现（`Media3PlaybackControllerConnectorFactory`、`PlaybackService` 组件名、通知图标）通过 `AsukaAppGraph` 构造器注入，`player-runtime` 不编译时依赖 `player-engine`。
8. settings 默认走 `DataStoreAppSettingsStore`；playback state / queue history 默认走 Room-backed store；媒体库元数据默认走本地 Room 索引，并在应用运行中持续增量同步。

## 当前代码组织

- 组合根与装配已经进一步瘦身：
  - `AsuraPlayerApp` 只持有 `graph` 和 `appComposition`
  - `AppComposition` 使用内联匿名对象将 graph 映射为窄依赖接口，不再通过单独的映射类
  - `MainLibraryFeatureInstaller` 负责媒体库 feature 的 repository / use case / view model factory 装配
  - `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 负责 runtime graph 内部 feature 构造
- 媒体库与设置页已经拆成 feature-oriented 文件：
  - `MainLibraryScreen` 负责状态汇总与 launcher
  - `MainLibraryNavHost` 负责导航装配
  - `MainLibraryUiState` 负责 library feature 入口状态聚合
  - `MediaLibraryDataSources` / `MediaLibraryRepository` / `MainLibraryViewModel` 形成 data source -> repository -> use case -> view model 的媒体库链路
  - `MainLibraryCatalogStore` 已退化成 facade，内部状态机拆成 folders / all videos / current folder / recent 四个 slice
  - `LibraryHomePage` / `LibraryVideoPages` / `LibraryRecentPage` / `SettingsPageContent` / `PlayerSettingsPageContent` / `ThemeSettingsScreen` / `MotionSettingsPageContent` 负责具体页面内容
- 主题与共享 UI 已按职责拆分：
  - `AsukaTheme` / `ThemeColorUtils` / `ThemeSwatchComponents` / `CustomThemeEditorSheet`
  - `UiComponentTokens` / `GroupedSurfaceComponents` / `SettingsNavigationRows` / `SettingsSliderRows` / `SettingsToggleRows` / `SettingsSelectionRows`
- 播放器和主题设置页内部也继续拆分：
  - `PlayerSettingsPageContent` 现在只做页面状态装配，section 在 `PlayerSettingsSections`，弹窗在 `PlayerSettingsDialogs`
  - `ThemeSettingsScreen` 现在只保留主题页入口逻辑，外观/颜色/显示 section 在 `ThemeSettingsSections`
  - `PlayerScreen` 现在只保留状态初始化与 shell 装配；副作用收敛在 `PlayerScreenEffects`，渲染树拆到 `PlayerScreenShells`
  - overlay 面板已经拆成 settings / tracks / speed 三组独立 section 文件
- 播放进度刷新不再是 attach 后常驻轮询，而是只在 `player.isPlaying` 时启动短周期 ticker

## 当前边界状态

- `player-contract` 只暴露纯业务 API，不再直接暴露 `Parcelable`、`Uri`、`MediaItem`、`Player`、`ComponentName`、`Window` 等平台类型。
- `player-platform` 承接 Android / Media3 binding API，包括 `PlaybackActivityDependencies` / `PlaybackServiceDependencies`、Intent/URI helper、track reader、Media3 queue mapper、异步持久化 writer 和 renderer 侧 connector 适配。
- `player-render-api` 只保留 surface/render 契约，不携带 Media3、Activity 或 app 层实现依赖。
- `player-renderer` 负责播放入口、session assembly、PIP、surface render adapter，不直接依赖 app 层。
- `player-ui` 不再直接依赖 `player-engine`、`Media3`、`androidx.activity` 或 `player-platform`；播放控制、轨道选择等 port 已收敛到 `player-contract`，surface render 通过 `player-render-api` 契约完成。
- 代码包前缀已经分离为 `com.asuka.player.app` / `runtime` / `contract` / `platform` / `engine` / `ui`，减少跨模块“同包伪同层”。

## 当前持久化状态

- settings 使用 `AppSettingsSnapshot` 作为单一 schema，默认存储实现为 DataStore。
- playback state / queue history 使用 Room；Room schema 已导出到 `player-data/schemas/`。
- 媒体库列表使用本地 Room 索引库，应用通过增量同步维护索引，再从索引做分页查询。
- legacy `SharedPreferencesAppSettingsStore`、`SharedPreferencesPlaybackStore`、`SharedPreferencesQueueHistoryStore` 保留为迁移源和兼容测试目标，不再是默认运行路径。
- migration 和 schema compatibility 已有自动化覆盖，新增字段优先改 snapshot / entity / migration，而不是到处补 key 与默认值。
- playback/history 回写已经走串行异步任务队列；service 销毁时触发非阻塞 flush + close（队列消费者在自有 IO scope 上处理剩余项），不阻塞主线程。

## 本地验证

```bash
# Kotlin 编译
./gradlew :app:compileDebugKotlin

# 全量 JVM 单元测试
./gradlew test

# 架构与体积治理
./gradlew verifyArchitectureBoundaries verifySourceFileSizes

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
- `./gradlew verifyArchitectureBoundaries verifySourceFileSizes`
- `./gradlew help` 应显示 `Configuration cache entry reused.`

## 文档

- [架构说明](docs/ARCHITECTURE.md)
- [测试说明](docs/TESTING.md)
- [路线与待办](docs/ROADMAP.md)
- [变更记录](docs/CHANGELOG.md)

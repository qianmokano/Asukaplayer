# 架构说明

## 设计原则

- 单一组合根：`AsuraPlayerApp` 是唯一装配入口，负责 graph、entry point 和应用级依赖 container 初始化。
- 组合根只做装配：`AsuraPlayerApp` 不直接 new feature 级 data source / repository / use case / `ViewModelFactory`，这些责任交给 installer / factory。
- framework 入口显式注入：`MainActivity`、`PlaybackActivity`、`PlaybackService` 通过 `AsuraPlayerApp` 提供的窄依赖 container 读取依赖，不再通过静态 registry 注入。
- 组合根输出窄 binding：`app` 层 installer / entry point 只接收 feature 级 binding，不再继续向下传整张 `AsukaAppGraph`。
- 纯 contract + 平台 binding 分层：`player-contract` 只保留纯 Kotlin API；Android / Media3 入口依赖统一放进 `player-platform`。
- 单一设置真相源：播放运行时设置统一来自 `PlaybackRuntimeSettingsSource`。
- 持久化语义显式异步：settings / playback / history 的 I/O contract 使用 `suspend` API 表达完成语义，调用返回即表示写入已完成或抛错。
- UI 依赖 UI 模型：播放页消费 `PlaybackScreenModel` / `PlaybackScreenDependencies`，不直接拼装 Media3 细节。
- 策略与落盘分离：规划、执行、持久化分别由独立对象负责，减少隐式耦合。
- 单一播放 payload：启动链路使用统一的 `PlaybackIntentPayload` / codec 表达队列、稳定 mediaId 与起播位置，避免多处重复解析 intent。
- feature 分层优先：媒体库采用 data source -> repository -> use case -> view model，theme/settings model 保持纯值对象，不携带 Compose 类型。

## 模块边界

### `app`
- 应用入口与组合根
- `AsuraPlayerApp` 依赖 container
- `AppCompositionFactory` / `AppComposition`
- `MainLibraryFeatureInstaller`
- `PlaybackFeatureEntryPointFactory`
- 媒体库、设置页、导航
- `MediaLibraryDataSources` / `MediaLibraryRepository` / use cases
- UI 层按 feature slice 组织页面壳子与页面内容

### `player-contract`
- 纯 Kotlin 稳定契约与模型
- `PlayerSettings` / `PlaybackRuntimeSettings`
- `PlaybackController` / `PlaybackStore` / `QueueHistoryStore`
- `PlaybackSessionPlanner`、`PlaybackStateRepository`、`QueueHistoryRepository`
- `PlaybackQueue` / `PlaybackQueueItem` / `PlaybackQueueEntry`
- 不直接依赖 Android / Media3 类型

### `player-platform`
- Android binding 层
- `PlaybackActivityDependencies` / `PlaybackServiceDependencies` / `PlaybackDependenciesProvider`
- `PlaybackControllerConnector` / `PlaybackDeviceControllerFactory`
- `IntentQueueReader` / `IntentUriRemapper` / `SeekFallbackCopier`
- `TrackInfoReader` / `TrackSelectionFacade` / `TrackSelectionStateReader`
- `PlaybackStateWriter` / `QueueHistoryWriter` / `PlaybackQueue.toMediaItems()`

### `player-runtime`
- `AsukaAppGraph`
- `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller`
- 设置仓库与 `PlaybackRuntimeSettingsSource`
- 播放启动编排：`PlaybackLaunchCoordinator`
- `PlaybackUiPersistence`、`PlaybackDeviceControllerFactory` 的运行实现
- `Media3PlaybackControllerConnectorFactory` 等 engine 实现的装配入口
- 不再依赖 Compose UI 类型，theme/settings model 使用纯 ARGB / 基础值类型

### `player-ui`
- `PlaybackActivity`、`PlaybackSessionHost`
- `PlaybackLaunchOrchestrator`
- `PlaybackWindowChromeController` / `PlaybackPictureInPictureController`
- `PlayerScreen` 与播放 UI 组件
- 手势编排、UI 状态、Media3 -> UI 的翻译层
- 只依赖 `player-contract` / `player-platform`，不直接 import engine 实现

### `player-engine`
- `PlaybackService`
- `Media3PlaybackController` / `Media3PlaybackControllerConnector`
- Media3 controller/service 实现

### `player-domain`
- 手势算法与状态机
- 纯 JVM，可直接单元测试

### `player-data`
- `PlaybackStore`、`QueueHistoryStore`、`AppSettingsStore`
- `DataStoreAppSettingsStore`
- `AsukaPlaybackRoomDatabase` + DAO + Room-backed store
- legacy SharedPreferences migration source
- schema / compatibility tests

## 组合根与依赖装配

当前唯一的运行时依赖装配路径是：

1. `AsuraPlayerApp` 创建 `AsukaAppGraph`
2. `AsukaAppGraph` 通过 `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 组装 runtime feature
3. `AppCompositionFactory` 调用 `MainLibraryFeatureInstaller` / `PlaybackFeatureEntryPointFactory` 生成 app 侧入口依赖
4. `AsuraPlayerApp` 暴露 `MainActivityDependencies` / `PlaybackActivityDependencies` / `PlaybackServiceDependencies`
5. `MainActivity` / `PlaybackActivity` / `PlaybackService` 在各自入口内从 `Application` 读取这些窄依赖

这里的关键变化是：

- `AppCompositionFactory` 现在先把 graph 拆成 `MainLibraryFeatureBindings` / `PlaybackActivityEntryBindings` / `PlaybackServiceEntryBindings`
- feature installer 与 entry dependency wrapper 只知道自己收到的 binding，而不是继续持有整张 graph
- 运行时依赖是否延迟初始化，成为组合根内部实现细节，而不是 feature 代码可见的能力

播放入口目前只暴露两个窄依赖：

- `PlaybackActivityDependencies`
- `PlaybackServiceDependencies`

这意味着：

- `MainActivity`、`PlaybackActivity`、`PlaybackService` 不会直接拿到整张 `AsukaAppGraph`
- `player-contract` 只知道纯业务模型
- `player-ui` 只知道 platform 层 connector / dependency 接口，而不知道 engine 里的具体实现
- 新增 feature 优先扩展 installer / factory，而不是继续膨胀 `Application`

## 播放链路

### 1. 启动阶段

- `MainActivity` 接收媒体选择或外部 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE`
- `IncomingPlaybackIntentReader` 用 `PlaybackIntentPayloadCodec.fromExternalIntent()` 把外部 intent 归一成单一 payload
- `PlaybackLaunchCoordinator` 负责：
  - 对 payload 当前项做 URI 解析与 seek fallback
  - 保留 stable mediaId / queue / startIndex 语义
  - 生成用于启动 `PlaybackActivity` 的 intent
- `IntentQueueReader` / `PlaybackSessionCoordinator` 在播放页只读取同一份 payload，不再重新发明队列规则

说明：

- 播放运行时设置不再通过 intent snapshot 传递
- `PlaybackActivity` 在实例化时就拿到 `PlaybackActivityDependencies`

### 2. 会话连接阶段

- `PlaybackActivity` 持有 `PlaybackSessionHost`
- `PlaybackActivity` 将窗口副作用委托给 `PlaybackWindowChromeController` / `PlaybackPictureInPictureController`
- `PlaybackSessionHost` 通过注入的 `PlaybackControllerConnector` 建立 `MediaController`
- `PlaybackLaunchOrchestrator` 负责当前 launch intent、seek fallback 与 runtime policy 编排
- `PlaybackSessionHost` 维护两类状态：
  - `PlayerUiStateHolder`：播放中的标题、时长、进度、错误、buffering
  - `PlaybackTrackUiStateHolder`：音轨、字幕、当前倍速、当前媒体 ID、选中项

### 3. 执行阶段

- `PlaybackSessionCoordinator` 把启动 intent 与 `PlaybackSessionPlanner` 输出的 `PlaybackSessionPlan` 应用到控制器
- 计划内容包括：
  - 显式队列
  - 续播位置
  - 倍速恢复
  - 轨道恢复请求
- `PlaybackSessionPlan.queue` 现在是纯 queue model，转成 `MediaItem` 的工作放在 platform 层 mapper

### 4. UI 阶段

`PlayerScreen` 只消费两类输入：

- `PlaybackScreenModel`
- `PlaybackScreenDependencies`

它不再直接构建：

- `TrackSelectionFacade`
- `TrackUiStateHolder`
- `SelectionState`

Media3 到 UI 的翻译已经前置到 host 层完成。

### 5. App UI 组织

- `MainLibraryScreen` 只负责：
  - 读取 `ViewModel` 状态
  - 处理 document picker / permission launcher / toast / 对话框入口
  - 把聚合后的状态传给 `MainLibraryNavHost`
- `MainLibraryUiState` 负责 library feature 入口状态聚合，不再把这部分 remember 逻辑塞进导航装配文件
- `MainLibraryViewModel` 只负责状态流和 UI 事件，不再直接查询 MediaStore
- `AndroidVideoAccessDataSource` / `AndroidMediaStoreVideoCatalogDataSource` / `PlaybackRecentMediaDataSource` 提供媒体库底层数据
- `ResolveVideoAccessUseCase` / `RefreshMediaLibraryUseCase` / `LoadRecentMediaIdsUseCase` 负责 feature 规则
- `MainLibraryNavHost` 负责导航和页面装配
- `LibraryChrome` / `LibraryHomePage` / `LibraryVideoPages` / `LibraryRecentPage` / `SettingsPageContent` / `PlayerSettingsPageContent` / `MotionSettingsPageContent` / `ThemeSettingsScreen` 负责页面级 UI
- settings 控件已拆成 `SettingsSliderRows` / `SettingsToggleRows` / `SettingsSelectionRows`
- 播放器设置页已拆成 `PlayerSettingsPageContent` + `PlayerSettingsSections` + `PlayerSettingsDialogs`
- 主题设置页已拆成 `ThemeSettingsScreen` + `ThemeSettingsSections`
- 主题与共享 UI 已拆成职责文件，而不是继续堆在单个 600+ 行文件里

这样做的目的：

- 降低媒体库与设置页入口文件的冲突率
- 让导航壳子与页面内容可独立修改
- 避免在单个超大文件里同时处理 launcher、导航、页面布局和业务交互
- 让大页面文件被拆成 section / dialog / page content 三层，便于体积阈值持续生效

## 运行时设置

当前播放运行时设置的唯一真相源是 `AppPlaybackRuntimeSettingsSource`。

它由两部分合成：

- `PlayerSettingsRepository`
- `PlaybackBehaviorRepository`

关键结果：

- 播放页启动时读取的是当前值，而不是旧的 intent 快照
- `hideButtonsBackground` 等默认值与 `PlayerSettings` 保持一致
- 设置变化通过 flow 持续同步到播放页
- 亮度记忆、缩放/轨道/倍速持久化、音量/亮度控制都不再由 `PlaybackActivity` 私自持有

## 持久化与恢复

### settings

- `AppSettingsSnapshot` 是 settings 的单一 schema，包含：
  - `UiSettingsRecord`
  - `PlayerSettingsRecord`
  - `PlaybackBehaviorRecord`
- 默认实现是 `DataStoreAppSettingsStore`
- `SharedPreferencesAppSettingsStore` 退化为 legacy migration source / compatibility store
- `DataStoreAppSettingsStore.saveSnapshot()` 只有在 `DataStore.updateData()` 完成后才会发布新 snapshot
- settings repository 对外暴露的是显式 `suspend` 写接口；是否等待完成由调用方决定，而不是 repository 内部偷偷 fire-and-forget
- snapshot 的默认值、归一化裁剪和 JSON codec 集中在 `player-data/AppSettingsStore.kt`
- 新增 settings 字段的改动面应收敛到：
  - snapshot / record
  - codec
  - migration / compatibility tests

### playback state / history

- `AsukaPlaybackRoomDatabase` 持有两张表：
  - `playback_state`
  - `queue_history`
- `RoomPlaybackStore` 负责按 `mediaId` 读写位置、速度、轨道、缩放，并维护最近使用顺序
- `RoomQueueHistoryStore` 负责顺序历史和容量裁剪
- `PlaybackPersistenceStoresFactory` 是 `suspend` factory，负责在后台打开 Room，并在首次初始化时从 legacy SharedPreferences 导入 playback/history 数据
- `PlaybackRuntimeFeature` 通过 deferred store wrapper + suspend resolver 延迟解析 persistence store，避免把首次建库工作泄漏到同步 getter 语义里
- SharedPreferences playback/history store 现在主要用于 legacy import 和回归测试

### 播放状态

- `PlaybackStateWriter` 写回：
  - 播放位置
  - 倍速
  - 音轨 / 字幕稳定 ID
- `PlaybackStateWriter` 与 `QueueHistoryWriter` 已从纯 contract 移到 `player-platform`
- writer 写入的底层 store 默认已经是 Room-backed store，而不是 SharedPreferences
- `OverlayActions` / `OverlayTrackActions` 不再直接落盘 speed/track，UI 只发命令
- 写回策略：
  - seek / pause / ended 等事件驱动
  - 播放中低频 checkpoint
  - service 销毁前 flush

### UI 进度刷新

- `PlayerUiStateHolder` 的 position/duration 刷新采用“事件驱动 + 播放中短轮询”
- attach 后不会无条件常驻 ticker
- 只有在 `player.isPlaying` 时才启动短周期刷新，暂停后立即停止
- 标题、错误、buffering、媒体切换等状态仍然主要依赖 `Player.Listener` 事件

### 最近历史

- `QueueHistoryStore` 现在要求实现必须可跨线程安全调用
- 默认实现是 `RoomQueueHistoryStore`
- `SharedPreferencesQueueHistoryStore` 与 `InMemoryQueueHistoryStore` 仍保留，用于迁移导入和测试

### 迁移与 schema

- settings:
  - `SharedPreferencesAppSettingsStore` -> `DataStoreAppSettingsStore`
  - migration 在首次读取 DataStore 时触发
- playback/history:
  - `SharedPreferencesPlaybackStore` / `SharedPreferencesQueueHistoryStore` -> Room
  - 导入由 `PlaybackPersistenceStoresFactory` 打开数据库时执行
- Room schema 导出目录：
  - `player-data/schemas/com.asuka.player.data.AsukaPlaybackRoomDatabase/`
- 持久化相关变更必须补：
  - migration test
  - schema compatibility test
  - store behavior test

## 当前推荐阅读顺序

1. `README.md`
2. `player-runtime/src/main/java/com/asuka/player/app/AppGraph.kt`
3. `app/src/main/java/com/asuka/player/app/AsuraPlayerApp.kt`
4. `player-runtime/src/main/java/com/asuka/player/app/SettingsRuntimeInstaller.kt`
5. `player-runtime/src/main/java/com/asuka/player/app/PlaybackRuntimeInstaller.kt`
6. `app/src/main/java/com/asuka/player/app/AppComposition.kt`
7. `app/src/main/java/com/asuka/player/app/MainLibraryFeatureInstaller.kt`
8. `player-platform/src/main/java/com/asuka/player/platform/PlaybackAndroidBindings.kt`
9. `player-runtime/src/main/java/com/asuka/player/app/PlaybackLaunchCoordinator.kt`
10. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackSessionHost.kt`
11. `player-engine/src/main/java/com/asuka/player/core/impl/Media3PlaybackController.kt`
12. `player-engine/src/main/java/com/asuka/player/core/service/PlaybackService.kt`
13. `player-data/src/main/java/com/asuka/player/data/AppSettingsStore.kt`
14. `player-data/src/main/java/com/asuka/player/data/DataStoreAppSettingsStore.kt`
15. `player-data/src/main/java/com/asuka/player/data/AsukaPlaybackRoomDatabase.kt`

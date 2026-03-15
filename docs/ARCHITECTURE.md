# 架构说明

## 设计原则

- 单一组合根：`AsuraPlayerApp` 是唯一装配入口，负责 graph、entry point 和应用级依赖 container 初始化。
- 组合根只做装配：`AsuraPlayerApp` 不直接 new feature 级 data source / repository / use case / `ViewModelFactory`，这些责任交给 installer / factory。
- framework 入口显式注入：`MainActivity`、`PlaybackActivity`、`PlaybackService` 通过 `PlaybackDependenciesProvider.from(application)` / `MainActivityDependenciesProvider.from(application)` 读取窄依赖，集中式查找带诊断错误信息，架构边界检查在构建期验证 Application 实现了所需 provider 接口。
- 组合根输出窄 binding：`app` 层 installer / entry point 只接收 feature 级 binding，不再继续向下传整张 `AsukaAppGraph`。
- 纯 contract + 平台 binding 分层：`player-contract` 只保留纯 Kotlin API；Android / Media3 入口依赖统一放进 `player-platform`。
- render contract 与 render implementation 分层：surface / renderer 契约放在 `player-render-api`；Media3 / Activity / PiP 适配实现放在 `player-renderer`。
- 单一设置真相源：播放运行时设置统一来自 `PlaybackRuntimeSettingsSource`。
- 持久化语义显式异步：settings / playback / history 的 I/O contract 使用 `suspend` API 表达完成语义，调用返回即表示写入已完成或抛错。
- 播放回调不直接写盘：播放状态与历史回写通过串行异步队列排队，service 销毁时只触发非阻塞 flush + close（队列消费者在自有 IO scope 上处理剩余项），不阻塞主线程。
- UI 依赖 UI 模型：播放页消费 `PlaybackScreenModel` / `PlaybackScreenDependencies`，不直接拼装 Media3 细节。`PlayerUiState`（高频进度/标题/错误）通过独立 `StateFlow` 传入 `PlayerScreen`，与低频 host 状态分离。
- 策略与落盘分离：规划、执行、持久化分别由独立对象负责，减少隐式耦合。
- 单一播放 request：启动链路使用统一的 `PlaybackSessionRequest` / codec 表达原始队列、稳定 mediaId、当前 playback URI 与 request 身份，避免多处重复解析 intent。
- feature 分层优先：媒体库采用 data source -> repository -> use case -> view model，theme/settings model 保持纯值对象，不携带 Compose 类型。
- 媒体库先索引后查询：MediaStore 变更先同步到本地索引库，再由 app 层从索引执行分页读取；直接扫描 MediaStore 不再是页面读路径。
- 旧异步结果不得覆盖新状态：folder 分页、播放启动、seek fallback 这类异步链路都必须带当前 request 身份；过期结果只能丢弃，不能回写。
- 运行时设置先发布最佳可用快照：settings repository 冷启动先暴露当前内存快照，再由真实 store 异步接管，避免主线程等待磁盘初始化。

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
- `PlayerSettings`
- `PlaybackController` / `PlaybackTrackSelectionController` / `PlaybackStore` / `QueueHistoryStore`
- `PlaybackSessionRequest` / `PlaybackSessionPlanner` / `PlaybackStateRepository` / `QueueHistoryRepository`
- `PlaybackQueue` / `PlaybackQueueItem` / `PlaybackQueueEntry`
- 不直接依赖 Android / Media3 类型

### `player-platform`
- Android binding 层
- `PlaybackActivityDependencies` / `PlaybackServiceDependencies` / `PlaybackDependenciesProvider`
- `PlaybackControllerConnector` / `PlaybackDeviceControllerFactory`
- `PlaybackSessionRequestCodec` / `IntentQueueReader` / `IntentUriRemapper` / `SeekFallbackCopier`
- `TrackInfoReader` / `TrackSelectionFacade` / `TrackSelectionStateReader`
- `PlaybackStateWriter` / `QueueHistoryWriter` / `SerialTaskQueue` / `PlaybackQueue.toMediaItems()`

### `player-render-api`
- 播放 surface / renderer 中立契约
- `PlaybackSurfaceState`
- `PlaybackSurfaceTransform`
- `PlaybackSurfaceRenderer`
- 不直接依赖 Media3 / Activity / app 层类型

### `player-renderer`
- 播放入口与 render implementation
- `PlaybackActivity`
- `PlaybackViewModel`
- `PlaybackSessionHost`
- `PlaybackControllerConnection` / `PlaybackSessionLaunchDriver` / `PlaybackSessionStateFeeds`
- `PlaybackLaunchOrchestrator`
- `PlaybackPictureInPictureController` / `PlaybackWindowChromeController`
- `Media3PlaybackSurfaceRenderer`
- `PlayerUiStateHolder` / `PlaybackTrackUiStateHolder` / `PlaybackSessionCoordinator`
- 依赖 `player-render-api` + `player-ui` + `player-platform`

### `player-runtime`
- `AsukaAppGraph`
- `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller`
- 设置仓库与 `PlaybackRuntimeSettingsSource`
- 播放启动编排：`PlaybackLaunchCoordinator`
- `PlaybackUiPersistence`、`PlaybackDeviceControllerFactory` 的运行实现
- engine 具体实现（`Media3PlaybackControllerConnectorFactory`、`PlaybackService` 组件名、通知图标）通过构造器注入，由 `app` 层提供
- 不依赖 `player-engine`、Compose UI 类型；theme/settings model 使用纯 ARGB / 基础值类型

### `player-ui`
- `PlayerScreen` 与播放 UI 组件
- `PlayerScreenEffects` / `PlayerScreenShells`
- 手势编排、UI 状态、UI 动作翻译层
- 通过 `player-render-api` 消费 surface render 契约
- 通过 `player-contract` 消费 playback / track-selection port
- 不直接 import `Media3` / `androidx.activity` / `player-platform` / engine 实现

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
- `AsukaMediaLibraryIndexDatabase` + DAO + 本地媒体库索引
- legacy SharedPreferences migration source
- schema / compatibility tests

## 组合根与依赖装配

当前唯一的运行时依赖装配路径是：

1. `AsuraPlayerApp` 创建 `AsukaAppGraph`，注入 engine 绑定（`Media3PlaybackControllerConnectorFactory`、`PlaybackService` 组件名、通知图标）
2. `AsukaAppGraph` 通过 `SettingsRuntimeInstaller` / `PlaybackRuntimeInstaller` 组装 runtime feature
3. `AppComposition` 使用内联匿名对象将 graph 映射为窄依赖接口（`PlaybackActivityDependencies`、`PlaybackServiceDependencies`）
4. `AsuraPlayerApp` 暴露 `MainActivityDependencies` / `PlaybackActivityDependencies` / `PlaybackServiceDependencies`
5. `MainActivity` / `player-renderer:PlaybackActivity` / `PlaybackService` 通过 `Provider.from(application)` 集中式查找读取这些窄依赖

这里的关键变化是：

- `AppComposition` 使用内联匿名对象实现窄依赖接口，消除了中间映射类（`PlaybackFeatureDependencies.kt` 已删除）
- engine 具体实现通过 `AsukaAppGraph` 构造器注入，`player-runtime` 不再编译时依赖 `player-engine`
- `PlaybackDependenciesProvider` / `MainActivityDependenciesProvider` 提供 `from(application)` 集中式查找方法，带诊断错误信息
- 架构边界检查在构建期验证 Application 实现了所需 provider 接口

播放入口目前只暴露两个窄依赖：

- `PlaybackActivityDependencies`
- `PlaybackServiceDependencies`

这意味着：

- `MainActivity`、`PlaybackActivity`、`PlaybackService` 不会直接拿到整张 `AsukaAppGraph`
- `player-contract` 只知道纯业务模型
- `player-ui` 只知道 contract/render-api 层 port，而不知道 platform/engine 里的具体实现
- 新增 feature 优先扩展 installer / factory，而不是继续膨胀 `Application`

## 播放链路

### 1. 启动阶段

- `MainActivity` 接收媒体选择或外部 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE`
- `IncomingPlaybackIntentReader` 用 `PlaybackSessionRequestCodec.fromExternalIntent()` 把外部 intent 归一成单一 request
- `PlaybackLaunchCoordinator` 负责：
  - 对 request 当前项做 URI 解析与初始 playback URI 决议
  - 保留 stable mediaId / 原始 queue / startIndex 语义
  - 生成用于启动 `player-renderer:PlaybackActivity` 的 intent
- `PlaybackLaunchOrchestrator` / `PlaybackSessionCoordinator` 在播放页只消费同一份 `PlaybackSessionRequest`，不再重复拼装 queue / policy / fallback 语义

说明：

- 播放运行时设置不再通过 intent snapshot 传递
- `PlaybackActivity` 在实例化时就拿到 `PlaybackActivityDependencies`

### 2. 会话连接阶段

- `player-renderer:PlaybackActivity` 通过 `viewModels` 持有 `PlaybackViewModel`
- `PlaybackViewModel` 作为 `AndroidViewModel`，持有 `PlaybackSessionHost`、`PlaybackActivityBehavior` 和播放 host 状态，跨 configuration change 存活
- `PlaybackActivity` 直接持有 `PlaybackWindowChromeController`、`PlaybackPictureInPictureController`，在各生命周期回调里协调 PiP / 窗口 / 亮度
- `PlaybackSessionHost` 现在只保留生命周期主时序
- `PlaybackControllerConnection` 负责 `MediaController` 建连与 session 协调器装配
- `PlaybackSessionLaunchDriver` 负责请求落地、seek fallback 触发与过期请求丢弃
- `PlaybackSessionStateFeeds` 负责把 `PlaybackTrackUiStateHolder` 状态喂给 host，`PlayerUiState` 通过独立 `StateFlow` 直接暴露（不再合入 `PlaybackHostState`）
- `PlaybackLaunchOrchestrator` 负责当前 launch intent、seek fallback 与 runtime policy 编排
- `PlaybackLaunchOrchestrator` 现在为每次播放请求分配 request id；新 intent 会使旧 request、旧 fallback job 和旧启动结果全部失效
- state feed 层维护两类状态：
  - `PlayerUiStateHolder`：播放中的标题、时长、进度、错误、buffering
  - `PlaybackTrackUiStateHolder`：音轨、字幕、当前倍速、当前媒体 ID、选中项

### 3. 执行阶段

- `PlaybackSessionCoordinator` 把启动 intent 与 `PlaybackSessionPlanner` 输出的 `PlaybackSessionPlan` 应用到控制器
- `PlaybackSessionCoordinator` 现在先 `prepare` 再 `apply`，让 host 可以在真正落地前丢弃过期计划
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

Media3 到 UI 的翻译已经前置到 renderer/host 层完成。

`PlayerScreen` 本身也已拆成三类壳层：

- `PlayerScreenLayoutShell`
- `PlayerScreenGestureShell`
- `PlayerScreenOverlayShell`

副作用链单独收敛在 `PlayerScreenEffects`，overlay 面板则进一步按 settings / tracks / speed 拆成独立 section 文件。

视频表面本身也不再由 `player-ui` 拥有实现：

- `player-ui` 只消费 `player-render-api` 里的 `PlaybackSurfaceState` / `PlaybackSurfaceRenderer`
- `player-renderer` 提供 `Media3PlaybackSurfaceRenderer`
- `player-ui` 主源码不再直接 import `androidx.media3.*`

### 5. App UI 组织

- `MainLibraryScreen` 只负责：
  - 读取 `ViewModel` 状态
  - 处理 document picker / permission launcher / toast / 对话框入口
  - 把聚合后的状态传给 `MainLibraryNavHost`
- `MainLibraryUiState` 负责 library feature 入口状态聚合，不再把这部分 remember 逻辑塞进导航装配文件
- `MainLibraryViewModel` 只负责设置项更新与把 catalog store 暴露给 UI
- `MainLibraryCatalogStore` 现在是门面对象，只保留权限状态、变化观察与对外 API
- 分页状态机已经拆成四个 slice：
  - `MainLibraryFoldersSlice`
  - `MainLibraryAllVideosSlice`
  - `MainLibraryCurrentFolderSlice`
  - `MainLibraryRecentSlice`
- current folder 分页现在使用 request token + cancel stale job，旧 folder 的结果不能再回写当前 folder 页
- `AndroidVideoAccessDataSource` / `AndroidMediaStoreVideoCatalogDataSource` / `PlaybackRecentMediaDataSource` 提供媒体库底层数据
- `MediaLibraryIndexingCoordinator` 负责 MediaStore -> 本地索引同步与 `ContentObserver` 驱动的增量收敛
- `ResolveVideoAccessUseCase` / `LoadFolderPageUseCase` / `LoadVideoPageUseCase` / `LoadRecentMediaIdsUseCase` / `ResolveRecentMediaItemsUseCase` / `ObserveMediaLibraryChangesUseCase` 负责 feature 规则
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
- settings repository 的首个值来自当前内存快照，真实 store 完成初始化后再异步接管；这样既避免主线程阻塞，也避免长期停留在默认值
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
- `PlaybackRuntimeFeature` 通过 deferred store wrapper + suspend resolver 延迟解析 persistence store，并在 app scope 里做后台预热，避免首次建库工作落在播放热路径
- SharedPreferences playback/history store 现在主要用于 legacy import 和回归测试
- `MediaLibraryIndexingCoordinator` 在没有 generation 列的设备上，除了 `DATE_MODIFIED` 水位，还会对已观察到的新增/变更 `_ID` 做定点 metadata 回补，降低旧时间戳文件漏索引的概率

### 播放状态

- `PlaybackStateWriter` 写回：
  - 播放位置
  - 倍速
  - 音轨 / 字幕稳定 ID
- `PlaybackStateWriter` 与 `QueueHistoryWriter` 已从纯 contract 移到 `player-platform`
- writer 写入的底层 store 默认已经是 Room-backed store，而不是 SharedPreferences
- `OverlayActions` / `OverlayTrackActions` 不再直接落盘 speed/track，UI 只发命令
- writer 内部使用 `SerialTaskQueue` 串行排队回写；播放器回调本身不直接同步写盘
- 写回策略：
  - seek / pause / ended 等事件驱动
  - 播放中低频 checkpoint
  - service 销毁时触发非阻塞 flush + close（队列消费者在自有 IO scope 上处理剩余项），不阻塞主线程

### UI 进度刷新

- `PlayerUiStateHolder` 的 position/duration 刷新采用“事件驱动 + 播放中短轮询”
- attach 后不会无条件常驻 ticker
- 只有在 `player.isPlaying` 时才启动短周期刷新，暂停后立即停止
- 标题、错误、buffering、媒体切换等状态仍然主要依赖 `Player.Listener` 事件

### 最近历史

- `QueueHistoryStore` 现在要求实现必须可跨线程安全调用
- 默认实现是 `RoomQueueHistoryStore`
- `SharedPreferencesQueueHistoryStore` 与 `InMemoryQueueHistoryStore` 仍保留，用于迁移导入和测试

## 媒体库索引与增量同步

- 媒体库 UI 读取路径不再直接查询 MediaStore；读取路径改为：
- `MediaLibraryIndexingCoordinator` 同步 MediaStore 到 `AsukaMediaLibraryIndexDatabase`
- `AndroidMediaStoreVideoCatalogDataSource` 从本地索引库做分页读取
- `MainLibraryCatalogStore` facade 统一暴露 `foldersState` / `allVideosState` / `currentFolderVideosState` / `recentKnownVideos`
- 实际状态迁移由四个 media-library slice 分别维护
- 增量同步优先级：
  - Android R+ 优先使用 `GENERATION_ADDED` / `GENERATION_MODIFIED`
  - 低版本回退到 `DATE_MODIFIED`
  - 删除优先使用 `ContentObserver` 观察到的具体 URI 做定点清理
  - 只有兜底场景才做全量 `_ID` reconcile
- 页面级分页反馈：
  - 首次加载：页内 loading block
  - 空页：empty block
  - 刷新失败：顶部错误块
  - 触底追加：footer loading
  - 触底追加失败：footer retry block

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

## 治理与约束

- 架构验证和文件大小检查的 task 实现位于 `buildSrc/src/main/kotlin/VerificationTasks.kt`，根 `build.gradle.kts` 只保留 task 注册
- `verifyArchitectureBoundaries`
  - 校验模块依赖、包归属、播放入口 manifest、`player-ui` 对 Media3 / Activity / platform 的零直接依赖
  - 验证 `player-runtime` 不依赖 `player-engine`（engine 绑定由 `app` 层注入）
  - 验证 Application 类实现了 `PlaybackDependenciesProvider` 和 `MainActivityDependenciesProvider`
- `verifySourceFileSizes`
  - 扫描所有模块 `src/main/java` 根目录，而不是只看少数 feature 目录
  - page 类文件默认预算 320 行
  - orchestration / state / implementation 类文件默认预算 280 行，命名覆盖 `ViewModel` / `Coordinator` / `Host` / `Activity` / `Repositories` / `Store` / `Impl` / `Installer` / `Slice` / `Driver` / `Indexing`
  - baseline 文件只允许描述当前仍存在且仍被扫描的超限文件；失效路径会直接让校验失败

## 当前推荐阅读顺序

1. `README.md`
2. `player-runtime/src/main/java/com/asuka/player/runtime/AppGraph.kt`
3. `app/src/main/java/com/asuka/player/app/AsuraPlayerApp.kt`
4. `player-runtime/src/main/java/com/asuka/player/runtime/SettingsRuntimeInstaller.kt`
5. `player-runtime/src/main/java/com/asuka/player/runtime/PlaybackRuntimeInstaller.kt`
6. `app/src/main/java/com/asuka/player/app/AppComposition.kt`
7. `app/src/main/java/com/asuka/player/app/MainLibraryFeatureInstaller.kt`
8. `app/src/main/java/com/asuka/player/app/MainLibraryCatalogStore.kt`
9. `app/src/main/java/com/asuka/player/app/MainLibraryCatalogSlices.kt`
10. `app/src/main/java/com/asuka/player/app/MediaLibraryIndexing.kt`
11. `player-runtime/src/main/java/com/asuka/player/runtime/PlaybackLaunchCoordinator.kt`
12. `player-renderer/src/main/java/com/asuka/player/renderer/activity/PlaybackViewModel.kt`
13. `player-renderer/src/main/java/com/asuka/player/renderer/activity/PlaybackSessionHost.kt`
14. `player-renderer/src/main/java/com/asuka/player/renderer/activity/PlaybackControllerConnection.kt`
15. `player-renderer/src/main/java/com/asuka/player/renderer/activity/PlaybackSessionLaunchDriver.kt`
16. `player-ui/src/main/java/com/asuka/player/ui/PlayerScreen.kt`
17. `player-ui/src/main/java/com/asuka/player/ui/PlayerScreenShells.kt`
18. `player-engine/src/main/java/com/asuka/player/engine/service/PlaybackService.kt`
19. `player-data/src/main/java/com/asuka/player/data/AppSettingsStore.kt`

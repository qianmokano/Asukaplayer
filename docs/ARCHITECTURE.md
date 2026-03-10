# 架构说明

## 设计原则

- 单一组合根：`AsuraPlayerApp` 是唯一装配入口，负责 graph、entry point 和应用级依赖 container 初始化。
- framework 入口显式注入：`MainActivity`、`PlaybackActivity`、`PlaybackService` 通过 `AsuraPlayerApp` 提供的窄依赖 container 读取依赖，不再通过静态 registry 注入。
- 单一设置真相源：播放运行时设置统一来自 `PlaybackRuntimeSettingsSource`。
- UI 依赖 UI 模型：播放页消费 `PlaybackScreenModel` / `PlaybackScreenDependencies`，不直接拼装 Media3 细节。
- 策略与落盘分离：规划、执行、持久化分别由独立对象负责，减少隐式耦合。
- feature 分层优先：媒体库采用 data source -> repository -> use case -> view model，theme/settings model 保持纯值对象，不携带 Compose 类型。

## 模块边界

### `app`
- 应用入口与组合根
- `AsuraPlayerApp` 依赖 container
- 媒体库、设置页、导航
- `MediaLibraryDataSources` / `MediaLibraryRepository` / use cases
- UI 层按 feature slice 组织页面壳子与页面内容

### `player-contract`
- 稳定契约与模型
- `PlayerSettings` / `PlaybackRuntimeSettings`
- `PlaybackController` / `PlaybackStore` / `QueueHistoryStore`
- `PlaybackSessionPlanner`、`PlaybackStateRepository`、`QueueHistoryRepository`
- `PlaybackActivityDependencies` / `PlaybackServiceDependencies`

### `player-runtime`
- `AsukaAppGraph`
- 设置仓库与 `PlaybackRuntimeSettingsSource`
- 播放启动编排：`PlaybackLaunchCoordinator`
- `PlaybackUiPersistence`、`PlaybackDeviceControllerFactory`
- 不再依赖 Compose UI 类型，theme/settings model 使用纯 ARGB / 基础值类型

### `player-ui`
- `PlaybackActivity`、`PlaybackSessionHost`
- `PlaybackLaunchOrchestrator`
- `PlaybackWindowChromeController` / `PlaybackPictureInPictureController`
- `PlayerScreen` 与播放 UI 组件
- 手势编排、UI 状态、Media3 -> UI 的翻译层

### `player-engine`
- `PlaybackService`、Media3 controller/service 实现
- intent / seek fallback / track reader 等 Android/Media3 运行实现

### `player-domain`
- 手势算法与状态机
- 纯 JVM，可直接单元测试

### `player-data`
- `PlaybackStore`、`QueueHistoryStore`、`AppSettingsStore`
- SharedPreferences 落盘实现

## 组合根与依赖装配

当前唯一的运行时依赖装配路径是：

1. `AsuraPlayerApp` 创建 `AsukaAppGraph`
2. `AsuraPlayerApp` 组装 `MainActivityDependencies` / `PlaybackActivityDependencies` / `PlaybackServiceDependencies`
3. `AsuraPlayerApp` 暴露 `MainActivityDependencies` / `PlaybackActivityDependencies` / `PlaybackServiceDependencies`
4. `MainActivity` / `PlaybackActivity` / `PlaybackService` 在各自入口内从 `Application` 读取这些窄依赖

播放入口目前只暴露两个窄依赖：

- `PlaybackActivityDependencies`
- `PlaybackServiceDependencies`

这意味着：

- `MainActivity`、`PlaybackActivity`、`PlaybackService` 不会直接拿到整张 `AsukaAppGraph`
- `player-contract` / `player-ui` 只知道窄依赖接口，而不知道 `app` 层的装配细节

## 播放链路

### 1. 启动阶段

- `MainActivity` 接收媒体选择或外部 `ACTION_VIEW` / `ACTION_SEND` / `ACTION_SEND_MULTIPLE`
- `PlaybackLaunchCoordinator` 负责：
  - 解析目标 URI
  - 必要时做 seek fallback
  - 转发或构造显式 `ClipData` 队列
  - 生成用于启动 `PlaybackActivity` 的 intent

说明：

- 播放运行时设置不再通过 intent snapshot 传递
- `PlaybackActivity` 在实例化时就拿到 `PlaybackActivityDependencies`

### 2. 会话连接阶段

- `PlaybackActivity` 持有 `PlaybackSessionHost`
- `PlaybackActivity` 将窗口副作用委托给 `PlaybackWindowChromeController` / `PlaybackPictureInPictureController`
- `PlaybackSessionHost` 通过 `ControllerProvider` 和注入的 `playbackServiceComponent` 建立 `MediaController`
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
- `MainLibraryViewModel` 只负责状态流和 UI 事件，不再直接查询 MediaStore
- `AndroidVideoAccessDataSource` / `AndroidMediaStoreVideoCatalogDataSource` / `PlaybackRecentMediaDataSource` 提供媒体库底层数据
- `ResolveVideoAccessUseCase` / `RefreshMediaLibraryUseCase` / `LoadRecentMediaIdsUseCase` 负责 feature 规则
- `MainLibraryNavHost` 负责导航和页面装配
- `LibraryChrome` / `LibraryPages` / `SettingsPageContent` / `PlayerSettingsPageContent` / `MotionSettingsPageContent` / `ThemeSettingsScreen` 负责页面级 UI
- 主题与共享 UI 已拆成职责文件，而不是继续堆在单个 600+ 行文件里

这样做的目的：

- 降低媒体库与设置页入口文件的冲突率
- 让导航壳子与页面内容可独立修改
- 避免在单个超大文件里同时处理 launcher、导航、页面布局和业务交互

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

### 播放状态

- `PlaybackStateWriter` 写回：
  - 播放位置
  - 倍速
  - 音轨 / 字幕稳定 ID
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
- SharedPreferences 与 InMemory 实现都做了同步保护

## 当前推荐阅读顺序

1. `README.md`
2. `player-runtime/src/main/java/com/asuka/player/app/AppGraph.kt`
3. `app/src/main/java/com/asuka/player/app/AsuraPlayerApp.kt`
4. `app/src/main/java/com/asuka/player/app/AsuraPlayerApp.kt`
5. `player-runtime/src/main/java/com/asuka/player/app/PlaybackLaunchCoordinator.kt`
6. `app/src/main/java/com/asuka/player/app/MediaLibraryDataSources.kt`
7. `app/src/main/java/com/asuka/player/app/MediaLibraryRepository.kt`
8. `app/src/main/java/com/asuka/player/app/MainLibraryViewModel.kt`
9. `app/src/main/java/com/asuka/player/app/MainLibraryNavHost.kt`
10. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackActivity.kt`
11. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackSessionHost.kt`
12. `player-contract/src/main/java/com/asuka/player/core/PlaybackSessionPlanner.kt`
13. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackLaunchOrchestrator.kt`
14. `player-engine/src/main/java/com/asuka/player/core/service/PlaybackService.kt`

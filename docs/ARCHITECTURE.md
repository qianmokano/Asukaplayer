# 架构说明

## 设计原则

- 单一组合根：所有播放运行时依赖都从 `AsukaAppGraph` 出发，不再通过全局 registry 回退。
- 单一设置真相源：播放运行时设置统一来自 `PlaybackRuntimeSettingsSource`。
- UI 依赖 UI 模型：播放页消费 `PlaybackScreenModel` / `PlaybackScreenDependencies`，不直接拼装 Media3 细节。
- 策略与落盘分离：规划、执行、持久化分别由独立对象负责，减少隐式耦合。

## 模块边界

### `app`
- 应用入口与组合根
- 媒体库、设置页、导航
- 播放启动编排：`PlaybackLaunchCoordinator`

### `player-ui`
- `PlaybackActivity`、`PlaybackSessionHost`
- `PlayerScreen` 与播放 UI 组件
- 手势编排、UI 状态、Media3 -> UI 的翻译层

### `player-core`
- `PlaybackController` 抽象
- `PlaybackSessionPlanner`、`QueuePlanner`
- `PlaybackService`、`PlaybackStateWriter`

### `player-domain`
- 手势算法与状态机
- 纯 JVM，可直接单元测试

### `player-data`
- `PlaybackStore`、`QueueHistoryStore`、`AppSettingsStore`
- SharedPreferences 落盘实现

## 组合根与依赖装配

当前唯一的运行时依赖装配路径是：

1. `AsuraPlayerApp` 创建 `AsukaAppGraph`
2. `AsuraPlayerApp` 通过 `PlaybackCoreGraphProvider` 暴露 graph
3. 需要播放依赖的组件通过 `Context.requirePlaybackCoreGraph()` 获取 graph

`PlaybackCoreGraph` 目前负责暴露：

- `playbackStore`
- `queueHistoryStore`
- `playbackStateRepository`
- `playbackSessionPlanner`
- `playbackRuntimeSettingsSource`
- `playbackServiceComponent`
- `sessionActivityClass`
- `notificationSmallIconResId`

这意味着：

- `player-core` 不再依赖全局单例 registry
- `player-ui` 不再硬编码 `PlaybackService`，而是消费 graph 提供的 service component

## 播放链路

### 1. 启动阶段

- `MainActivity` 接收媒体选择或外部 `ACTION_VIEW`
- `PlaybackLaunchCoordinator` 负责：
  - 解析目标 URI
  - 必要时做 seek fallback
  - 转发或构造显式 `ClipData` 队列
  - 生成用于启动 `PlaybackActivity` 的 intent

说明：

- 播放运行时设置不再通过 intent snapshot 传递
- `PlaybackActivity` 启动后直接读取当前 `PlaybackRuntimeSettingsSource`

### 2. 会话连接阶段

- `PlaybackActivity` 持有 `PlaybackSessionHost`
- `PlaybackSessionHost` 通过 `ControllerProvider` 和 graph 提供的 `playbackServiceComponent` 建立 `MediaController`
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

## 运行时设置

当前播放运行时设置的唯一真相源是 `AppPlaybackRuntimeSettingsSource`。

它由两部分合成：

- `PlayerSettingsRepository`
- `PlaybackBehaviorRepository`

关键结果：

- 播放页启动时读取的是当前值，而不是旧的 intent 快照
- `hideButtonsBackground` 等默认值与 `PlayerSettingsConfig` 保持一致
- 设置变化通过 flow 持续同步到播放页

## 持久化与恢复

### 播放状态

- `PlaybackStateWriter` 写回：
  - 播放位置
  - 倍速
  - 音轨 / 字幕稳定 ID
- 写回策略：
  - seek / pause / ended 等事件驱动
  - 播放中低频 checkpoint
  - service 销毁前 flush

### 最近历史

- `QueueHistoryStore` 现在要求实现必须可跨线程安全调用
- SharedPreferences 与 InMemory 实现都做了同步保护

## 当前推荐阅读顺序

1. `README.md`
2. `app/src/main/java/com/asuka/player/app/AppGraph.kt`
3. `app/src/main/java/com/asuka/player/app/PlaybackLaunchCoordinator.kt`
4. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackActivity.kt`
5. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackSessionHost.kt`
6. `player-ui/src/main/java/com/asuka/player/ui/PlayerScreenContract.kt`
7. `player-ui/src/main/java/com/asuka/player/ui/PlayerScreen.kt`
8. `player-core/src/main/java/com/asuka/player/core/PlaybackSessionPlanner.kt`
9. `player-core/src/main/java/com/asuka/player/core/service/PlaybackService.kt`

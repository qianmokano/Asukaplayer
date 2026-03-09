# Asuka Player

一款基于 Jetpack Compose + Media3/ExoPlayer 的 Android 本地视频播放器。

## 当前能力

- 本地媒体库浏览、文件夹分组、最近播放
- 外部 `ACTION_VIEW` / `ClipData` 多文件启动
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
app/            应用入口、媒体库、设置页、组合根
player-ui/      播放页 UI、会话宿主、UI 侧状态与动作翻译
player-core/    播放抽象、MediaSessionService、队列/恢复规划
player-domain/  纯 JVM 算法与状态机
player-data/    持久化接口与 SharedPreferences 实现
```

依赖方向：`app` → `player-ui` → `player-core` → `player-data`；`player-ui` → `player-domain`

## 关键架构

1. `AsuraPlayerApp` 构建 `AsukaAppGraph`，并通过 `PlaybackCoreGraphProvider` 暴露唯一运行时依赖图。
2. `MainActivity` 和 `PlaybackLaunchCoordinator` 负责解析启动 URI、seek fallback 与显式队列转发。
3. `PlaybackActivity` 只从 `PlaybackRuntimeSettingsSource` 读取当前运行时设置；不再维护额外的启动快照。
4. `PlaybackSessionHost` 使用 graph 提供的播放 service component 连接 `MediaController`，并把 Media3 状态翻译成 `PlaybackScreenModel` / `PlaybackScreenDependencies` 给 `PlayerScreen`。
5. `PlaybackSessionCoordinator` + `PlaybackSessionPlanner` 负责队列、续播位置、倍速和轨道恢复。
6. `PlaybackService` + `PlaybackStateWriter` 负责播放状态写回，包含播放中 checkpoint 和销毁前 flush。

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

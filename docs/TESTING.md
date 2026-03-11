# 测试说明

## 默认本地基线

无设备环境下，默认执行：

```bash
./gradlew test
./gradlew :player-ui:compileDebugAndroidTestKotlin
```

含义：

- `./gradlew test`
  - 覆盖 `:app`、`:player-runtime`、`:player-engine`、`:player-platform`、`:player-contract`、`:player-domain`、`:player-ui`、`:player-data` 的 JVM / Robolectric 单元测试
- `:player-ui:compileDebugAndroidTestKotlin`
  - 保证 `player-ui/src/androidTest` 与当前播放页 API 保持同步
  - 不需要设备

## 其他常用命令

```bash
# Kotlin 编译
./gradlew :app:compileDebugKotlin

# Lint
./gradlew lintDebug

# 真机 / 模拟器 UI 测试
./gradlew :player-ui:connectedAndroidTest

# 安装 Debug 包
./gradlew :app:installDebug
```

## 当前重点测试面

### 启动与队列

- 外部 `ACTION_VIEW`
- 外部 `ACTION_SEND`
- 外部 `ACTION_SEND_MULTIPLE`
- `ClipData` 多文件启动
- seek fallback 后 URI / 队列一致性
- `AsuraPlayerApp` container 启动链路

### 恢复与持久化

- 续播位置
- 倍速恢复
- 轨道恢复
- 最近历史
- UI overlay 不直接写 speed/track store
- `player-contract` 纯 queue/session model 与 `player-platform` writer / mapper 的边界一致性
- `SharedPreferencesAppSettingsStore` -> DataStore migration
- `SharedPreferencesPlaybackStore` / `SharedPreferencesQueueHistoryStore` -> Room import
- `AppSettingsSnapshot` schema compatibility
- Room-backed playback/history store 的顺序、裁剪和 round-trip

### 播放页行为

- 控制栏显隐与锁定
- overlay 开关
- PiP / 后台保活策略
- controller 建连失败后的错误态 / 重试
- 错误态与重试
- 进度 ticker 在播放/暂停切换时是否正确启停

## 手动测试清单

### 启动与基础播放

1. 从媒体库打开一个本地视频。
2. 从外部文件管理器以 `ACTION_VIEW` 打开单个视频。
3. 从分享入口以 `ACTION_SEND` 打开单个视频。
4. 从分享入口以 `ACTION_SEND_MULTIPLE` 或支持多选的来源触发多文件播放。

预期：

- 能进入播放页
- 当前文件正确
- 显式队列中的上一项 / 下一项可用

### 控制层与手势

1. 播放中静置，观察控制栏自动隐藏。
2. 点击空白区域，观察控制栏恢复。
3. 测试锁定、双击、横向 seek、亮度、音量、缩放、平移。

预期：

- 锁定后禁用手势
- 各手势互斥
- 缩放和平移边界正常

### 轨道 / 倍速 / 比例

1. 打开速度面板并切换倍速。
2. 切换音轨 / 字幕。
3. 切换视频比例。

预期：

- 设置立即生效
- 重新进入同一媒体时恢复一致

### PiP / 后台

1. 进入 PiP。
2. 使用后台播放动作离开前台。
3. 回到应用。

预期：

- 会话和界面状态一致
- 返回前台后不丢播放上下文

### 媒体库分层

1. 首次进入媒体库页面，确认权限状态、列表刷新、最近播放加载正常。
2. 触发刷新，确认最小刷新动画时长与缩略图预热不影响主列表展示。
3. 在有限媒体权限和完整权限之间切换，确认状态更新正常。

预期：

- `MainLibraryViewModel` 只做状态编排
- `MediaLibraryRepository` / use case 负责媒体库规则
- MediaStore 查询与最近播放读取由 data source 承担

### 错误与恢复

1. 用损坏媒体或不可读 URI 触发错误。
2. 模拟 service / controller 建连失败，确认不会出现黑屏。
3. 测试重试和下一项。

预期：

- 有错误提示
- 不崩溃
- 可重试或安全跳过

## CI 建议

- PR 默认执行：
  - `./gradlew test`
  - `./gradlew :player-ui:compileDebugAndroidTestKotlin`
- 可选补充：
  - `./gradlew lintDebug`
- 持久化 schema 变更时额外检查：
  - `player-data/schemas/` 是否有对应导出更新
- 设备环境或 nightly 再执行：
  - `./gradlew :player-ui:connectedAndroidTest`

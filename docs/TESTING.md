# 测试说明

## 默认本地基线

无设备环境下，默认执行：

```bash
./gradlew test
./gradlew :player-ui:compileDebugAndroidTestKotlin
./gradlew verifyArchitectureBoundaries verifySourceFileSizes
./gradlew help
```

含义：

- `./gradlew test`
  - 覆盖 `:app`、`:player-runtime`、`:player-engine`、`:player-platform`、`:player-render-api`、`:player-renderer`、`:player-contract`、`:player-domain`、`:player-ui`、`:player-data` 的 JVM / Robolectric 单元测试
- `:player-ui:compileDebugAndroidTestKotlin`
  - 保证 `player-ui/src/androidTest` 与当前播放页 API 保持同步
  - 不需要设备
- `./gradlew verifyArchitectureBoundaries verifySourceFileSizes`
  - 验证模块边界、播放入口约束和文件体量预算
  - `verifySourceFileSizes` 现在会扫描全部 `src/main/java` 模块根目录，并在 baseline 出现失效路径时直接失败
- `./gradlew help`
  - 快速验证 `Gradle configuration cache` 是否已经可复用
  - 预期输出包含 `Configuration cache entry reused.`

## 其他常用命令

```bash
# Kotlin 编译
./gradlew :app:compileDebugKotlin

# 架构与体积约束
./gradlew verifyArchitectureBoundaries verifySourceFileSizes

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
- `PlaybackSessionRequest` 编解码 round-trip
- stable mediaId 与 fallback runtime URI 共存
- seek fallback 后 URI / 队列一致性
- superseded launch request / seek fallback 不得在新 request 之后落地
- `AsuraPlayerApp` container 启动链路

### 恢复与持久化

- settings 写入完成语义（调用返回时已持久化）
- 续播位置
- 倍速恢复
- 轨道恢复
- 最近历史
- UI overlay 不直接写 speed/track store
- `player-contract` 纯 queue/session model 与 `player-platform` writer / mapper 的边界一致性
- `SharedPreferencesAppSettingsStore` -> DataStore migration
- `SharedPreferencesPlaybackStore` / `SharedPreferencesQueueHistoryStore` -> Room import
- deferred persistence resolver 首次初始化
- `AppSettingsSnapshot` schema compatibility
- Room-backed playback/history store 的顺序、裁剪和 round-trip
- playback/history writer 在慢存储场景下不阻塞播放器回调线程
- `PlaybackService` 启动/销毁生命周期，包括非阻塞 teardown 与后台持久化 drain
- settings repository 冷启动先发布最佳可用内存快照，随后由真实 store 异步接管
- `MediaLibraryIndexingCoordinator` 首次索引后使用增量条件（`DATE_MODIFIED` / generation）继续同步
- 已观察到的删除 URI 优先走定点清理，而不是每次都全量 `_ID` reconcile
- 已观察到的新增 `_ID` 在旧 `DATE_MODIFIED` 时间戳场景下仍会补 metadata，不会静默漏进索引

### 播放页行为

- 控制栏显隐与锁定
- overlay 开关
- PiP / 后台保活策略
- controller 建连失败后的错误态 / 重试
- 错误态与重试
- 进度 ticker 在播放/暂停切换时是否正确启停
- surface renderer 契约是否仍保持在 `player-render-api`
- `player-ui` 是否继续保持对 Media3 / Activity 零直接依赖

### 媒体库分页与 UX

- 首次加载、空态、刷新失败、追加加载、追加失败的状态切换
- 快速切换 folder 时，旧分页结果不会覆盖当前 folder 状态
- recent 列表对 `media-store:` / `content` / `file` / `http(s)` / opaque id 的解析语义
- opaque recent id 应展示为不可用且不可点击，而不是当成伪 URI 回放
- `ContentObserver` 触发后已加载列表是否自动收敛到索引最新状态

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
- fallback 文件 URI 与稳定 mediaId 保持一致，不会在恢复时串档

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
4. 删除、添加、修改媒体文件后，确认列表会自动收敛，不需要手动刷新。
5. 在触底加载时断网或模拟 provider 失败，确认 footer error / retry 正常。

预期：

- `MainLibraryViewModel` 只做状态编排
- `MainLibraryCatalogStore` 作为 facade 暴露状态与入口
- folders / all videos / current folder / recent 四个 slice 各自维护局部分页状态机
- `MediaLibraryRepository` / use case 负责媒体库规则
- 页面读取走本地索引而不是直接走 MediaStore 全量查询

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
  - `./gradlew verifyArchitectureBoundaries verifySourceFileSizes`
  - `./gradlew help`
- 可选补充：
  - `./gradlew lintDebug`
- 组合根 / request / persistence contract 发生改动时，优先检查：
  - `MainActivityDirectPlaybackTest`
  - `PlaybackLaunchCoordinatorTest`
  - `PlaybackLaunchOrchestratorTest`
  - `IntentQueueReaderTest`
  - `MainLibraryCatalogStoreTest`
  - `SettingsRepositoriesTest`
  - `DataStoreAppSettingsStoreTest`
  - `PlaybackPersistenceStoresFactoryTest`
  - `PlaybackStateWriterTest`
  - `QueueHistoryWriterTest`
  - `PlaybackServiceTest`
  - `MediaLibraryIndexingCoordinatorTest`
  - `RecentPlaybackDescriptorTest`
- 持久化 schema 变更时额外检查：
  - `player-data/schemas/` 是否有对应导出更新
- 设备环境或 nightly 再执行：
  - `./gradlew :player-ui:connectedAndroidTest`

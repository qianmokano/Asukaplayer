# M4 测试运行说明

## 单元测试
- `./gradlew test`

说明：
- 该命令覆盖 `:app`、`:player-core`、`:player-domain`、`:player-ui`、`:player-data` 的 JVM 单元测试
- 如需单独验证播放器会话编排，可运行 `./gradlew :player-core:testDebugUnitTest --tests "com.asuka.player.core.PlaybackSessionPlannerTest"`
- 如需单独验证播放页控制层逻辑，可运行 `./gradlew :player-ui:testDebugUnitTest`

## Android 仪器测试源码编译预检
- `./gradlew :player-ui:compileDebugAndroidTestKotlin`

说明：
- 该命令不需要连接设备
- 用于保证 `player-ui/src/androidTest` 与当前 `PlaybackController`、`PlayerScreen` 等 API 保持同步
- 适合作为 PR 的默认预检门禁

## Lint
- `./gradlew lintDebug`

## UI 测试
- `./gradlew :player-ui:connectedAndroidTest`

说明：
- UI 测试需要连接设备或模拟器
- 该命令不属于当前无设备默认基线，建议在设备环境或 nightly 中执行

# M4 测试运行说明

## 单元测试
- `./gradlew test`

说明：
- 该命令覆盖 `:app`、`:player-core`、`:player-domain`、`:player-ui`、`:player-data` 的 JVM 单元测试
- 如需单独验证播放器会话编排，可运行 `./gradlew :player-core:testDebugUnitTest --tests "com.asuka.player.core.PlaybackSessionPlannerTest"`
- 如需单独验证播放页控制层逻辑，可运行 `./gradlew :player-ui:testDebugUnitTest`

## Lint
- `./gradlew lintDebug`

## UI 测试
- `./gradlew :player-ui:connectedAndroidTest`

> 注意：UI 测试需要连接设备或模拟器。

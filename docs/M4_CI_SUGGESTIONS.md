# M4 CI 建议

- 在 PR 中运行 `./gradlew test :player-ui:compileDebugAndroidTestKotlin`
- 如需补充静态检查，可在 PR 或 merge gate 中加入 `./gradlew lintDebug`
- nightly 或带设备环境的 main 分支运行 `./gradlew :player-ui:connectedAndroidTest`
- 失败时保留测试报告与截图

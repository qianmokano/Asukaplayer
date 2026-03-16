# Versioning / 版本管理

## Goals / 目标

- 版本号只有一个事实来源，避免 `versionName` 和 `versionCode` 手工不同步。
- 发版时只改 `MAJOR.MINOR.PATCH` 三个字段，不再直接改模块脚本里的数字。
- 让 Android 安装升级所需的 `versionCode` 和人类可读的 `versionName` 保持可推导关系。

## Source Of Truth / 单一来源

版本号统一维护在根目录 [gradle.properties](../gradle.properties)：

```properties
appVersionMajor=0
appVersionMinor=1
appVersionPatch=0
```

`app` 模块和根任务都通过 [AppVersioning.kt](../buildSrc/src/main/kotlin/com/asuka/player/build/AppVersioning.kt) 读取这三个属性并计算最终版本。

## Rules / 规则

- `versionName = MAJOR.MINOR.PATCH`
- `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH`
- `MINOR` 和 `PATCH` 必须在 `0..99` 之间，保证 `versionCode` 可逆且不会位数冲突。
- `MAJOR` 必须大于等于 `0`。
- 不允许在 [app/build.gradle.kts](../app/build.gradle.kts) 里手工写死 `versionName` / `versionCode`。

示例：

- `0.1.0` -> `100`
- `0.12.3` -> `1203`
- `1.0.0` -> `10000`
- `2.4.15` -> `20415`

## Semantics / 语义

当前项目仍处在 `0.x` 阶段，建议采用下面的语义：

- `PATCH`：只做 bugfix、回归修复、文案修正、非行为性小调整；不引入新能力，不改变已有默认行为。
- `MINOR`：新增用户可见能力、默认行为调整、模块边界重构、持久化结构演进、需要在 changelog 单独说明的改动。
- `MAJOR`：对外行为或兼容性发生显著变化，例如配置/数据迁移不可逆、播放语义重定义、API/存档不再兼容、产品定位进入稳定发布阶段（如 `1.0.0`）。

补充约定：

- 在 `0.x` 阶段，如果出现“对现有用户有明显迁移成本”的 breaking change，也仍然通过提升 `MINOR` 表达。
- 当播放启动、媒体库索引、持久化 schema、后台/PiP 策略等核心体验稳定后，再进入 `1.0.0`。

## Release Workflow / 发版流程

1. 评估本次改动属于 `MAJOR` / `MINOR` / `PATCH` 哪一类。
2. 只修改 [gradle.properties](../gradle.properties) 中的三个字段。
3. 更新 [docs/CHANGELOG.md](./CHANGELOG.md)。
4. 运行 `./gradlew printAppVersion`，确认生成结果正确。
5. 运行至少一轮本地验证：`./gradlew test`。

## Build Behavior / 构建行为

- 缺少版本属性、属性不是整数、`MINOR/PATCH` 超出 `0..99` 时，Gradle 会在配置阶段直接失败。
- 根任务 `printAppVersion` 可用于快速查看当前构建实际使用的版本：

```bash
./gradlew printAppVersion
```

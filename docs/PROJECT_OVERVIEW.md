# Asuka Player 项目全景说明

## 1. 项目定位
- 项目名称：`Asuka Player`
- 形态：Android 本地视频播放器（Jetpack Compose UI）
- 当前状态：里程碑 M0-M4 初版已完成（见 `README.md` 与 `docs/STATUS_AND_TODO.md`）
- 目标：在可维护、可扩展前提下，提供流畅的本地播放、手势交互、队列播放与设置能力
- 合规约束：采用清洁室重写方式，避免复制第三方 GPL 项目代码与资源

## 2. 技术栈与运行环境
- 语言：Kotlin
- UI：Jetpack Compose + Material 3
- 播放内核：AndroidX Media3（ExoPlayer / Session）
- 构建：Gradle Kotlin DSL
- JDK：17
- Android 配置：
  - `minSdk = 23`
  - `targetSdk = 36`
  - `compileSdk = 36`

## 3. 模块架构
根工程采用多模块分层，入口在 `settings.gradle.kts`：
- `:app`
  - 应用入口与媒体库页面
  - 设置页、文件夹列表、导航与主题编排
  - 启动 `PlaybackActivity` 并下发运行时播放器参数
- `:player-ui`
  - 播放页 Compose UI 与交互协调
  - 手势识别与控制层状态机
  - Overlay 面板、进度条、反馈组件
- `:player-core`
  - 播放控制抽象与 Media3 适配
  - Service/Session、队列策略、播放状态读写
  - 轨道选择与恢复机制
- `:player-domain`
  - 纯算法与状态机（手势相关）
  - 便于单元测试和复用
- `:player-data`
  - 数据存储抽象与实现（播放状态持久化等）

## 4. 关键页面与能力
- 首页（媒体库）
  - 文件夹分组与数量展示
  - 下拉刷新与扫描反馈
  - 本地视频快速打开
- 播放页
  - 顶/中/底控制区
  - 横向快进、竖向音量/亮度、双击、长按、缩放/平移等手势
  - 控制层自动隐藏、锁定、进度反馈
- 播放器设置页
  - 手势、界面、播放行为配置
  - 参数已接入运行时业务（如续播、默认倍速、双击动作、长按倍速、自动 PIP、后台播放等）

## 5. 数据与状态流（简版）
- 用户在 `:app` 中选择媒体 -> 组装播放参数 -> 进入 `PlaybackActivity`
- `:player-ui` 负责 UI 状态与手势输入编排
- `:player-core` 执行播放控制、队列与状态恢复
- `:player-data` 持久化播放相关状态（位置、速度、轨道偏好等）

## 6. 测试与质量
- 单元测试：
  - `:player-domain:test`
  - `:player-core:test`
- UI/仪器测试：
  - `:player-ui:connectedAndroidTest`
- 已有文档：
  - `docs/M4_TEST_PLAN.md`
  - `docs/M4_UI_TEST_PLAN.md`
  - `docs/M4_PERF_CHECKLIST.md`
  - `docs/M4_RELEASE_CHECKLIST.md`

## 7. 构建与常用命令
- 编译 Kotlin：
  - `./gradlew :app:compileDebugKotlin`
- 安装 Debug 包：
  - `./gradlew :app:installDebug`
- 运行单元测试：
  - `./gradlew :player-domain:test :player-core:test`
- 运行 UI 测试（需设备）：
  - `./gradlew :player-ui:connectedAndroidTest`

## 8. 性能与体验关注点
- 冷启动首次进入文件夹可能出现掉帧，重点受以下因素影响：
  - 首批缩略图解码与列表首帧并发
  - 首次媒体扫描与 IO 峰值
  - 列表项动画/重组开销
- 已采取的优化方向（项目内）：
  - 减少设置页不必要动画与重组
  - 手势与控制层逻辑拆分，降低耦合
  - 持续优化媒体加载与渲染路径

## 9. 合规与版权说明
- 本项目遵循清洁室重写原则。
- 可参考第三方产品的交互思路与信息架构，但不直接复制其 GPL 代码与资源文件。
- 图标方案建议：
  - 优先使用系统/官方图标库（如 Material Icons）
  - 自定义资源采用项目自绘或独立来源

## 10. 现阶段已知待办（摘要）
- 后台播放增强：前台通知、系统中断处理完善
- 轨道选择体验：展示信息与选中态优化
- 队列策略：扫描排序与最近播放策略继续打磨
- 发布流水线：Lint/测试门禁与版本发布规范化

## 11. 适合新同学的阅读顺序
1. `README.md`
2. `docs/STATUS_AND_TODO.md`
3. `app/src/main/java/com/asuka/player/app/MainActivity.kt`
4. `player-ui/src/main/java/com/asuka/player/ui/activity/PlaybackActivity.kt`
5. `player-ui/src/main/java/com/asuka/player/ui/PlayerScreen.kt`
6. `player-core/src/main/java/com/asuka/player/core/impl/Media3PlaybackController.kt`
7. `player-domain/src/main/java/com/asuka/player/domain/`


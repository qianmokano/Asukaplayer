# Asuka Player

Asuka Player 是一个使用 Jetpack Compose 和 Media3/ExoPlayer 构建的开源 Android 本地视频播放器。

这个项目希望同时做好两件事：一是提供现代、顺手、以本地媒体为核心的播放体验；二是保持一个长期可维护、边界清晰、便于扩展的 Android 多模块代码库。

English README: [README](../README.md)

## 为什么是 Asuka Player

- 面向本地文件的本地优先播放体验
- 使用 Jetpack Compose 和 Material 3 构建现代 Android UI
- 提供手势、倍速、音轨、字幕、PiP 等完整播放能力
- 通过本地索引媒体库获得更快的浏览和增量同步体验
- 用清晰模块边界替代单一巨大 app 模块

## 你可以用它做什么

### 浏览媒体库

- 按文件夹浏览本地视频
- 对大型媒体库做分页加载
- 查看最近播放记录
- 在本地媒体发生变化后自动同步列表状态

### 播放视频

- 从应用内媒体库直接打开视频
- 通过 `ACTION_VIEW`、`ACTION_SEND`、`ACTION_SEND_MULTIPLE` 和 `ClipData` 启动播放
- 使用手势和进度条进行 seek
- 调整亮度、音量、缩放、平移、画面比例、字幕、音轨和播放速度
- 恢复续播位置、速度设置和轨道选择

### 保留播放上下文

- 恢复播放进度
- 持久化播放速度、音轨/字幕选择、缩放状态和队列历史
- 使用画中画与后台播放保活
- 通过串行异步持久化避免在播放器回调里阻塞磁盘写入

## 项目状态

- 持续开发中
- 当前版本策略为 `0.x.y`
- Android 目标：`minSdk 23`、`targetSdk 36`、`compileSdk 36`
- 核心技术栈：Kotlin `2.3.0`、Jetpack Compose、Media3 `1.9.1`

## 技术亮点

- 使用独立播放 UI 模块的 Jetpack Compose 界面
- 通过明确的播放 port 隔离 Media3 / ExoPlayer 集成
- 使用 Room 本地媒体索引支撑分页浏览
- 使用 DataStore 持久化应用设置
- 使用 Room 持久化播放状态和队列历史
- 提供 JVM、Robolectric 与 Compose UI 回归测试
- 在构建阶段执行架构边界校验

## 模块结构

```text
app/               应用入口、媒体库功能、设置页面、顶层依赖装配
player-contract/   稳定 Kotlin 契约、会话规划、持久化和播放 port
player-platform/   Android / Media3 绑定、intent 适配、异步 writer、平台辅助层
player-render-api/ 渲染器中立的播放 surface 契约
player-renderer/   PlaybackActivity、会话装配、PiP、Media3 渲染适配
player-runtime/    运行时 graph、设置仓库、启动编排、设备与持久化装配
player-ui/         纯播放 UI 与手势编排，不直接依赖 Media3
player-engine/     PlaybackService 与 Media3 controller 实现
player-domain/     纯 JVM 算法与状态机
player-data/       Room/DataStore 实现、本地媒体索引、迁移兼容测试
```

主要依赖方向：

`app` -> `player-runtime` / `player-platform` / `player-renderer` / `player-data` / `player-engine`

`player-renderer` -> `player-render-api` / `player-ui` / `player-platform` / `player-contract`

`player-ui` -> `player-render-api` / `player-contract` / `player-domain`

`player-runtime` -> `player-contract` / `player-platform` / `player-data`

`player-engine` -> `player-contract` / `player-platform`

`player-data` -> `player-contract`

## 架构概览

- `AsuraPlayerApp` 是唯一组合根
- 所有播放启动输入都会先归一成一个 `PlaybackSessionRequest`
- 播放 host 的职责被拆分为连接、启动驱动和状态 feed
- `player-ui` 只消费 contract 和 render API，不直接依赖 Media3 原始类型
- 媒体库读取优先走本地 Room 索引，而不是页面级直接扫描 MediaStore
- 运行时设置和持久化路径都采用显式边界与异步写入语义

这种结构的目标，是让项目更容易测试、更容易重构，也更不容易出现跨层耦合逐渐失控的问题。

## 快速开始

### 环境要求

- JDK 17
- 已配置好与项目匹配的 Android SDK
- 能运行 Gradle Android 构建的本地开发环境

### 构建

```bash
./gradlew :app:compileDebugKotlin
```

### 默认本地验证基线

```bash
./gradlew test
./gradlew :player-ui:compileDebugAndroidTestKotlin
./gradlew verifyArchitectureBoundaries verifySourceFileSizes
./gradlew help
```

### 常用命令

```bash
# 查看集中管理后的应用版本
./gradlew printAppVersion

# Lint
./gradlew lintDebug

# 真机 / 模拟器 UI 测试
./gradlew :player-ui:connectedAndroidTest

# 安装 Debug APK
./gradlew :app:installDebug
```

## 开发理念

这个项目将架构与回归安全视作产品质量的一部分：

- UI 行为应该被测试，而不只是“能跑”
- 播放与持久化链路应该显式表达，而不是埋在隐式副作用中
- 模块边界应该由构建系统强制执行
- 本地媒体处理能力应该能随着项目规模增长而保持稳定

## 文档

- [Architecture](ARCHITECTURE.md) / [架构说明](ARCHITECTURE.md)
- [Testing](TESTING.md) / [测试说明](TESTING.md)
- [Versioning](VERSIONING.md) / [版本管理](VERSIONING.md)

## 开源意图

Asuka Player 不只是一个播放器项目，也是一个关于 Android 播放运行时、渲染、UI、持久化和本地媒体处理如何解耦协作的开源实践。模块拆分和边界控制不是“形式化设计”，而是为了让这个仓库在功能继续增长时仍然可读、可测、可维护。

# 完成状态总结 + 待办清单

## 完成状态总结

**里程碑完成度：M0–M4（初版）✅**

- **M0 基础工程与播放骨架** ✅
  - Service + MediaController + 最小可播放闭环
- **M1 手势与状态系统** ✅
  - 单击/双击/长按/横拖/竖拖/缩放
  - 控制栏显示/锁定/自动隐藏
- **M2 播放器 UI 完整布局** ✅
  - Top/Middle/Bottom/Overlay/Feedback
  - 自定义进度条、反馈层
- **M3 持久化与播放恢复** ✅
  - 位置/速度/音轨/字幕/缩放写回
  - 队列策略（显式队列 / ClipData）
  - PIP + 后台播放基础接入
- **M4 质量与打磨** ✅
  - 单元测试通过
  - JVM 测试与 Lint 基线通过
  - 文档与检查清单完善

## 当前架构基线（2026-03）

- 启动编排：
  - `AsukaAppGraph` 负责应用级依赖装配
  - `PlaybackLaunchCoordinator` 负责 URI 解析、seek fallback、`ClipData` 队列转发和运行时设置组装
- 播放会话：
  - `PlaybackSessionCoordinator` 负责把启动 intent、`MediaController` 和恢复逻辑串起来
  - `PlaybackSessionPlanner` 负责队列、续播位置、速度和轨道恢复策略
- 状态持久化：
  - `PlaybackStateWriter` 负责写回
  - `PlaybackStateRepository` / `QueueHistoryRepository` 负责 typed 读取
- 生命周期策略：
  - `BackgroundPlaybackPolicy` 统一决定后台、PiP 和手动后台播放下是否保留会话连接

## 测试结果摘要
- `./gradlew test` ✅
- `./gradlew :player-ui:compileDebugAndroidTestKotlin` ✅
- `./gradlew lintDebug`
  - 作为补充质量检查保留
- `:player-ui:connectedAndroidTest`
  - 需要连接设备或模拟器，不属于当前无设备默认基线

## 待办清单（建议）

### 1) 系统媒体控制完善（高优）
- 系统中断、耳机事件、音频焦点处理继续完善
- 通知控制项与系统媒体面板体验继续打磨

### 2) 仪器测试补强（高优）
- 外部 `ACTION_VIEW` + `ClipData` 多文件播放
- PiP、后台播放、通知链路
- 保持 `:player-ui:compileDebugAndroidTestKotlin` 作为 PR 预检，避免仪器测试源码再次失编

### 3) 队列策略与媒体扫描（中优）
- 目录扫描/排序策略
- 最近播放列表持久化

### 4) UI/交互打磨（中优）
- 更精细的手势冲突处理
- 动效与反馈视觉优化

### 5) 发布流程完善（低优）
- 版本号与发布说明
- CI 质量门禁（测试+Lint）

# M4 测试计划（初版）

## 1. 目标
- 覆盖核心算法（手势/队列/索引编码）
- 验证启动链路、会话规划与后台保活策略
- 确保播放恢复、轨道选择与字幕关闭状态稳定

## 2. 单元测试
- `GestureAlgorithmsTest`
  - seek 位置 clamping
  - 竖向滑动增量
  - 缩放边界
- `TrackIndexCodecTest`
  - 编码/解码一致性
- `QueuePlannerTest`
  - 当前项优先 + 邻居排序
  - 历史合并去重
- `PlaybackLaunchCoordinatorTest`
  - 外部 `VIEW` 启动时保留并转发 `ClipData`
  - seek fallback 后当前项 URI 与队列项保持一致
- `PlaybackSessionPlannerTest`
  - 队列规划与历史合并
  - 续播位置、速度与音轨/字幕恢复策略
- `TrackSelectionRestoreControllerTest`
  - 轨道信息 ready 后再应用恢复
- `SelectionStateResolverTest`
  - 字幕关闭态正确映射到 UI 选中项
- `BackgroundPlaybackPolicyTest`
  - 保后台连接、自动后台播放、PiP 与手动后台播放的留存判断

## 3. 交互测试建议（手工/后续 UI Test）
- 控制栏显示/隐藏与锁定
- 双击快进/快退
- 横向拖动进度
- 竖向拖动音量/亮度
- 缩放/平移
- 覆盖层（音轨/字幕/倍速/比例）

## 4. 回归清单
- 外部 `ACTION_VIEW` + `ClipData` 多文件启动
- 恢复播放位置
- 恢复速度
- 恢复音轨/字幕
- 字幕关闭状态回显
- 进入/退出 PIP
- 后台播放保持

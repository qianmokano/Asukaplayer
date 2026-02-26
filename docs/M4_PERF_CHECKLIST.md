# M4 性能/稳定性检查清单

## 性能
- 缩放/平移时帧率是否稳定
- 横向拖动 seek 是否造成卡顿
- 频繁切换媒体是否造成资源泄漏

## 稳定性
- 进入/退出 PIP 不崩溃
- 后台播放结束后能正确恢复前台
- 错误弹窗是否能安全重试/跳过

## 资源释放
- Activity onStop 后 MediaController 是否释放
- Service 退出后是否释放 ExoPlayer

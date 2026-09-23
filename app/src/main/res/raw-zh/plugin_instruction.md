AI Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

当前安装版仍只展示宿主状态. P1 宿主接口及 P2.1-P2.4 工具, 决策, 运行器, 上下文和模型客户端核心已实现并通过测试. 实际任务执行还需 P2.5 Binder/前台服务接入和 P3/P4 执行适配器. 脚本 API 与任务台分别在 P5/P6 落地. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方法

1. 在安装了 AutoJs6 构建 5285 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `AI Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.
3. 从启动器或 AutoJs6 抽屉项的管理入口打开 AI Agent: 本预览版只显示宿主状态. 任务台和 `ai.agent` API 随后续阶段提供.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

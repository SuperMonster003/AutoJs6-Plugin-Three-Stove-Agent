AI Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

版本 1.0.0 是路线图的 P0 开发预览: 插件身份, AutoJs6 发现契约 (INFO 服务, Wake Activity 与 `org.autojs.plugin.AI_AGENT` 服务占位) 以及一个显示宿主状态的启动页. 智能体循环, 脚本目录, `ai.agent` API 与任务台尚未实现; 进度与证据记录在 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). 插件将要求 AutoJs6 构建 5283 或更高版本.

### 使用方法

1. 在安装了 AutoJs6 构建 5283 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `AI Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.
3. 从启动器打开 AI Agent: 本预览版的页面只显示是否安装了兼容的 AutoJs6 宿主. 任务台, 抽屉项与 `ai.agent` API 随后续路线图阶段提供.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

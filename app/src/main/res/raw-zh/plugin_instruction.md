AI Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

开发预览: 任务台已支持目标输入, 进度, 内联回答与最近任务详情. ai.agent 任务 API 需要 AutoJs6 build 5293 或更高版本. 历史管理, 自定义预设与其他入口继续按 P6 实施, 健壮性验收仍在 P7. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方法

1. 在安装了 AutoJs6 构建 5289 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `AI Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.
3. 打开 AI Agent 并连接 AutoJs6, 输入目标, 选择默认预设后开始. 在任务卡片中回答询问或确认操作, 点击最近任务查看详情.
4. 在启动器的 "脚本目录" 中配置附加目录, 每行一个绝对路径. 保存后由宿主校验并应用; 任务只能缩小已批准的目录范围.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

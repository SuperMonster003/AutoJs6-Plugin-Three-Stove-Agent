AI Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

开发预览: P6.1 任务台与 P6.2 任务历史已可用. ai.agent API 需要 AutoJs6 build 5293 或更高版本. 自定义预设及其他界面继续按 P6.3-P6.7 实施, 健壮性与发布门槛仍在 P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 使用方法

1. 在安装了 AutoJs6 构建 5289 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `AI Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.
3. 打开 AI Agent 并连接 AutoJs6, 输入目标, 选择默认预设后开始. 在任务卡片中回答询问或确认操作, 点击最近任务查看详情.
4. 在启动器的 "脚本目录" 中配置附加目录, 每行一个绝对路径. 保存后由宿主校验并应用; 任务只能缩小已批准的目录范围.
5. 最多 200 条任务 / 32 MiB. 优先清理最久未查看的已结束任务. 重跑会把原目标和预设填入任务台, 核对后点击开始任务再次执行. 清空历史会保留运行中的任务. 导出保留诊断计数, 工具名称和确认结果. 目标, 参数, 观察内容及脚本结果会移除. 请选择文件保存位置.

连接指南与当前进度请参阅 [项目 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) 与 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

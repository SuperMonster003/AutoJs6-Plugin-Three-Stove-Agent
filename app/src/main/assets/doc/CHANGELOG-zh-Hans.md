******

### 发行历史

******

# v1.0.0

###### 2026/09/23

* `提示` P0 开发预览: 插件身份, AutoJs6 发现契约与显示宿主状态的启动页. 智能体循环, 脚本目录, ai.agent API 与任务台尚未实现. 详见 ROADMAP.md.
* `提示` 宿主的 AI Agent 契约, 能力与模型代理, 屏幕观察, 脚本登记执行, 抽屉与插件中心入口已实现; 插件任务执行仍在开发中
* `新增` 插件身份 `ai-agent`, 含 INFO 服务, Wake Activity, 运行在 `:agent` 进程的 `org.autojs.plugin.AI_AGENT` 服务占位, 以及显示是否安装了兼容 AutoJs6 宿主的启动页
* `新增` 10 语言的 README, 插件中心说明与更新日志
* `新增` Agent 核心工具目录, 含 30 个工具, 分组准入, 参数 Schema, bridge 调用准备, 有界观察与敏感风险提升; 运行时接入随后续阶段提供
* `优化` 最低宿主要求确定为 AutoJs6 6.8.0 / 构建 5285, 与宿主 P1 接口及入口交付版本一致
* `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为共享插件契约, 以 SHA-256 锁定于 `locks/host-api-aars.lock`
* `依赖` 附加 Gson 版本 2.13.2, 用于有界严格 JSON 解析与 Schema 数据树

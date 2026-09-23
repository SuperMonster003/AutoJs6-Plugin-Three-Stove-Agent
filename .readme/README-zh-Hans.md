<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>按自然语言目标在 AutoJs6 中选择已登记脚本并逐步操作界面完成任务</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### 简介

******

AI Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

插件既是 AutoJs6 插件, 也是独立应用. 脚本通过 AutoJs6 的 `ai.agent` API 使用它; 用户通过它自己的任务台, AutoJs6 抽屉项, 悬浮球, 系统分享面板, 应用快捷方式和语音输入使用它. 模型调用与设备操作始终经 Binder 交给 AutoJs6: 宿主借给插件一个模型代理 (宿主已知的 AI Provider 插件, 例如 3-Stone AI) 和一个带有限 grant 的能力代理. 插件从不持有凭据, 从不自行绑定模型提供方, 也不申请无障碍权限.

******

### 当前状态

******

当前安装版仍只展示宿主状态. P1 宿主接口及 P2.1-P2.3 工具, 决策和运行器核心已实现并通过测试. 实际任务执行还需 P2.4/P2.5 模型与宿主接入以及 P3/P4 执行适配器. 脚本 API 与任务台分别在 P5/P6 落地. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### 功能

******

1.0.0 计划提供以下能力:

- 脚本选择: 通过 `project.json` 或 `@agent` 头注释登记的脚本连同描述与参数 Schema 呈现给模型; 智能体挑选脚本, 补全参数, 在需要时请求确认, 在 AutoJs6 中运行并读取结构化结果.
- 界面逐步操作: 智能体以紧凑文本形式观察无障碍节点树 (安装了 OCR 插件时还能读取屏幕文字), 然后经 AutoJs6 能力代理点击, 输入, 滚动与按键, 直到能够校验目标已达成.
- 安全设计: 只读工具自动执行; 敏感操作 (支付, 发送, 删除, 写文件, shell, 坐标手势, 登记为敏感的脚本) 需要确认; 每次任务都有步数, 模型调用次数, 时长与 token 预算.
- 脚本 API 与用户界面: `ai.agent.run(goal, options)` 返回带事件, 回应与取消的 `AgentRun` 句柄; 独立应用提供任务台, 历史, 预设, 偏好记忆, 设置与发行历史.

### 工具目录

Agent 串行运行器与任务队列 (1 个活动任务 + 8 个排队任务), 支持模型/工具取消, 用户交互超时, 唯一终态事件与宿主失联阻塞; 真实宿主接入仍在后续阶段. Agent 确定性上下文装箱, 支持字节上限, 最近完整步骤对, 中英文提示与节点优先保留; 本地模型使用 3000 token 输入预算和紧凑工具签名.

| 工具 | 分组 | 风险 | 默认 | 描述 |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | 按包名或显示名称打开应用. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | 读取剪贴板文字. |
| `clipboard_set` | `act` | `NORMAL` | `on` | 替换剪贴板文字. |
| `ui_click` | `act` | `NORMAL` | `on` | 点击一个已观察目标. |
| `ui_long_click` | `act` | `NORMAL` | `on` | 长按一个已观察目标. |
| `ui_press_key` | `act` | `NORMAL` | `on` | 执行 Android 导航或通知面板动作. |
| `ui_scroll` | `act` | `NORMAL` | `on` | 对一个已观察目标执行有界次数的滚动. |
| `ui_set_text` | `act` | `NORMAL` | `on` | 在一个已观察的可编辑目标上设置或追加文字. |
| `files_list` | `files` | `NORMAL` | `off` | 列出工作目录文件. |
| `files_read` | `files` | `NORMAL` | `off` | 读取有界工作目录文件文字. |
| `files_stat` | `files` | `NORMAL` | `off` | 读取工作目录文件信息. |
| `files_write` | `files` | `SENSITIVE` | `off` | 确认后写入工作目录文件. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | 仅在手势组开启并确认后点击坐标. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | 确认后沿有界坐标路径执行手势. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | 确认后在两组坐标间滑动. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | 读取当前作用域可用的偏好记忆. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | 提议由用户确认保存偏好, 不保存凭据. |
| `app_current` | `observe` | `READ_ONLY` | `on` | 读取当前窗口与应用. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | 读取有界控制台尾部, 其中可能包含无关脚本. |
| `device_info` | `observe` | `READ_ONLY` | `on` | 读取设备信息. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | 读取屏幕是否亮起. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | 在选择动作前观察当前无障碍节点树. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | 查找满足全部选择器条件的节点. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | 在时限内等待选择器目标出现或消失. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | 通过宿主 OCR 插件读取屏幕文字. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | 查找明确登记供智能体使用的脚本. |
| `script_run` | `script` | `NORMAL` | `on` | 按 ID 执行登记脚本, 校验参数并采用登记风险. |
| `script_stop` | `script` | `NORMAL` | `on` | 停止所属脚本执行. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | 确认后执行有时限的非 Root shell 命令. |
| `report_progress` | `user` | `READ_ONLY` | `on` | 报告有界进度, 不声明任务已完成. |

******

### 使用方法

******

1. 在安装了 AutoJs6 构建 5285 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `AI Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.
3. 从启动器或 AutoJs6 抽屉项的管理入口打开 AI Agent: 本预览版只显示宿主状态. 任务台和 `ai.agent` API 随后续阶段提供.

> 宿主抽屉已提供连接与管理入口; 当前插件尚不能运行任务. `ai.agent` API 与任务台仍分别属于 P5 和 P6.

******

### 权限与安全

******

插件遵循明确的边界:

- Binder 入口受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 能访问; 启动页是唯一另外导出的组件.
- 插件不持有 API key, 不自行绑定模型提供方, 也不申请无障碍权限: 模型调用与设备操作经 AutoJs6 为单条附着链路借出并在断开时收回的代理执行, 每个代理都受 grant 约束 (允许的方法, 速率, 体积, 模型配额).
- 插件不使用网络. 本预览版除插件权限外不声明任何权限; 前台服务, 通知与悬浮窗权限将随需要它们的功能加入并在此说明.
- 任务历史, 预设与偏好记忆只保存在插件私有存储; 备份与设备迁移已禁用.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5285 (6.8.0)
```

`AiAgentPluginService` 在 `:agent` 进程中响应 `org.autojs.plugin.AI_AGENT` (category `ai-agent`); 本预览版在宿主契约模块落地前只暴露一个携带 descriptor `org.autojs.plugin.ai.agent.api.IAiAgentPlugin` 的占位 Binder. `AiAgentPluginInfoService` 以 PluginInfo 响应 `org.autojs.plugin.INFO`. `WakeActivity` 供宿主激活插件.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.0.0

_2026/09/23_

- `提示` 当前安装版仍只展示宿主状态. P1 宿主接口及 P2.1-P2.3 工具, 决策和运行器核心已实现并通过测试. 实际任务执行还需 P2.4/P2.5 模型与宿主接入以及 P3/P4 执行适配器. 脚本 API 与任务台分别在 P5/P6 落地.
- `提示` 宿主的 AI Agent 契约, 能力与模型代理, 屏幕观察, 脚本登记执行, 抽屉与插件中心入口已实现; 插件任务执行仍在开发中
- `新增` 插件身份 `ai-agent`, 含 INFO 服务, Wake Activity, 运行在 `:agent` 进程的 `org.autojs.plugin.AI_AGENT` 服务占位, 以及显示是否安装了兼容 AutoJs6 宿主的启动页
- `新增` 10 语言的 README, 插件中心说明与更新日志
- `新增` Agent 核心工具目录, 含 30 个工具, 分组准入, 参数 Schema, bridge 调用准备, 有界观察与敏感风险提升; 运行时接入随后续阶段提供
- `新增` Agent 决策核心, 含协议 Schema 变体, 严格及提取式 JSON 解析, 工具/分支校验, 最多两次修复重试与中英文提示模板; 尚未接入任务执行
- `新增` Agent 任务预算, 统一限制步数, 模型调用, 时长和 token, 并提供工具/交互时限, usage 估算及输出 token 准入
- `新增` Agent 确认门, 支持默认/审慎策略, 同任务同工具同风险授权, 支付逐次确认与 10 语言支付关键词
- `新增` Agent 私有步骤日志, 限制为 200 步和 1 MiB, 支持密码文本脱敏, 终态裁剪保留状态和计数
- `新增` Agent 串行运行器与任务队列 (1 个活动任务 + 8 个排队任务), 支持模型/工具取消, 用户交互超时, 唯一终态事件与宿主失联阻塞; 真实宿主接入仍在后续阶段
- `新增` Agent 确定性上下文装箱, 支持字节上限, 最近完整步骤对, 中英文提示与节点优先保留; 本地模型使用 3000 token 输入预算和紧凑工具签名
- `优化` 最低宿主要求确定为 AutoJs6 6.8.0 / 构建 5285, 与宿主 P1 接口及入口交付版本一致
- `依赖` 附加 `common-plugin-api.aar` (AutoJs6 模块 `plugin-api/common-plugin-api`, 宿主构建 6.8.0 / 5282, MPL 2.0) 作为共享插件契约, 以 SHA-256 锁定于 `locks/host-api-aars.lock`
- `依赖` 附加 Gson 版本 2.13.2, 用于有界严格 JSON 解析与 Schema 数据树

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 构建与验证

******

本节面向希望从源码构建插件的开发者; 普通用户直接安装 Releases 页面的预构建 APK 即可.

构建 Debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并构建 instrumentation 测试 APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建 Release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

收集发布产物并在文件名后追加版本与 CRC32 摘要:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档源与生成产物是否同步 (CI 同样执行此检查):

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 或更高版本以及 Android SDK 37; Gradle 与插件版本由 `version.properties` 和 `io.github.supermonster003.autojs6-platform-versions` 统一管理.

******

### 本地化与文档生成

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

`.readme/` 与 `.changelog/` 下的语言 JSON 文件是 README, 插件中心说明与更新日志的唯一文案源. 请始终修改这些 JSON 源文件并重新运行 `py .python/generate_markdown.py`; 生成的 README, `plugin_instruction.md` 与更新日志产物不得手工编辑. 运行 `py .python/generate_markdown.py --check` 可校验全部生成产物.

******

### 许可证

******

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- AutoJs6 讨论 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md

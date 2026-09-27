<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-stove-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>按自然语言目标在 AutoJs6 中选择已登记脚本并逐步操作界面完成任务</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 语言

******

当前 README.md 支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ar.md)

******

### 简介

******

Three Stove Agent 把一句自然语言目标变成运行 AutoJs6 的 Android 设备上的实际操作. 它或者从用户登记给智能体使用的脚本中挑选一个, 补全参数并运行; 或者通过无障碍节点树观察屏幕, 按观察, 决策, 操作, 校验的循环逐步操作, 直到达成目标, 需要用户确认, 或预算用尽. 它回应 [AutoJs6 讨论 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Three Stove Agent 既是独立任务台, 也是通过 ai.agent 调用的 AutoJs6 插件. 内置设备操作和模型调用由宿主代理; 可选 MCP 工具仅连接用户配置的服务器. 不直接绑定模型 Provider, 不申请无障碍权限.

******

### 当前状态

******

1.2.0 开发版本提供可选 MCP 工具, 原生工具调用, 截图观察和动态脚本. AiGoCode gpt-5.6-sol 已通过 P9.2 初始图片与工具结果图片探针. P9.1 JSON/原生路径均已完成 Wi-Fi 开启与状态回读: 测试时临时关闭当前热点的自动连接, 模型经蜂窝网络和 VPN 联网. 默认自动连接时 VPN 跨网络切换后的失败仍未解决. 证据见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).

******

### 功能

******

当前实现提供以下能力:

- 脚本选择: 通过 `project.json` 或 `@agent` 头注释登记的脚本连同描述与参数 Schema 呈现给模型; 智能体挑选脚本, 补全参数, 在需要时请求确认, 在 AutoJs6 中运行并读取结构化结果.
- 界面逐步操作: 智能体以紧凑文本形式观察无障碍节点树 (安装了 OCR 插件时还能读取屏幕文字), 然后经 AutoJs6 能力代理点击, 输入, 滚动与按键, 直到能够校验目标已达成.
- 安全设计: 只读工具自动执行; 敏感操作 (支付, 发送, 删除, 写文件, shell, 坐标手势, 登记为敏感的脚本) 默认需要确认; 每次任务都有步数, 模型调用次数, 时长与 token 预算. 确认可仅允许一次, 也可在本次任务内始终允许; 设置还提供审慎模式与完全访问, 后者跳过确认并有醒目标注.
- 脚本 API 与用户界面: `ai.agent.run(goal, options)` 返回带事件, 回应与取消的 `AgentRun` 句柄; 独立应用提供任务台, 历史, 预设, 偏好记忆, 设置与发行历史.
- 经宿主进行原生工具调用: 目录 Schema, 整批参数校验, 顺序执行, 逐项确认, 工具结果续轮及步骤记录共用已有任务规则
- Android 11+ 经 AutoJs6 截图观察: screen_capture 缩放到最长边 1280, JPEG 质量 70, 配套视觉提示词, 图片 token 准入与原生工具结果图片
- 经 script_run_source 执行生成的 JavaScript: script_dynamic 工具组默认关闭, 每次展示源码摘要及可展开的完整源码, 可允许一次或在本次任务内始终允许; 完全访问跳过此审阅. 支持超时, 取消, 结构化结果及私有源码记录. UTF-8 源码和其 JSON 字符串编码均限 8 KiB.
- 本机或外部 MCP 服务器的选定工具, 按服务器设置风险等级, mcp 工具组默认关闭
- 独立应用基于 Material 3 重新设计: 主页为任务流, 输入栏停靠在键盘上方; 顶栏提供模型胶囊, 历史与菜单 (新建任务, 预设, 记忆, 脚本目录, MCP 服务器, 设置); 仅在未连接 AutoJs6 时显示连接提示; 步骤时间线按序号增量更新; 再次运行与换个模型重试只填入输入栏而不直接开始. 设置分区清晰, 明暗外观一致

### 界面截图

以下为 Android API 37.1 上的真实英文界面, 使用专门的示例任务和预设响应的演示模型. 图片用于展示界面, 不作为真实模型任务成功的证据, 不含私人账户数据. [截图复现说明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/README.md).

| 任务台 | 任务详情 |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/workbench.png?raw=true" alt="任务台" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/detail.png?raw=true" alt="任务详情" width="288" /> |
| 操作确认 | 悬浮任务输入 |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="操作确认" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/floating.png?raw=true" alt="悬浮任务输入" width="288" /> |

******

### 安装

******

1. 在安装了 AutoJs6 构建 5293 或更高版本的设备上, 从 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) 安装插件 APK.
2. 打开 AutoJs6 插件中心, 确认 `Three Stove Agent` 已被识别并启用它. 官方发布包会自动通过签名校验.

安装并启用 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), 在其中配置在线模型或导入受支持的本地模型. 当前宿主模型代理选择 3-Stone AI, 其他 Provider 需要宿主完成接入后才能使用. 在 Three Stove Agent 主页点击模型胶囊选择模型. 仅在未连接 AutoJs6 时, 主页才会显示连接提示.

### 兼容性

支持 Android 7.0+ (API 24). 宿主附着要求 AutoJs6 6.8.0 / build 5289+, 完整任务 API 与本快速开始要求 build 5293+. 请使用包含 Agent 改动的宿主构建. 屏幕操作需要宿主的无障碍服务; Agent 会先通过 AutoJs6 已配置的免打扰方式 (Root, 安全设置或 Shizuku) 自动启动, 仅在失败时提示手动开启. OCR 为可选能力, 需要安装并授权 OCR 插件, 且宿主报告其可用. Three Stove Agent 本身不保存模型凭据, 不提供独立无障碍服务.

### 界面快速开始

打开 Three Stove Agent 并连接 AutoJs6, 输入目标并开始任务. 主页的模型胶囊可选择在线或本地模型, 或选择自动 (优先本地模型, 否则使用第一个可用模型). 模型列表支持搜索, 置顶常用模型和复用最近使用的模型, 标签显示已声明的工具调用与图片输入能力. 任务台与悬浮球的新任务共用这一选择, 不修改预设或正在运行的任务, 预设也不再包含模型. 输入栏中的预设标签用于选择可选的预设. 在任务卡片中回答问题并查看进展.

### 脚本快速开始

连接 Three Stove Agent 并配置模型后, 在 AutoJs6 中运行以下 JavaScript. 询问与确认由插件界面承接. 如需使用已保存的配置, 在选项中加入 `preset: "your-preset-name"`.

```javascript
let run = ai.agent.run('读取 Android 版本, 根据实际观察结果报告.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

请检查 `result.status`: Promise 兑现的结果仍可能是 completed, partial, failed, blocked 或 cancelled. `run.cancel()` 可停止任务. 模型目标, 事件, 预算及脚本承接交互见 [ai.agent API](https://docs.autojs6.com/#ai).

### 登记脚本

将下例保存为 AutoJs6 工作目录或宿主已批准的脚本目录中的 `text-counter.js`. 文件开头的 `@agent` JSDoc 表示主动登记到目录. 向 Agent 提出统计指定文本字符数的需求即可, 必填参数缺失时会先询问.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through Three Stove Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

也可在 `main.js` 旁放置以下 `project.json`, main.js 的代码与上例相同, 读取 `ai.agent.context().parameters` 并调用 `ai.agent.result(...)`. 项目登记内容放在 `agent` 对象中.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

参数类型支持 string, number, integer 和 boolean, 不支持嵌套对象或数组. sensitive 脚本执行前需要确认, 选择完全访问时除外. 请仅登记已经审阅的脚本, 风险声明不会为 JavaScript 建立沙箱. [完整登记格式](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### 工具目录

此表由打包的 ToolCatalog 生成. 实际屏幕目标可能提高风险等级, 审慎模式还会确认所有非只读操作, 完全访问则对已启用工具免确认. 设置, 预设, 任务选项与宿主授权共同限制可用工具组.

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
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | 文本节点不足时为所选视觉模型捕获已解锁屏幕. 返回缩放后的 JPEG 观察, 不可直接作为设备坐标. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | 读取屏幕是否亮起. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | 在选择动作前观察当前无障碍节点树. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | 查找满足全部选择器条件的节点. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | 在时限内等待选择器目标出现或消失. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | 通过宿主 OCR 插件读取屏幕文字. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | 查找明确登记供智能体使用的脚本. |
| `script_run` | `script` | `NORMAL` | `on` | 按 ID 执行登记脚本, 校验参数并采用登记风险. |
| `script_stop` | `script` | `NORMAL` | `on` | 停止所属脚本执行. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | 逐次确认源码后以宿主脚本权限运行生成的 Rhino JavaScript. 无沙箱隔离. 源码含 JSON 转义最多 8192 UTF-8 字节. 使用 ai.agent.result(value) 返回结果. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | 确认后执行有时限的非 Root shell 命令. |
| `report_progress` | `user` | `READ_ONLY` | `on` | 报告有界进度, 不声明任务已完成. |

### 预设与记忆

从任务台打开 "预设" 保存任务配置. 名称是脚本与记忆的固定标识, 换名请复制预设. 内置 default 可编辑但不能删除. 预设不包含模型; 早期版本保存在预设中的模型只对脚本保留. 任务选项只能进一步收紧预设限制. 固定上下文与任务上下文合计最多 8 KiB. 记忆范围可选全局及当前预设, 仅其中一种或关闭. 编辑或删除预设不改变已入队任务. 私有存储最多 32 个预设 / 1 MiB.

打开 "记忆" 查看, 编辑, 删除或备份偏好. 最多 500 条 / 256 KiB, 保留作用域, 来源任务和时间信息. memory_propose 与导入的每条记忆均须单独确认. 未知预设作用域须先创建对应预设. 自动注入允许范围内最新的完整条目, 最多 4 KiB; 当前预设的同名 key 覆盖全局值. memory: false 仅关闭自动注入; 同时禁止查询和提议请关闭 memory 工具组或选择无记忆作用域. 导出包含真实值及来源信息. 请勿保存凭据, 可识别的凭据键名和令牌格式会被拒绝.

### 使用方法

- 在启动器的 "脚本目录" 中配置附加目录, 每行一个绝对路径. 保存后由宿主校验并应用; 任务只能缩小已批准的目录范围.
- 最多 200 条任务 / 32 MiB. 优先清理最久未查看的已结束任务. 重跑会把原目标和预设填入任务台, 核对后点击开始任务再次执行. 清空历史会保留运行中的任务. 导出保留诊断计数, 工具名称和确认结果. 目标, 参数, 观察内容及脚本结果会移除. 请选择文件保存位置.
- 前台在任务台回答, 后台从高优先级通知打开对应请求. 确认页显示工具, 参数, 风险及剩余时间. 当前会话始终允许在本次任务结束前放行同一工具的同级风险操作, 也适用于后续记忆提议或生成源码; 付款需单独授权. "记住此答案" 在允许的记忆作用域内生成单独的 memory_propose 供审阅. 确认通常等待 120 秒, 询问最多 10 分钟, 均受任务预算限制. 超时返回 USER_TIMEOUT, 由模型决定再次询问或报告部分完成. 旧请求无法回答新请求. 后台提醒受通知权限和频道设置影响.
- 从任务台打开 "设置", 选择工具组, 预算, 操作权限 (标准, 审慎或完全访问), 语音输入和默认预设. 每项修改即时保存, 对新任务生效. 完全访问让已启用的工具 (含付款) 免确认执行, 开启期间任务台, 悬浮球和历史详情会显示警示标注. gesture/files/shell/script_dynamic 初始关闭, OCR 还需宿主提供可用且授权的插件. 任务限制设为自动时沿用初始默认值, 时长以分钟填写, 设置值受协议硬上限约束, 预设与单次参数只能继续收紧. 数据管理显示条数与字节占用, 按类别清除须确认且不能有运行中任务; 清除预设后恢复内置 default. 预设, 记忆, 脚本目录与 MCP 服务器也可从设置进入.
- 从主页右上角菜单打开设置. 语言, 暗色模式与主题色可跟随 AutoJs6 或独立配置, 语言与暗色模式也可跟随系统. 版本历史和法律声明内置, 可离线阅读. 手动 GitHub 更新检查缓存成功结果 24 小时. 自动检查默认关闭, 开启后仅在应用使用期间每 12 小时最多尝试一次, 失败或遇到已忽略版本时保持安静, 不自动下载 APK. 管理已忽略更新可逐项恢复版本提醒. 关于页面展示版本, 开发者, 源代码, 许可证与第三方声明.
- 在设置中开启悬浮球, 并授权显示在其他应用上层. 默认关闭, 仅在 AutoJs6 已连接时显示, 锁屏或断开时隐藏, 空闲时不维持前台服务. 可拖动调整位置, 点击输入目标并选择预设, 查看询问或确认, 停止任务. 收起卡片后恢复后台确认通知. 可将纯文本分享到 Three Stove Agent, 使用新建任务快捷方式, 或在预设页将预设及可选固定目标固定到桌面. 所有入口先显示可编辑草稿, 点击开始任务才执行. 预设已删除时不静默回退. 语音使用跟随界面语言的系统识别器, 不可用时隐藏, 结果只回填不自动发送.

### 常见问题

**为什么需要 AutoJs6?**

插件负责任务循环与界面, AutoJs6 负责模型访问, 无障碍操作和登记脚本执行. 未连接兼容宿主时可以查看历史, 无法启动新的设备任务. 宿主断开会阻塞活动任务, 重新连接不会自动重放任务.

**付款什么时候需要确认?**

付款是独立的敏感操作. 批准下单, 脚本或其他操作不等于批准付款. 默认情况下每次识别到的付款动作都需要单独确认, 超时视为拒绝. 在付款确认上选择当前会话始终允许, 只会在本次任务内放行该工具的后续付款. 完全访问会跳过付款确认, 请仅在信任目标和模型时开启. 批准前请核对商家, 商品, 地址和金额.

**本地模型有哪些局限?**

模型能加载不代表任务能成功. 已记录的 Gemma 4 E2B IT Wi-Fi 决策校验用例未通过, 该目标保留 JSON 路径. 原生工具调用也需要兼容宿主及目标, 并保留参数, 确认和预算检查. 请从小任务开始, 检查 partial/failed 结果. 图片输入要求支持图片的目标, AiGoCode gpt-5.6-sol 已通过初始图片与工具结果图片探针, 其他目标须单独验证. 生成脚本须显式启用, 每份源码均按确认策略处理.

******

### 权限与安全

******

插件遵循明确的边界:

- 权限清单: org.autojs.permission.PLUGIN (宿主契约入口), FOREGROUND_SERVICE 与 FOREGROUND_SERVICE_SPECIAL_USE (任务运行期间的前台服务), POST_NOTIFICATIONS (后台确认与进度通知), INTERNET (手动或自动检查 GitHub 发行版本, 以及连接用户配置的 MCP 服务器), ACCESS_LOCAL_NETWORK (Android 17+ 仅从 MCP 设置主动申请), SYSTEM_ALERT_WINDOW (仅在设置中开启悬浮球时申请). 不申请无障碍, 存储或麦克风权限, 模型流量不经过插件.
- Binder 契约入口受 org.autojs.permission.PLUGIN 签名权限保护. 启动器 (也用于快捷方式) 和 text/plain ACTION_SEND 分享目标为公开入口, 只接受有界的目标/预设草稿. 外部 Intent 不能执行任务, 提交确认或改变授权. 设置, 语音结果与任务控制入口均不导出.
- Three Stove Agent 既是独立任务台, 也是通过 ai.agent 调用的 AutoJs6 插件. 内置设备操作和模型调用由宿主代理; 可选 MCP 工具仅连接用户配置的服务器. 不直接绑定模型 Provider, 不申请无障碍权限.
- 模型凭据仍由模型 Provider 保管, 模型调用经 AutoJs6. MCP Bearer 令牌使用 Android Keystore 加密后存于私有目录, 不进入提示词或历史导出. INTERNET 也用于连接已配置的 MCP 服务器; Android 17+ 本地网络权限仅从 MCP 设置主动申请. 远端工具使用用户为服务器指定的风险等级, 初始为 SENSITIVE. 取消不回滚远端操作, 调用失败不自动重放.
- 任务历史, 预设与偏好记忆只保存在插件私有存储; 备份与设备迁移已禁用.
- 截图经 AutoJs6 发送到所选模型, 该模型可能在线运行. 截图要求屏幕已解锁且处于唤醒状态. 步骤历史只保存尺寸和字节数等元数据, 不保存图片内容. JSON 决策保留当前图片, 直到其他观察或用户回答替换它; 原生会话在每批和会话限额内保留已有图片, 每轮重新预留相应 token.
- 生成的脚本以 AutoJs6 权限运行, 不受 JavaScript 沙箱隔离, 可执行已启用工具组之外的操作. 完整源码保存在私有步骤中, 仍遵守既有密码脱敏及历史保留规则. 后续密码脱敏改变的源码无法作为原始脚本保存. 分享 .js 前请检查内容.
- 完全访问只能在插件私有设置中开启, 模型输出, 屏幕内容, 脚本请求与外部 Intent 都无法开启或扩大它. 它对已启用的工具 (含付款) 免确认, 但不开启额外工具组, 也不放宽预算与宿主授权. 无障碍由宿主按 AutoJs6 已配置的方式启动, 插件本身仍不申请无障碍权限.

请只从官方 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) 页面或 AutoJs6 插件中心获取插件. 来源不明的安装包即使版本号相同, 也可能无法通过宿主校验或带来风险.

******

### 插件接口

******

以下信息面向 AutoJs6 宿主与插件开发者; 宿主使用这些标识发现插件并协商兼容性:

```text
application id: io.github.supermonster003.autojs6.plugin.three.stove.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5289 (6.8.0)
```

`ThreeStoveAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: 经身份校验的宿主连接, 支持任务排队, 应答, 取消, 查询与私有步骤记录; 宿主断开时任务阻塞, 进程重建后不会自动续跑.

******

### 路线图

******

插件的规划与进度以可勾选清单的形式维护在 ROADMAP.md 中, 按阶段组织并附有验收条件与证据等级. 未勾选条目表达的是意图而非当前能力; 欢迎通过 Issues 讨论.

- [查看 ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### 发行历史

******

#### v1.2.0

_2026/09/27_

- `提示` 应用已更名为 Three Stove Agent: 应用 ID 改为 io.github.supermonster003.autojs6.plugin.three.stove.agent, 仓库改为 AutoJs6-Plugin-Three-Stove-Agent. 不兼容旧名称: 请先卸载旧的 AI Agent 再安装, 历史, 预设与记忆不会迁移. 插件 ID ai-agent, 服务 action 与 AIDL 包名由 AutoJs6 契约 AAR 决定, 将随宿主更名并换锁后一并替换.
- `提示` 1.2.0 开发版本提供可选 MCP 工具, 原生工具调用, 截图观察和动态脚本. AiGoCode gpt-5.6-sol 已通过 P9.2 初始图片与工具结果图片探针. P9.1 JSON/原生路径均已完成 Wi-Fi 开启与状态回读: 测试时临时关闭当前热点的自动连接, 模型经蜂窝网络和 VPN 联网. 默认自动连接时 VPN 跨网络切换后的失败仍未解决. 证据见 ROADMAP.md.
- `新增` 本机或外部 MCP 服务器的选定工具, 按服务器设置风险等级, mcp 工具组默认关闭
- `新增` 打开 Three Stove Agent 并连接 AutoJs6, 输入目标并开始任务. 主页的模型胶囊可选择在线或本地模型, 或选择自动 (优先本地模型, 否则使用第一个可用模型). 模型列表支持搜索, 置顶常用模型和复用最近使用的模型, 标签显示已声明的工具调用与图片输入能力. 任务台与悬浮球的新任务共用这一选择, 不修改预设或正在运行的任务, 预设也不再包含模型. 输入栏中的预设标签用于选择可选的预设. 在任务卡片中回答问题并查看进展.
- `新增` 从主页右上角菜单打开设置. 外观, 操作权限, 工具组, 任务限制 (时长以分钟计), 语音输入, 悬浮球与数据清理均即时生效, 无需保存按钮. 语言, 暗色模式与主题色可跟随 AutoJs6 或独立配置, 语言与暗色模式也可跟随系统. 版本历史和法律声明内置, 可离线阅读. 手动 GitHub 更新检查缓存成功结果 24 小时. 自动检查默认关闭, 开启后仅在应用使用期间每 12 小时最多尝试一次, 失败或遇到已忽略版本时保持安静, 不自动下载 APK. 管理已忽略更新可逐项恢复版本提醒. 关于页面展示版本, 开发者, 源代码, 许可证与第三方声明.
- `新增` 界面任务需要无障碍时, 先使用 AutoJs6 中已配置的免打扰启动方式 (Root, 安全设置或 Shizuku). 仅在自动启动失败或未配置时, 任务卡片才提示手动开启并提供无障碍设置入口.
- `新增` 设置中的操作权限新增完全访问: 已启用的工具 (含付款, 删除, 脚本与记忆写入) 免确认执行. 它不会开启额外工具组, 也不放宽预算或宿主权限. 任务台, 悬浮球, 当前任务与历史详情以醒目文字标注, 不弹出打扰对话框. 显式要求审慎确认的任务仍按审慎模式运行.
- `新增` 确认卡片新增当前会话始终允许: 本次任务结束前, 同一工具的同级风险操作不再重复确认, 参数变化也适用. 付款操作需单独授权; 动态脚本与记忆提议也可按会话允许.
- `修复` 记忆列表中较长的范围名称不再把条目键挤出行外: 范围徽标单行省略并保留完整名称的无障碍描述 (API 24 / 360 dp 宽度下的 CI 布局审计暴露)
- `修复` 内部异常导致任务失败时, 步骤记录保留异常类名 (不含消息) 以便诊断; 工具超时终止时结果注明工具时长上限维度; MCP 工具发现限时 8 秒, 不再挤占 15 秒的任务准备窗口
- `修复` 插件能力声明补齐 native-tools 与 vision, 运行上限常量直接绑定宿主契约, MCP 客户端版本号取自安装包信息, 分散的超时与体积字面量统一引用契约常量
- `修复` AutoJs6 无障碍服务停止时, 模型收到 A11Y_SERVICE_NOT_RUNNING, 而不是参数错误
- `优化` 启动器图标改为维护者提供的 Three Stove 图案: 亮色模式为浅灰底深色图案, 暗色模式为深灰底浅色图案, 圆形与自适应图标由同一源图合成
- `优化` 悬浮球步骤标签按可截断角色提供完整文本给读屏器, 模型置顶已满的提示改为页内提示条, 启动器声明圆形图标; 界面工具集移除未使用的成员并统一正文与说明文本构建
- `优化` 工具目录以 confirmAlways 属性声明记忆提议与生成脚本的强制确认, 内置工具名统一经 ToolNames 常量引用并由快照测试与目录对齐
- `优化` 模型凭据仍由模型 Provider 保管, 模型调用经 AutoJs6. MCP Bearer 令牌使用 Android Keystore 加密后存于私有目录, 不进入提示词或历史导出. INTERNET 也用于连接已配置的 MCP 服务器; Android 17+ 本地网络权限仅从 MCP 设置主动申请. 远端工具使用用户为服务器指定的风险等级, 初始为 SENSITIVE. 取消不回滚远端操作, 调用失败不自动重放.
- `优化` 独立应用基于 Material 3 重新设计: 主页为任务流, 输入栏停靠在键盘上方; 顶栏提供模型胶囊, 历史与菜单 (新建任务, 预设, 记忆, 脚本目录, MCP 服务器, 设置); 仅在未连接 AutoJs6 时显示连接提示; 步骤时间线按序号增量更新; 再次运行与换个模型重试只填入输入栏而不直接开始. 设置分区清晰, 明暗外观一致
- `优化` 确认卡片以可读表格展示风险等级, 工具组和全部参数, 不再显示原始 JSON; 允许一次, 当前会话始终允许与拒绝三个操作清晰区分. 悬浮球采用相同的 Material 设计, 预设在卡片内直接选择, 模型一行可打开共用的模型切换器
- `优化` 任务历史新增搜索, 状态标签, 预设与日期范围筛选, 可从菜单清除已结束的任务. 任务详情显示所用模型, 带参数表格与可展开观察内容的步骤时间线, 提供再次运行与换个模型重试, 菜单中可导出诊断, 删除记录或将该任务的模型用于新任务
- `优化` 预设, 记忆, MCP 服务器与脚本目录采用统一设计: 预设以卡片呈现并提供行菜单, 编辑器为整页 (时长以分钟计, 保存按钮固定在底部); 记忆支持搜索与作用域标签; MCP 提供启用开关, 风险选择与工具清单; 离开未保存的修改前会先确认
- `依赖` 升级三份宿主 API release 制品至 AutoJs6 3cdf7de13c / build 5297 (P10 的 mcp 工具组选项与 TOOL_FAILED 常量), 基础契约仍为 V1
- `依赖` 附加 AndroidX AppCompat 1.7.1 与 Material Components for Android 1.13.0 及其 AndroidX 运行时依赖, 用于 Material 3 界面

#### v1.1.0

_2026/09/26_

- `提示` 1.1.0 未单独发布, 其全部内容随 1.2.0 一并发布
- `提示` 原生调用需要 AutoJs6 build 5297+ 和具备 tools 能力的目标, 如 3-Stone AI 1.2.0 开发候选的在线目标. 旧宿主和不支持的目标保留 JSON 决策. 每个原生会话保留初始超时, 上下文/输出上限及最多 16 个工具轮次; 工具执行后发生错误不会改走 JSON 重启
- `提示` 图片输入要求兼容宿主, observe 工具组和显式启用图片输入的视觉模型. 实现与确定性测试已完成, 真实在线视觉验收仍待补测. 旧系统和纯文本目标继续使用文本观察. 见 ROADMAP.md
- `提示` 生成的脚本以 AutoJs6 权限运行, 不受 JavaScript 沙箱隔离, 可执行已启用工具组之外的操作. 完整源码保存在私有步骤中, 仍遵守既有密码脱敏及历史保留规则. 后续密码脱敏改变的源码无法作为原始脚本保存. 分享 .js 前请检查内容.
- `新增` 经宿主进行原生工具调用: 目录 Schema, 整批参数校验, 顺序执行, 逐项确认, 工具结果续轮及步骤记录共用已有任务规则
- `新增` Android 11+ 经 AutoJs6 截图观察: screen_capture 缩放到最长边 1280, JPEG 质量 70, 配套视觉提示词, 图片 token 准入与原生工具结果图片
- `新增` 经 script_run_source 执行生成的 JavaScript: script_dynamic 工具组默认关闭, 每次均展示源码摘要及可展开的完整源码并逐次确认. 支持超时, 取消, 结构化结果及私有源码记录. UTF-8 源码和其 JSON 字符串编码均限 8 KiB.
- `依赖` 升级三份宿主 API release 制品至 AutoJs6 52ce694f92 / build 5297, 支持协商图片输入, 保留 build 5289+ 的基础连接契约

#### v1.0.0

_2026/09/25_

- `提示` 1.0.0 提供自然语言任务, 已登记脚本调用与分级确认的设备操作. 已通过用例, 模型限制与待补设备验收见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md). 原生工具调用, 视觉输入与动态脚本生成计划于 1.1.0 支持.
- `提示` 要求 Android 7+, AutoJs6 6.8.0 / build 5293+ 以使用任务 API, 并启用已配置模型的 3-Stone AI 插件. OCR 为可选项. 仅附着协议的最低宿主为 build 5289+.
- `提示` 兼容提示: AutoJs6 build 5297 的原生工具代理扩展与本版本兼容. 3-Stone AI 1.2.0 开发候选版已实现在线三协议工具续轮; 本 Agent 版本仍使用结构化 JSON 决策, 原生循环接入与实测对比见 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).
- `新增` 自然语言任务台支持内联询问, 进度, 停止和结果, 提供可选悬浮输入, 文本分享, 预设快捷方式与语音草稿
- `新增` ai.agent 脚本 API 支持创建任务, 事件, 查询, 回应与取消, 包括 detached 任务及登记脚本的结果/上下文访问
- `新增` project.json / @agent 登记脚本支持目录搜索, 参数校验与默认值, 缺失值询问, 执行确认, 有界执行及结构化结果
- `新增` 通过文本节点和可选的已授权 OCR 观察屏幕, 按节点引用点击, 输入, 滚动和按键, 并检查画面变化与完成证据
- `新增` 经 AutoJs6 宿主代理访问在线及本地模型, 不保存模型凭据; 已选目标缺失时明确失败, 不静默更换模型
- `新增` 步数, 模型调用次数, 时长与 token 预算, 工具时限, 每步最多两次决策修复重试, 以及重复无效动作保护
- `新增` 命名预设与全局设置支持模型选择, 上下文, 工具组, 预算, 审慎模式, 脚本目录和记忆范围; gesture/files/shell 默认关闭
- `新增` 分范围偏好记忆支持提议/导入逐项确认, 编辑, 删除与 JSON 备份, 上限 500 条 / 256 KiB; 自动注入上限 4 KiB
- `新增` 任务详情和时间线, 筛选, 重跑草稿与脱敏 JSON 导出, 私有历史上限为 200 个任务 / 32 MiB
- `新增` 任务台, 通知和悬浮卡按风险确认操作; 付款与记忆始终逐次批准; 宿主断开会阻塞任务, 进程重启不会自动续跑
- `新增` 十语言设置, 离线发行历史与法律声明; 手动 GitHub 更新检查支持取消, 每日缓存与忽略版本, 不自动下载 APK
- `修复` 模型将剩余预算误判为已用预算而提前结束任务的问题
- `修复` 表单与筛选控件触控区域不足, 选择项与脚本参数列换行, 以及大字体和 Android 7 下悬浮控件布局的问题
- `修复` 偏好记忆内容中的全角, 零宽字符及部分凭据名称可绕过凭据校验的问题
- `修复` 无安全锁设备唤醒时屏幕状态尚未稳定, 导致任务悬浮球无法恢复显示的问题
- `修复` 插件进程中断的任务在重启后正确记录为失败; 锁屏观察阻止后续屏幕操作
- `修复` 文件工具在确认或调用宿主前拒绝路径穿越, 绝对路径及非法工作目录路径; 任务历史保留有界的拒绝分类, 不保存被拒绝的模型正文
- `修复` 通知确认先退回目标应用再恢复操作, 页面停止后仍处理确认回执, 悬浮应答提交时收起卡片以释放焦点
- `修复` Android 13 上启动应用时, 提前读取尚未创建窗口的系统栏控制器导致崩溃的问题
- `修复` 最近历史按任务开始时间排序与保留, 避免重启时重写存档导致新任务被旧记录挤掉
- `修复` 脚本与插件界面的询问和确认回应按 interaction 归属校验, 避免脚本替代插件界面回应
- `修复` 收银台的确认交易按钮未识别为支付动作的问题, 现逐次确认且不可复用整轮授权
- `修复` 屏幕外匹配项的空白或倒置边界导致查询误报参数错误的问题, 现保留文字并标记坐标不可用
- `修复` 节点重定位时边界或操作能力不同的嵌套容器被误判为同一目标的问题
- `修复` 节点目标的模型修复提示明确保留 # 引用前缀, 使用 selector 时省略 snapshotId
- `修复` 任务接入预加载订单意图规则, 并减少规则初始化开销
- `修复` 校验区分不同窗口中的相同节点, 剪贴板读取不会解除界面观察要求, 文件传输不再误判为支付任务
- `修复` 动作后的界面回读无响应时, 稳定等待超过截止时间的问题
- `修复` 控制台拆行和裁剪前处理多行参数脱敏, 避免参数文本与凭据标签重名时漏掉凭据
- `修复` 连续启动任务时, 已退出的前台服务不再误拒绝下一任务的启动请求
- `优化` 任务上下文裁剪长历史时复用未变化的提示词与观察片段, 减少每步处理耗时
- `优化` 脚本确认描述按 JSON 转义后的大小限流, 避免大参数表超过 Binder 事件上限
- `优化` 最低宿主版本为 AutoJs6 6.8.0 / 构建 5289, 用于动作节点检查以及确认与执行的绑定
- `依赖` 附加同一 AutoJs6 6.8.0 / 5289 release 构建的 common-plugin-api, host-capability-api 与 ai-agent-api (MPL 2.0), 通过 SHA-256 锁定
- `依赖` 附加 Gson 版本 2.13.2, 用于有界严格 JSON 解析与 Schema 数据树

##### 更多发行历史

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

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

项目代码基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE) 授权. 第三方组件及其许可证列于 [第三方声明](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- AutoJs6 讨论 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md

3-Stove Agent 是在手机上代为执行操作的智能体. 用一句话描述目标后, 它借助 AutoJs6 查看屏幕并逐步点击, 输入与滚动, 或挑选已登记的脚本补全参数后运行, 直到完成目标. 付款, 发送, 删除等敏感操作执行前需要确认, 每个任务都有步数与时长上限.

### 开始使用

1. 安装并启用 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), 在其中添加在线模型或导入本地模型. 3-Stove Agent 通过它调用模型.
2. 打开 3-Stove Agent. 主页提示未连接时, 点按 "连接 AutoJs6".
3. 点按顶栏的模型胶囊选择模型, 或保留 "自动" (优先使用本地模型).
4. 在输入栏填写目标, 点按 "开始任务". 任务卡片显示每一步的进展, 需要回答问题或确认操作时也在此处理.

### 主要功能

- **任务与历史**: 任务进行中可以查看步骤, 回答询问或随时停止. "任务历史" 保留已结束的任务, 可以分享摘要或再次运行 (再次运行只把原目标填入输入栏, 核对后再开始).
- **确认与操作权限**: 只读操作直接执行; 付款, 发送, 删除, 写文件等敏感操作默认先确认, 可以只允许一次或在本次任务内始终允许. 设置中的 "操作权限" 可选 "标准", "审慎" 或 "完全访问" (无需确认, 开启期间有醒目标注).
- **预设**: 把常用的工具组, 预算与固定上下文保存为预设, 开始任务前在输入栏的预设标签中选择. 开启 "计划模式" 后, 任务先给出 3 到 8 步的计划, 经审阅后再执行.
- **记忆**: 保存需要智能体记住的偏好, 任务开始时自动附带. 智能体提议的每条记忆都需要确认后才会保存; 请勿在记忆中保存密码等凭据.
- **登记脚本**: 在脚本开头添加 `@agent` 注释 (示例见下文) 后, 智能体即可挑选并运行该脚本. 工作目录以外的脚本位置在 "脚本目录" 中添加.
- **悬浮球**: 在设置中启用后, 可以在其他应用上方输入目标, 处理确认或停止任务.
- **快速开始**: 把文字分享给 3-Stove Agent, 使用桌面快捷方式 "新建任务", 或把预设固定到桌面. 这些入口只生成草稿, 点按 "开始任务" 后才执行.
- **语音**: 在设置中开启 "启用语音输入" 后, 输入框旁显示麦克风; 安装了 3-Macaw Speech 时直接离线识别. "常驻监听" (默认关闭) 识别到唤醒词 "小炉小炉" 或 "Hey Stove" 后接收一句任务. 语音结果同样只成为草稿.
- **更多工具**: 在设置中可以启用文字识别 (需要 OCR 插件), 其他插件提供的工具 (例如 3-Macaw Speech 的朗读与转写), MCP 服务器提供的工具, 以及由模型生成且经审阅后才运行的脚本.

### 在脚本中使用

在 AutoJs6 脚本中使用 `ai.agent.run` 发起任务, 询问与确认仍在 3-Stove Agent 的界面中处理:

```js
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

- 请检查结果的 `status`: Promise 兑现时, 结果仍可能是 `completed`, `partial`, `failed`, `blocked` 或 `cancelled`. 调用 `run.cancel()` 可以停止任务.
- 在选项中加入 `preset: 'your-preset-name'` 可使用已保存的预设, 加入 `plan: true` 可让任务先给出计划.
- 完整的选项与事件见 [AutoJs6 文档 > ai.agent](https://docs.autojs6.com/#/ai).

如需让智能体运行自定义脚本, 将下例保存为 AutoJs6 工作目录中的 `text-counter.js`. 此后提出统计一段文字字符数的需求即可, 缺少参数时智能体会先询问:

```js
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through 3-Stove Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

### 运行要求

- Android 7.0 或更高版本, AutoJs6 6.8.0 或更高版本.
- 模型由 [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) 提供, 3-Stove Agent 本身不保存模型凭据.
- 屏幕操作使用 AutoJs6 的无障碍服务. AutoJs6 已配置 Root, 安全设置或 Shizuku 时会自动开启, 否则按提示手动开启.
- 通知权限用于后台确认与任务提醒; 悬浮球需要 "显示在其他应用上层" 权限.

### 常见问题

**需要 AutoJs6 的原因**

3-Stove Agent 负责任务与界面, 模型调用, 屏幕操作与脚本运行均经由 AutoJs6 完成. 未连接 AutoJs6 时只能查看历史, 无法开始新任务; 连接断开会使进行中的任务受阻, 重新连接后不会自动重放.

**付款需要确认的情形**

默认每次付款都需要单独确认, 批准下单或其他操作不等于批准付款, 超时视为拒绝. "完全访问" 会跳过付款确认, 请仅在信任目标与模型时开启.

**本地模型较慢或无法完成任务的原因**

模型可以加载不代表能够完成任务, 建议从简单的任务开始并检查结果. 较旧的手机上首次调用可能超过 5 分钟的时限, 等待期间任务台显示已等待的秒数, 超时后结果会写明可能的原因与对策.

**常驻监听停止的原因**

常驻监听不随开机启动, 打开任务台或设置后恢复. ColorOS 或 MIUI 上需要允许自启动并加入电池白名单, 否则常驻服务可能被系统结束; 其他应用录音时也无法识别唤醒词.

工具列表, 脚本登记格式与权限说明见 [项目主页](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent).

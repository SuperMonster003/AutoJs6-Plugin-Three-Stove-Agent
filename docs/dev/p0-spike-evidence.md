# P0.2 结构化 JSON 决策循环 spike 证据

日期: 2026-09-22. 路线图条目: P0.2 (D7 / D35 决策点). 原始脚本, 夹具与结果: `docs/dev/spike/p0/` (已脱敏, 见第 10 节).

## 1. 结论摘要

- 可用目标只有三个: (a) 本地 LiteRT-LM 社区模型两台 (Pad gemma-4-E4B-it, Sony gemma-4-E2B-it), (b) 一个 OpenAI 兼容 profile (`PoloAPI`, 模型 `claude-opus-4-8`, 维护者付费代理). (c) Anthropic 与 (d) Gemini profile 在任何设备上都未配置, 本次未测.
- (b) 在线目标: 20 轮 JSON 合法 20 / 20 (100%), Schema 合规 20 / 20 (100%), 决策合理 (人工复核后) 20 / 20 (100%), 脚本侧耗时中位 6930 ms.
- (a) Pad E4B (CPU): 20 轮 JSON 合法 20 / 20 (100%), Schema 合规 20 / 20 (100%), 决策合理 16 / 20 (80%), 脚本侧耗时中位 167758 ms (约 2.8 分钟 / 步).
- (a) Pad E4B (GPU, 同一脚本重跑): 20 轮, 超时 0, JSON 合法 20 / 20 (100%), Schema 合规 20 / 20 (100%), 决策合理 14 / 20 (70%), 脚本侧耗时中位 19642 ms; GPU 后端把每步延迟从分钟级降到十几秒.
- (a) Sony E2B (GPU): 20 轮, 超时 6, 有应答轮次 JSON 合法 14 / 14 (100%), Schema 合规 13 / 14 (93%), 决策合理 9 / 20 (45%), 有应答轮次脚本侧耗时中位 43111 ms.
- 附录 D 的扁平 Schema (含 `arguments: { type: object }`) 在本地约束解码下可用 (8 个变体全部接受), 不需要改为 JSON 字符串; 字符串变体 (B) 反而给了小模型输出非法内嵌 JSON 的机会 (Sony E2B 出现 1 次).
- 决策点 ("(b) / (c) / (d) 至少两种 Schema 合规率 >= 95% 且本地 JSON 合法率 >= 90%") 因在线目标只有一种而不成立, 按路线图既定规则: 结构化模式的修复重试从 1 次提高到 2 次 (与退化模式统一, 回填 D35). D7 (结构化 JSON 先行) 在现有证据下保留, 不触发 H.2 退路.
- 三个新增事实影响 P2 设计: 本地 LiteRT-LM 的提示词上限为 4096 token (目录申报的 262144 字节不是有效上限), 超限以 `PROVIDER_FAILED` 无细节失败; 三种在线协议对 JSON Schema 的约束互不兼容, 附录 D 原样发送会被 OpenAI 严格模式 / Anthropic / Gemini `responseSchema` 拒绝, `DecisionSchema` 必须按目标协议生成变体; `ai.ask` 只解析出文本, 指标需要 `ai.chat`.

## 2. 环境

| 项 | Xiaomi Pad (968e9f18) | Sony G8441 (BH900ASK9E) |
| --- | --- | --- |
| 机型 / 系统 | 23046RP50C, Android 15 (API 35, HyperOS 2) | Xperia XZ1 Compact, Android 9 (API 28) |
| SoC / 内存 | SM8475, 11.8 GB | Snapdragon 835 (qcom), 3.8 GB |
| 宿主 AutoJs6 | 6.8.0 (5282) | 6.8.0 (5282) |
| 3-Stone AI | 1.1.4 (213) | 1.1.0 (55) |
| 本地目标 | `local:litertlm.0b2a8980...` gemma-4-E4B-it.litertlm, backend cpu (默认) | `local:litertlm.ab7838cd...` gemma-4-E2B-it-litert-lm.litertlm, backend gpu (正式) / cpu (探针) |
| 在线目标 | 无 | `profile:5d99a40b-...` `PoloAPI`, provider `openai-compatible`, model `claude-opus-4-8`, origin `https://poloapi.top`, 默认目标 |
| 目录申报 | capabilities streaming / structured-json / usage / persistent-session; controls 含 `response-json-schema`; limits 262144 / 65536 字节 | 同左 (在线目标无 backendProfiles) |
| 计算器 App | 无 | 无 |

目录探针脚本 `catalog-probe.js` 的输出见 `docs/dev/spike/p0/results/`. 未配置 Anthropic / Gemini profile, 因此 (c) (d) 未测.

## 3. 方法

### 3.1 夹具 (紧凑节点树)

- 真机捕获 (`fixture-dump.js`, 经宿主 `RunIntentActivity` 运行): 桌面, 设置首页, Wi-Fi 页, 两台设备各三张. 格式: `#n<ref> <Class> text=".." desc=".." id=<id> (cx,cy) [l,t,r,b] <flags>`, 只保留可见且有文本 / desc / id / 状态标志的节点, 上限 160 个.
- 合成变体 (`make_fixtures.py`): `launcher-calc` (桌面追加计算器图标), `wifi-toggled` (Pad: 复选框 checked + 网络列表; Sony: 开关 OFF + 空列表提示), `wifi-networks` (Sony 的真实网络列表, 两台共用), `calc-empty` / `calc-result` (合成的 Google 计算器树, 结果页 `12x34` / `408`). 两台设备都没有计算器 App, 用例 (2) 只能合成.
- 二级压缩 (`make_local_fixtures.py`, 后缀 `.local`): 去掉 bounds, 去掉无文本无状态的纯容器行, 上限 70 行, 节点引用保持不变. 仅本地目标使用, 原因见 3.5.
- 脱敏: 提交到仓库的副本把维护者的 SSID, 小米账号数字与一个 App 名替换为占位 (`export_spike.py`), 其余字节不变.

### 3.2 轮次 (每目标 2 遍 x 10 轮 = 20 轮正式, 另加 2 轮预检)

| 轮次 | 用例 | 目标 (zh Pad / en Sony) | 夹具 | 历史 | 自动期望 |
| --- | --- | --- | --- | --- | --- |
| W1 | Wi-Fi | 打开 WLAN / Turn off Wi-Fi | launcher | 无 | `app_launch` settings 或点击设置图标 |
| W2 | Wi-Fi | 同上 | settings-home | 已启动设置 | 点击 WLAN / Network & internet 行 |
| W3 | Wi-Fi | 同上 | wifi | 已进入 Wi-Fi 页 | 点击开关 / 复选框 |
| W4 | Wi-Fi | 同上 | wifi-toggled | 已切换开关 | `done` completed |
| W5 | Wi-Fi | 关闭 WLAN / Turn on Wi-Fi (与当前状态相同) | wifi | 已进入 Wi-Fi 页 | `done` completed (不做多余动作) |
| W6 | Wi-Fi | 连接到 Wi-Fi 网络 (未指明哪个) | wifi-networks | 已进入 Wi-Fi 页 | `ask` |
| W7 | Wi-Fi | 打开 WLAN / Turn off Wi-Fi, 提供已登记脚本 `wifi-set(enabled)` | launcher | 无 | `script_run` wifi-set, enabled 正确 |
| C1 | 计算器 | 用计算器计算 12 乘以 34 | launcher-calc | 无 | `app_launch` 计算器或点击图标 |
| C2 | 计算器 | 同上 | calc-empty | 已启动计算器 | 点击 `1` 或向公式框 `ui_set_text` |
| C3 | 计算器 | 同上 | calc-result | 已输入并得到 408 | `done` completed 且证据含 408 |

### 3.3 请求形态

- 宿主脚本 API `ai.chat(request, options)` (不是 `ai.ask`: 后者的 Promise 只解析出文本字符串, 拿不到 `usage` / `finishReason`; 第一次尝试因此全部记为空). `request.messages` = system (角色, 循环规则, 决策形状, 附录 D.3 要点的英文压缩版) + user (目标, 工具清单 8 项, 已登记脚本, 历史观察, 当前快照, 预算行).
- `options`: `target`, `structuredJson: true`, `responseSchema` (附录 D.1 原样; 变体 A `arguments: { type: object }`, 变体 B `arguments: { type: string }`), `maxTokens` 600 (在线) / 400 (本地), `temperature: 0.2`, `timeout` 120 s (在线) / 300-540 s (本地), Sony 正式轮 `backend: 'gpu'`.
- 预检: 以 W2 各跑 A / B 一次, A 可用则正式轮全部用 A, 否则 B.
- 指标: JSON 合法 = `JSON.parse(text)` 得到对象 (剥围栏 / 取首个 `{...}` 的宽松解析单独计数); Schema 合规 = 顶层键集合, `kind` 枚举, `reasoning` <= 600 字符, `tool` 分支的 `tool` 字符串 + `arguments` 对象 (B: 字符串可解析为对象), `ask` / `done` 的必填键与枚举, 无未知键; 耗时 = 脚本侧 Promise 往返 (wall) 与提供方 `usage.durationMillis`; token = `usage.inputTokens` / `outputTokens`; 决策合理 = 3.2 的自动期望, 不符者逐条人工复核 (复核结论列在各目标的 "不符轮次" 下).

### 3.4 运行方式

`adb push` 脚本与夹具到 `/sdcard/ai-agent-spike/`, `am start RunIntentActivity --es path .../spike.js` 启动, 结果逐轮追加到 `results-<target>.jsonl`, `progress.txt` 记录进度, 全部 `adb pull` 回 `build/tmp/spike/` 后由 `summarize.py` / 本文生成脚本 `build_evidence.py` 汇总.

### 3.5 过程中发现并修正的问题

- `ai.ask` 只解析出文本 (见 3.3), 改用 `ai.chat`.
- 本地目标首轮以一级夹具 (设置首页 8.9 KB) 发送时, Pad 报 `PROVIDER_FAILED`, logcat 显示 LiteRT-LM `Input token ids are too long. Exceeding the maximum number of tokens allowed: 5041 >= 4096`; Sony 同样输入 300 s 超时. 目录申报的 `maximumContextBytes = 262144` 不是本地模型的有效上限, 且脚本侧只能看到 `PROVIDER_FAILED`, 无法区分 "超长" 与其他失败. 改用二级压缩夹具后本地轮次的输入在 900-1800 token.
- Sony E2B 在 CPU 后端每步 100-175 s, 改用 `backend: 'gpu'` 后 20-48 s (第 6 节), 正式轮采用 GPU; GPU 轮次中出现多次 300 s 超时, logcat 无 LiteRT 错误, 同期有 ThermalEngine 降亮度记录 (电池 44.7 C), 判定为设备发热降频或 GPU 后端偶发挂起, 未进一步定位.

## 4. 结果: (b) 在线 OpenAI 兼容 profile (Sony, `PoloAPI` / `claude-opus-4-8`)

| 指标 | 数值 |
| --- | --- |
| 正式轮次 | 20 (预检 2 轮: A=ok, B=ok; 正式采用变体 A) |
| 传输 / 超时错误 | 0 / 20 (0%) (其中超时 0) |
| JSON 合法率 (分母 = 全部轮次) | 20 / 20 (100%) (严格解析 20, 宽松解析 0) |
| JSON 合法率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 全部轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| 决策合理率 (自动期望) | 18 / 20 (90%) |
| 决策合理率 (人工复核后) | 20 / 20 (100%) |
| 脚本侧耗时 ms (均值 / 中位 / 最大) | 7140 / 6930 / 10391 |
| 提供方 durationMillis (均值 / 中位 / 最大) | 6225 / 6041 / 9585 |
| 输入 token (均值 / 中位 / 最大) | 2391 / 2209 / 3673 |
| 输出 token (均值 / 中位 / 最大) | 153 / 121 / 324 |

不符轮次:

- #5 W6: 自动期望不符 (expected ask, got done); 人工复核: 合理 (夹具 (Sony 真机 Wi-Fi 列表) 已含 `Connected` 网络, 模型以此判定目标已满足并给出证据; 自动期望 (`ask`) 过严, 夹具设计应改用无已连接网络的列表); 推理 `The Wi-Fi settings screen shows Wi-Fi is ON and already connected: the network 'HomeRouter_5G' displays summary 'Connected' with full signal. No action needed.`
- #15 W6: 自动期望不符 (expected ask, got done); 人工复核: 合理 (夹具 (Sony 真机 Wi-Fi 列表) 已含 `Connected` 网络, 模型以此判定目标已满足并给出证据; 自动期望 (`ask`) 过严, 夹具设计应改用无已连接网络的列表); 推理 `Wi‑Fi is already enabled and the device is connected: the switch is ON and the first network entry reads 'HomeRouter_5G, Connected, Wi-Fi signal full., Secu'. Goal already satisfied, no action needed.`

说明: 该目标是第三方代理 (`poloapi.top`) 转发的 Claude 模型, 不是 `api.openai.com`; 附录 D Schema 违反 OpenAI 严格模式的三条规则 (第 7 节) 却被接受, 说明代理不做严格模式校验. 代理是否至少执行了 `response_format` 无法判断: 第 8 节的探针在正式轮结束约 20 分钟后运行, 此时该目标对任何请求 (含无结构化的纯文本基线) 都在约 1 s 内返回 `PROVIDER_FAILED` (网络可达, 提供方进程重启后依旧), 脚本侧看不到 HTTP 状态, 推测为代理侧额度或限流, 请维护者在 3-Stone AI 中核对该 profile. 因此本节的 100% 合规不能外推到官方 OpenAI 严格端点.

<details><summary>逐轮记录</summary>

| # | 轮次 | JSON | Schema | 合理 | 耗时 ms | 输入 / 输出 token | 决策 (截断) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | W1 | y | y | y | 5804 | 1378 / 96 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.settings"}}` |
| 1 | W2 | y | y | y | 5901 | 2399 / 85 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n15"}}` |
| 2 | W3 | y | y | y | 6608 | 3671 / 102 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n9"}}` |
| 3 | W4 | y | y | y | 7468 | 1810 / 193 | `{"kind": "done", "done": {"status": "completed", "summary": "Wi-Fi has been turned off.", "evidence": ["#n9 Switch text=\"OFF\" id=switch_wi` |
| 4 | W5 | y | y | y | 8678 | 3671 / 252 | `{"kind": "done", "done": {"status": "completed", "summary": "Wi‑Fi is already turned on; no action was required.", "evidence": ["#n9 Switch ` |
| 5 | W6 | y | y | y (复核) | 9707 | 3673 / 296 | `{"kind": "done", "done": {"status": "completed", "summary": "Device is already connected to the Wi-Fi network HomeRouter_5G (Wi-Fi toggle is` |
| 6 | W7 | y | y | y | 5703 | 1414 / 114 | `{"kind": "tool", "tool": "script_run", "arguments": {"scriptId": "wifi-set", "arguments": {"enabled": false}}}` |
| 7 | C1 | y | y | y | 5409 | 1476 / 80 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n11"}}` |
| 8 | C2 | y | y | y | 7749 | 2032 / 196 | `{"kind": "tool", "tool": "ui_set_text", "arguments": {"nodeRef": "n3", "text": "12*34"}}` |
| 9 | C3 | y | y | y | 8874 | 2387 / 167 | `{"kind": "done", "done": {"status": "completed", "summary": "Computed 12 × 34 on the calculator; the result is 408.", "evidence": ["#n3 Edit` |
| 10 | W1 | y | y | y | 7346 | 1378 / 100 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.settings"}}` |
| 11 | W2 | y | y | y | 7244 | 2399 / 86 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n15"}}` |
| 12 | W3 | y | y | y | 5183 | 3671 / 92 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n9"}}` |
| 13 | W4 | y | y | y | 7323 | 1810 / 207 | `{"kind": "done", "done": {"status": "completed", "summary": "Wi-Fi has been turned off in Settings.", "evidence": ["#n9 Switch text=\"OFF\" ` |
| 14 | W5 | y | y | y | 8469 | 3671 / 216 | `{"kind": "done", "done": {"status": "completed", "summary": "Wi-Fi is already turned on.", "evidence": ["#n9 Switch id=switch_widget text=\"` |
| 15 | W6 | y | y | y (复核) | 10391 | 3673 / 324 | `{"kind": "done", "done": {"status": "completed", "summary": "The device is connected to a Wi-Fi network. Wi-Fi is enabled (switch = ON) and ` |
| 16 | W7 | y | y | y | 6311 | 1414 / 128 | `{"kind": "tool", "tool": "script_run", "arguments": {"scriptId": "wifi-set", "arguments": {"enabled": false}}}` |
| 17 | C1 | y | y | y | 5718 | 1476 / 88 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n11"}}` |
| 18 | C2 | y | y | y | 6616 | 2032 / 108 | `{"kind": "tool", "tool": "ui_set_text", "arguments": {"nodeRef": "n3", "text": "12*34"}}` |
| 19 | C3 | y | y | y | 6311 | 2387 / 147 | `{"kind": "done", "done": {"status": "completed", "summary": "Calculator computed 12 × 34 = 408.", "evidence": ["#n3 EditText text=\"12x34\" ` |

</details>

## 5. 结果: (a) 本地 LiteRT-LM

### 5.1 Pad gemma-4-E4B-it (CPU, 中文目标)

| 指标 | 数值 |
| --- | --- |
| 正式轮次 | 20 (预检 2 轮: A=ok, B=ok; 正式采用变体 A) |
| 传输 / 超时错误 | 0 / 20 (0%) (其中超时 0) |
| JSON 合法率 (分母 = 全部轮次) | 20 / 20 (100%) (严格解析 20, 宽松解析 0) |
| JSON 合法率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 全部轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| 决策合理率 (自动期望) | 16 / 20 (80%) |
| 决策合理率 (人工复核后) | 16 / 20 (80%) |
| 脚本侧耗时 ms (均值 / 中位 / 最大) | 185166 / 167758 / 246473 |
| 提供方 durationMillis (均值 / 中位 / 最大) | 177167 / 159815 / 238489 |
| 输入 token (均值 / 中位 / 最大) | 1364 / 1331 / 1773 |
| 输出 token (均值 / 中位 / 最大) | 72 / 72 / 99 |

不符轮次:

- #4 W5: 自动期望不符 (expected done, got tool); 人工复核: 不合理; 推理 `The goal is to turn off WLAN. The current screen shows the WLAN settings, and there is a checkbox labeled 'WLAN' (n17) which is currently unchecked. I need to click this checkbox to turn it off.`
- #5 W6: 自动期望不符 (expected ask, got tool); 人工复核: 不合理; 推理 `The goal is to connect to a Wi-Fi network. The current screen shows a list of available networks. I should select one of the networks to connect to. I will choose the first available network, 'HomeRouter_5G', which is no`
- #14 W5: 自动期望不符 (expected done, got tool); 人工复核: 不合理; 推理 `The goal is to turn off WLAN. The current screen shows the WLAN settings, and there is a checkbox labeled 'WLAN' (n15) which is unchecked (n17). I need to click the checkbox to turn it off.`
- #15 W6: 自动期望不符 (expected ask, got tool); 人工复核: 不合理; 推理 `The goal is to connect to a Wi-Fi network. The current screen shows a list of available networks. I should select one of the networks to connect to. I will choose the first available network, 'HomeRouter_5G', which is no`

<details><summary>逐轮记录</summary>

| # | 轮次 | JSON | Schema | 合理 | 耗时 ms | 输入 / 输出 token | 决策 (截断) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | W1 | y | y | y | 164453 | 1310 / 56 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n13"}}` |
| 1 | W2 | y | y | y | 231149 | 1772 / 72 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n30"}}` |
| 2 | W3 | y | y | y | 143896 | 1038 / 60 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 3 | W4 | y | y | y | 158297 | 1253 / 66 | `{"kind": "done", "done": {"status": "completed", "summary": "WLAN is already enabled.", "evidence": ["#n17 CheckBox id=checkbox (1658,266) c` |
| 4 | W5 | y | y | n | 151405 | 1038 / 72 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 5 | W6 | y | y | n | 242566 | 1576 / 92 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 6 | W7 | y | y | y | 167903 | 1326 / 64 | `{"kind": "tool", "tool": "script_run", "arguments": {"scriptId": "wifi-set", "arguments": {"enabled": true}}}` |
| 7 | C1 | y | y | y | 167614 | 1353 / 64 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n27"}}` |
| 8 | C2 | y | y | y | 180170 | 1336 / 85 | `{"kind": "tool", "tool": "ui_set_text", "arguments": {"nodeRef": "n3", "text": "12"}}` |
| 9 | C3 | y | y | y | 240031 | 1635 / 88 | `{"kind": "done", "done": {"status": "completed", "summary": "Successfully calculated 12 multiplied by 34.", "evidence": ["#n4 TextView text=` |
| 10 | W1 | y | y | y | 162685 | 1311 / 55 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n13"}}` |
| 11 | W2 | y | y | y | 230688 | 1773 / 71 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n30"}}` |
| 12 | W3 | y | y | y | 151860 | 1039 / 74 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 13 | W4 | y | y | y | 165402 | 1254 / 79 | `{"kind": "done", "done": {"status": "completed", "summary": "WLAN is already enabled.", "evidence": ["#n17 CheckBox id=checkbox (1658,266) c` |
| 14 | W5 | y | y | n | 152202 | 1039 / 75 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 15 | W6 | y | y | n | 242272 | 1577 / 92 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 16 | W7 | y | y | y | 168758 | 1327 / 65 | `{"kind": "tool", "tool": "script_run", "arguments": {"scriptId": "wifi-set", "arguments": {"enabled": true}}}` |
| 17 | C1 | y | y | y | 157721 | 1354 / 47 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n27"}}` |
| 18 | C2 | y | y | y | 177794 | 1337 / 82 | `{"kind": "tool", "tool": "ui_set_text", "arguments": {"nodeRef": "n3", "text": "12"}}` |
| 19 | C3 | y | y | y | 246473 | 1635 / 99 | `{"kind": "done", "done": {"status": "completed", "summary": "The calculator was used to compute 12 multiplied by 34, resulting in 408.", "ev` |

</details>

### 5.2 Pad gemma-4-E4B-it (GPU, 中文目标, 与 5.1 同一脚本与夹具)

| 指标 | 数值 |
| --- | --- |
| 正式轮次 | 20 (预检 2 轮: A=ok, B=ok; 正式采用变体 A) |
| 传输 / 超时错误 | 0 / 20 (0%) (其中超时 0) |
| JSON 合法率 (分母 = 全部轮次) | 20 / 20 (100%) (严格解析 20, 宽松解析 0) |
| JSON 合法率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 全部轮次) | 20 / 20 (100%) |
| Schema 合规率 (分母 = 有应答轮次) | 20 / 20 (100%) |
| 决策合理率 (自动期望) | 14 / 20 (70%) |
| 决策合理率 (人工复核后) | 14 / 20 (70%) |
| 脚本侧耗时 ms (均值 / 中位 / 最大) | 20410 / 19642 / 25863 |
| 提供方 durationMillis (均值 / 中位 / 最大) | 12552 / 11880 / 18082 |
| 输入 token (均值 / 中位 / 最大) | 1364 / 1331 / 1773 |
| 输出 token (均值 / 中位 / 最大) | 66 / 62 / 99 |

不符轮次:

- #4 W5: 自动期望不符 (expected done, got tool); 人工复核: 不合理; 推理 `The goal is to turn off WLAN. In the current screen, there is a checkbox next to 'WLAN' which is unchecked. I should click this checkbox to turn WLAN off.`
- #5 W6: 自动期望不符 (expected ask, got done); 人工复核: 不合理; 推理 `The goal is to connect to a Wi-Fi network. The current screen shows a list of available Wi-Fi networks. I will select the first available network, 'HomeRouter_5G', to attempt the connection.`
- #6 W7: 自动期望不符 (expected script_run, got tool app_launch); 人工复核: 不合理; 推理 `The goal is to open WLAN. I should launch the settings app to access WLAN settings.`
- #14 W5: 自动期望不符 (expected done, got tool); 人工复核: 不合理; 推理 `The goal is to turn off WLAN. The current screen shows WLAN settings, and there is a checkbox labeled 'WLAN' (n15) which is unchecked. I need to click this checkbox to turn it off.`
- #15 W6: 自动期望不符 (expected ask, got tool); 人工复核: 不合理; 推理 `The goal is to connect to a Wi-Fi network. The current screen shows a list of available networks. I should click on one of the available networks to connect. I will choose the first available network, 'HomeRouter_5G', wh`
- #16 W7: 自动期望不符 (expected script_run, got tool app_launch); 人工复核: 不合理; 推理 `The goal is to open WLAN. I should launch the settings app first, as WLAN settings are typically found there.`

<details><summary>逐轮记录</summary>

| # | 轮次 | JSON | Schema | 合理 | 耗时 ms | 输入 / 输出 token | 决策 (截断) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | W1 | y | y | y | 18462 | 1310 / 51 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n13"}}` |
| 1 | W2 | y | y | y | 19390 | 1772 / 54 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n30"}}` |
| 2 | W3 | y | y | y | 18567 | 1038 / 58 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 3 | W4 | y | y | y | 20522 | 1253 / 69 | `{"kind": "done", "done": {"status": "completed", "summary": "WLAN is already enabled.", "evidence": ["#n17 CheckBox id=checkbox (1658,266) c` |
| 4 | W5 | y | y | n | 19100 | 1038 / 62 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 5 | W6 | y | y | n | 25539 | 1576 / 96 | `{"kind": "done", "done": {"status": "completed", "summary": "Selected the first available Wi-Fi network to connect to.", "evidence": ["#n17"` |
| 6 | W7 | y | y | n | 17005 | 1326 / 44 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.settings"}}` |
| 7 | C1 | y | y | y | 18351 | 1353 / 50 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n27"}}` |
| 8 | C2 | y | y | y | 22406 | 1336 / 78 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n12"}}` |
| 9 | C3 | y | y | y | 24634 | 1635 / 97 | `{"kind": "done", "done": {"status": "completed", "summary": "The calculator successfully computed 12 multiplied by 34, resulting in 408.", "` |
| 10 | W1 | y | y | y | 17667 | 1311 / 45 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.settings"}}` |
| 11 | W2 | y | y | y | 20179 | 1773 / 57 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n33"}}` |
| 12 | W3 | y | y | y | 18388 | 1039 / 62 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 13 | W4 | y | y | y | 19894 | 1254 / 64 | `{"kind": "done", "done": {"status": "completed", "summary": "WLAN has been successfully turned on.", "evidence": ["#n17 CheckBox id=checkbox` |
| 14 | W5 | y | y | n | 20320 | 1039 / 70 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 15 | W6 | y | y | n | 23952 | 1577 / 93 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 16 | W7 | y | y | n | 18196 | 1327 / 49 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.settings"}}` |
| 17 | C1 | y | y | y | 19095 | 1354 / 56 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n27"}}` |
| 18 | C2 | y | y | y | 20676 | 1337 / 74 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n12"}}` |
| 19 | C3 | y | y | y | 25863 | 1635 / 99 | `{"kind": "done", "done": {"status": "completed", "summary": "The calculator was used to compute 12 multiplied by 34, resulting in 408.", "ev` |

</details>

### 5.3 Sony gemma-4-E2B-it (GPU, 英文目标)

| 指标 | 数值 |
| --- | --- |
| 正式轮次 | 20 (预检 2 轮: A=fail, B=ok; 正式采用变体 B) |
| 传输 / 超时错误 | 6 / 20 (30%) (其中超时 6) |
| JSON 合法率 (分母 = 全部轮次) | 14 / 20 (70%) (严格解析 14, 宽松解析 0) |
| JSON 合法率 (分母 = 有应答轮次) | 14 / 14 (100%) |
| Schema 合规率 (分母 = 全部轮次) | 13 / 20 (65%) |
| Schema 合规率 (分母 = 有应答轮次) | 13 / 14 (93%) |
| 决策合理率 (自动期望) | 9 / 20 (45%) |
| 决策合理率 (人工复核后) | 9 / 20 (45%) |
| 脚本侧耗时 ms (均值 / 中位 / 最大) | 42704 / 43111 / 56811 |
| 提供方 durationMillis (均值 / 中位 / 最大) | 34109 / 34335 / 48161 |
| 输入 token (均值 / 中位 / 最大) | 1299 / 1350 / 1650 |
| 输出 token (均值 / 中位 / 最大) | 84 / 82 / 122 |

不符轮次:

- #0 W1: 自动期望不符 (expected tool, got ask); 人工复核: 不合理; 推理 `The current screen is the launcher, and there is no visible Wi-Fi toggle. I need to launch the settings app to find the Wi-Fi option.`
- #6 W7: 自动期望不符 (expected script_run, got ask ); 人工复核: 不合理; 推理 `The current screen is the launcher, and there is no visible Wi-Fi toggle. I need to launch the settings app to find the Wi-Fi option.`
- #8 C2: Schema 不合规 ['arguments string invalid JSON']; 原文 `{"kind":"tool","reasoning":"The goal is to compute 12 times 34. I need to input '12', the operator 'x', '34', and then press '='. I will start by setting the first number '12' in the formula field (#n`
- #10 W1: 自动期望不符 (expected tool, got ask); 人工复核: 不合理; 推理 `The current screen is the launcher, and there is no visible Wi-Fi toggle. I need to launch the settings app to find the Wi-Fi option.`
- #16 W7: 自动期望不符 (expected script_run, got ask ); 人工复核: 不合理; 推理 `The current screen is the launcher, and there is no visible Wi-Fi toggle. I need to launch the settings app to find the Wi-Fi option.`

<details><summary>逐轮记录</summary>

| # | 轮次 | JSON | Schema | 合理 | 耗时 ms | 输入 / 输出 token | 决策 (截断) |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | W1 | y | y | n | 34575 | 921 / 64 | `{"kind": "ask", "ask": {"question": "Launch the settings app", "kind": "text", "choices": ["com.android.settings"]}}` |
| 1 | W2 | y | y | y | 43502 | 1349 / 90 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 2 | W3 | y | y | y | 46315 | 1592 / 81 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n9"}}` |
| 3 | W4 | n | n | n | 300087 | - / - | `ERROR TIMED_OUT` |
| 4 | W5 | n | n | n | 300086 | - / - | `ERROR TIMED_OUT` |
| 5 | W6 | y | y | y | 48309 | 1594 / 90 | `{"kind": "ask", "ask": {"question": "Which Wi-Fi network would you like to connect to?", "kind": "text"}}` |
| 6 | W7 | y | y | n | 32978 | 940 / 64 | `{"kind": "ask", "ask": {"question": "Launch the settings app", "kind": "text", "choices": ["com.android.settings"]}}` |
| 7 | C1 | y | y | y | 35876 | 988 / 75 | `{"kind": "tool", "tool": "app_launch", "arguments": {"app": "com.android.calculator"}}` |
| 8 | C2 | y | n | n | 45383 | 1351 / 97 | `{"kind": "tool", "tool": "ui_set_text", "arguments": ":[{"}` |
| 9 | C3 | y | y | y | 45947 | 1650 / 80 | `{"kind": "done", "done": {"status": "completed", "summary": "Computed 12 times 34, resulting in 408."}}` |
| 10 | W1 | y | y | n | 33118 | 922 / 64 | `{"kind": "ask", "ask": {"question": "Launch the settings app", "kind": "text", "choices": ["com.android.settings"]}}` |
| 11 | W2 | y | y | y | 42041 | 1350 / 85 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n17"}}` |
| 12 | W3 | n | n | n | 300089 | - / - | `ERROR TIMED_OUT` |
| 13 | W4 | n | n | n | 300077 | - / - | `ERROR TIMED_OUT` |
| 14 | W5 | n | n | n | 300081 | - / - | `ERROR TIMED_OUT` |
| 15 | W6 | y | y | y | 56811 | 1595 / 122 | `{"kind": "ask", "ask": {"question": "Which Wi-Fi network would you like to connect to?", "kind": "text", "choices": ["Select a network"]}}` |
| 16 | W7 | y | y | n | 36149 | 941 / 77 | `{"kind": "ask", "ask": {"question": "What is the next step to turn off Wi-Fi?", "kind": "text", "choices": ["Launch Settings app", "Search f` |
| 17 | C1 | n | n | n | 300077 | - / - | `ERROR TIMED_OUT` |
| 18 | C2 | y | y | y | 42720 | 1352 / 83 | `{"kind": "tool", "tool": "ui_set_text", "arguments": {"nodeRef": "n3", "text": "12 * 34"}}` |
| 19 | C3 | y | y | y | 54142 | 1650 / 112 | `{"kind": "done", "done": {"status": "completed", "summary": "12 times 34 computed as 408", "evidence": ["changes: formula \"12x34\", result ` |

</details>

### 5.4 Schema 特性探针 (Pad E4B, `schema-probe.js`, 一轮固定提示)

| 变体 | 内容 | 结果 |
| --- | --- | --- |
| V1 | 附录 D 原样 (`arguments: { type: object }`) | 接受, `{"kind":"tool","tool":"ui_click","arguments":{"nodeRef":"n30"}}` |
| V2 | 去掉 `maxLength` / `maxItems` | 接受, 同上 |
| V3 | `arguments` 加 `additionalProperties: true` | 接受, 同上 |
| V4 | `arguments` 带类型化可选属性 | 接受, 同上 |
| V5 | `arguments: { type: string }` (变体 B) | 接受, `"arguments": "{\"nodeRef\": \"n30\"}"` |
| V6 | B 且去掉嵌套 `ask` / `done` | 接受, 同 V5 |
| V7 | B 且去掉所有 `additionalProperties: false` | 接受, 同 V5 |
| V8 | 附录 D 去掉 `arguments` | 接受, 但模型在 `kind: tool` 下同时填了 `ask` 与 `done` (Schema 表达不了互斥, 验证器必须按 `kind` 只接受对应分支) |

八个变体的提供方耗时 29-45 s (输入 75 token, 输出 33-62 token), 输出中 token 间夹带换行但严格 JSON 解析通过. 结论: 扁平 Schema 在本地约束解码下可用, `oneOf` / `if-then` 不需要, `arguments` 保持对象.

## 6. 本地延迟探针 (`timing-probe.js`, 三种提示长度, `maxTokens` 120)

| 目标 / 后端 | 提示 | 脚本侧 ms | 提供方 ms | 输入 / 输出 token | 输出 (截断) |
| --- | --- | --- | --- | --- | --- |
| Sony E2B cpu | tiny ~75 tokens | 104882 | 30738 | 56 / 39 | `{"kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "n30" }` |
| Sony E2B cpu | medium ~700 tokens | 167755 | 158851 | 587 / 41 | `{ "kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "n30" ` |
| Sony E2B cpu | settings-home local fixture | 174845 | 165957 | 709 / 41 | `{ "kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "dashb` |
| Sony E2B gpu | gpu warm-up tiny | 52272 | 6545 | 32 / 10 | `{"kind": "done" }` |
| Sony E2B gpu | tiny ~75 tokens | 19801 | 11327 | 56 / 39 | `{"kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "n30" }` |
| Sony E2B gpu | medium ~700 tokens | 26868 | 18320 | 587 / 41 | `{ "kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "n30" ` |
| Sony E2B gpu | settings-home local fixture | 26904 | 18530 | 709 / 39 | `{"kind": "tool", "tool": "ui_click", "arguments": { "nodeRef": "dashbo` |
| Pad E4B gpu | gpu warm-up tiny | 67972 | 4001 | 32 / 8 | `{"kind": "done"}` |
| Pad E4B gpu | tiny ~75 tokens | 12573 | 5047 | 56 / 33 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n30"}}` |
| Pad E4B gpu | medium ~700 tokens | 14871 | 6964 | 587 / 33 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n30"}}` |
| Pad E4B gpu | settings-home local fixture | 15326 | 7775 | 1149 / 33 | `{"kind": "tool", "tool": "ui_click", "arguments": {"nodeRef": "n33"}}` |

首次调用的脚本侧耗时含引擎加载 (Sony cpu 约 75 s, gpu 约 45 s). Sony gpu 对 700 token 提示约 18 s 提供方耗时, 是 cpu 的 1/9. Pad E4B cpu 正式轮 (输入 1000-1800 token) 提供方耗时 135-235 s, 见 5.1.

## 7. 在线协议 `responseSchema` 映射核对 (只读代码核对 + 官方文档, 2026-09)

| 协议 | 3-Stone AI 请求字段 (代码) | 官方约束 (结构化输出) | 附录 D 原样是否可接受 | 失败呈现 |
| --- | --- | --- | --- | --- |
| OpenAI 兼容 (`backend/OpenAiCompatibleRequest.kt`) | `response_format: { type: json_schema, json_schema: { name: "response", strict: true, schema } }`; 模板 `structuredJson=false` 的 DeepSeek 直接抛 `Online AI provider does not support response schemas` | 严格模式: 每个对象所有属性必须列入 `required` (可选用 `["string","null"]`), 每个对象 `additionalProperties: false`, 不支持 `minLength` / `maxLength` / `pattern` / `format` / `minimum` / `maximum` / `minItems` / `maxItems`, 无属性的自由对象不允许, 深度 <= 5, 属性 <= 100 | 否 (可选属性未 required, `maxLength` / `maxItems`, 自由对象 `arguments`); 本次代理未校验故通过 | HTTP 400 -> `REQUEST_REJECTED` |
| Anthropic (`backend/AnthropicMessagesProtocol.kt`) | `output_config.format = { type: json_schema, schema }` (GA 形态, 无 beta 头) | 每个对象 `additionalProperties` 必须为 false, 不支持 `minLength` / `maxLength`, 数组只支持 `minItems` 0 / 1, 不支持数值约束与递归, 允许可选属性; 不支持的特性返回 400 | 否 (`maxLength` / `maxItems`; `arguments` 对象缺 `additionalProperties: false`) | HTTP 400 -> `REQUEST_REJECTED` |
| Gemini (`backend/GeminiGenerateContentProtocol.kt`) | `generationConfig.responseMimeType = application/json` + `generationConfig.responseSchema = schema` (OpenAPI 子集的 `Schema` 对象, 不是 `responseJsonSchema`) | `Schema` proto 字段: type / format / title / description / nullable / enum / items / maxItems / minItems / properties / required / min-maxProperties / minimum / maximum / minLength / maxLength / pattern / example / anyOf / propertyOrdering / default; 没有 `additionalProperties` (含该键返回 400 `Unknown name "additional_properties"`) | 否 (三处 `additionalProperties: false`); `maxLength` / `maxItems` 可以 | HTTP 400 -> `REQUEST_REJECTED` |
| 本地 LiteRT-LM (`backend/LiteRtLocalSession.kt`) | `ConversationConfig(enableResponseFormat = schema != null)` + `ResponseFormat.json(schema)` | 约束解码, 5.3 的八个变体全部接受 | 是 | 超长输入 `PROVIDER_FAILED` (无细节) |

来源: OpenAI structured outputs 指南 (developers.openai.com/api/docs/guides/structured-outputs) 与 Azure OpenAI 同题文档 (learn.microsoft.com, 2026-08-24, 列出严格模式的 required / additionalProperties / 不支持关键字 / 深度与属性数上限); Claude structured outputs (platform.claude.com/docs/en/docs/build-with-claude/structured-outputs); Gemini `generative_service.proto` 与 `content.proto` (github.com/googleapis/googleapis, `GenerationConfig.response_schema` / `response_json_schema` 与 `message Schema` 字段) 及 python-genai issue #1815 (2025-12, `response_schema` 含 `additionalProperties` 的 400 报错); 3-Stone AI 源码为本机工作副本 (Pad 1.1.4 / Sony 1.1.0 与之同源).

失败呈现的代码路径: `backend/OnlineAiFailure.kt` `failureForOnlineAiHttpStatus`: 3xx `REDIRECT_REFUSED`, 401 `AUTHENTICATION_FAILED`, 403 `PERMISSION_DENIED`, 408 / 504 `TIMED_OUT`, 429 `RATE_LIMITED`, 5xx `SERVICE_UNAVAILABLE`, 其余 (含 400) `REQUEST_REJECTED`; 提供方响应体不保留, 脚本侧收到 `AiPluginRouteException` (code 见 `AiErrorCode`), 看不到 Schema 被拒的具体字段. Schema 本身经 `OnlineAiRequestSupport.parseSchema` 严格解析, 与消息合计 <= 256 KiB.

对 P2 `DecisionSchema` 的含义: 单一 Schema 无法同时满足三种在线协议. 建议按目录中目标的 `provider` 字段生成变体: 全部在线变体去掉 `maxLength` / `maxItems` (长度限制改由 `DecisionValidator` 截断); Gemini 变体去掉 `additionalProperties`; Anthropic 变体给 `arguments` 补 `additionalProperties: false` 并保留可选属性; OpenAI 严格变体把所有属性列入 `required` 并用 `["..", "null"]` 表达可选, `arguments` 用 `anyOf` 枚举附录 C 各工具的参数 Schema (或退回字符串变体 B); 首选变体 A, 收到 `REQUEST_REJECTED` 时降级 B 并按目标记忆. 这是建议, 待维护者在 P2.2 前确认.

## 8. 代理是否执行 `response_format` (`proxy-probe.js` / `proxy-probe2.js`, 未得出结论)

```text
prose-under-schema -> REJECTED [object Object] code=PROVIDER_FAILED
```

追加探针 (`proxy-probe2.js`, 四次调用: 无结构化基线 / 结构化 + Schema 重复 / 结构化无 Schema / 枚举外取值):

```text
1 baseline prose, structuredJson=false, no schema -> REJECTED wall=1089ms [object Object] code=PROVIDER_FAILED message=AI plugin chat failed (PROVIDER_FAILED)
2 prose under structuredJson=true + schema (repeat) -> REJECTED wall=975ms [object Object] code=PROVIDER_FAILED message=AI plugin chat failed (PROVIDER_FAILED)
3 prose under structuredJson=true, no schema -> REJECTED wall=1203ms [object Object] code=PROVIDER_FAILED message=AI plugin chat failed (PROVIDER_FAILED)
4 schema-violating request: ask for kind=finished (not in enum) under schema -> REJECTED wall=1020ms [object Object] code=PROVIDER_FAILED message=AI plugin chat failed (PROVIDER_FAILED)
done
```

## 9. 决策点结论

- 规则原文: 若 (b) / (c) / (d) 中至少两种的 Schema 合规率 >= 95% 且本地模型 JSON 合法率 >= 90%, D7 成立; 否则结构化模式的修复重试从 1 次提高到 2 次.
- 实测: (b) 一种, Schema 合规 20 / 20 (100%); (c) (d) 未配置; 本地 Pad E4B JSON 合法 20 / 20 (100%) (cpu) / 20 / 20 (100%) (gpu), Sony E2B 有应答轮次 14 / 14 (100%) (全部轮次 14 / 20 (70%), 超时计入分母). 规则的 "至少两种在线" 无法满足, 按既定规则执行 "否则" 分支: 结构化模式修复重试 = 2 (D35 回填).
- D7 保留: 在线目标决策合理率 20 / 20 (100%), 本地 E4B 16 / 20 (80%) (cpu) / 14 / 20 (70%) (gpu), 结构化 JSON 决策循环在现有证据下可行; H.2 退路 (计划 + 执行两段式) 不触发. 当 (c) / (d) profile 可用时 (P2 验收前) 用同一脚本补跑并回填本文.
- 本地模型的定位维持路线图 3.3 的判断: 协议正确 (E4B JSON / Schema 合规 100%), 决策质量 E4B 可用于简单用例 (1) (2) 但会误读开关状态与忽略 `ask` 规则, E2B 不足 (把 `ask` 当动作, 内嵌 JSON 出错, 30% 超时); 每步延迟 E4B cpu 2.5-4 分钟 / gpu 约 20 秒, E2B gpu 30-57 秒, 决定 1.0.0 的验收用例以在线目标为主, 本地目标默认应选 gpu 后端 (可用时).
- 影响 P2 的三项设计输入 (第 1 节末条): 本地提示词预算 (4096 token, 需要二级压缩与历史截断), `DecisionSchema` 按协议生成变体, `DecisionValidator` 按 `kind` 只接受对应分支并把长度限制从 Schema 移到验证器.

## 10. 文件清单 (`docs/dev/spike/p0/`)

- `fixture-dump.js` (真机夹具捕获), `make_fixtures.py` (合成变体), `make_local_fixtures.py` (二级压缩), `spike.js` (主脚本), `schema-probe.js`, `timing-probe.js`, `proxy-probe.js`, `summarize.py`, `config-*.json`.
- `fixtures/` 原始捕获 (脱敏), `fixtures-out/<device>/` 运行用夹具 (含 `.local`).
- `results/results-*.jsonl` 逐轮记录 (含模型原文与推理), `results/schema-probe-pad.txt`, `results/timing-probe-*.txt`, `results/proxy-probe-sony.txt`, `results/catalog-*.txt`.
- 本文由 `build/tmp/spike/build_evidence.py` 生成 (未提交), 数字直接来自上述 JSONL.

# P9.1 Wi-Fi 双路径复测与模型格式诊断 (2026-09-26)

本轮按原 P9.1 剩余测试条目复测. 设备与独立网络条件已具备, 真实任务仍未通过: 两条路径均在首个决策步骤收到 3 次空完成事件, 未执行工具. 不需要在线图片模型或增加测试设备; P9.2 图片输入验收继续独立待办.

## 条件与测试范围

- 五台既有真机均可由 ADB 访问. 实际调用模型和修改临时测试设置只在 QV710AF65F / Sony XQ-AT72 / Android 12 / API 31 上进行, 其测试 SIM 与移动数据已启用, 屏幕已解锁.
- 宿主 6.8.0 / build 5297, Provider 3-Stone AI 1.2.0 / build 216. Agent 从 1.1.0 / 87 更新到已验证的正式签名 R8 候选 1.2.0 / 88, 保留应用数据. APK CRC32 `47afd9ea`, SHA-256 `f1395767880420bb36a96bc083ffb82973bb60086e3aa02778e50b1b689ee310`.
- 公开目录重新查询成功, Model8 目标 configured/available 为 true, 声明 structured-json 和 tools; 设备界面模型 ID 为 `claude-fable-5-1`. 不读取凭据, 不修改模型配置, 不使用 PoloAPI 或图片输入. 目录声明不能代替真实模型能力验收.
- AutoJs6 无障碍最初处于 enabled 但 crashed/unbound 状态. 只重绑定用户原已启用的 AutoJs6 服务, 保留其他无障碍组件, 复测前实际 bound 且不再 crashed. 无需用户手动重开.
- Provider 的允许移动/计费网络原值为 false, 经正常应用设置页临时启用. Gemma 下载页显示已下载并校验, 未在下载过程中关闭 Wi-Fi.

## 独立网络预检

先将 Wi-Fi 关闭作为任务初始状态, 再等待实际默认 CELLULAR network 同时具有 INTERNET 与 VALIDATED. 第一轮约 4280 ms 后满足; 第二轮同一条件仍满足. 排除仅带 IMS 的蜂窝连接, 不把 NetworkRequest 或历史日志中的 capability 当作当前活动网络. 两次模型任务均在预检通过后启动.

这修正了上一批 JSON 用例仅等待 5 秒且验证未通过就启动的编排疏漏. 本轮在严格预检后仍复现空回复, 因此不能把旧空响应直接归因为该预检疏漏.

## 真实用例结果

同一目标, 同一 Wi-Fi 开启并回读任务, 同样的 observe/act/user 工具组, cautious 确认规则与系统设置普通动作自动确认名单. 两轮均限 30 步, 40 模型调用, 600000 ms, 默认 300000 tokens. JSON 使用 modelPath=json, 原生优先使用 modelPath=auto; 不开放动态脚本, MCP 或视觉. 两轮均从 Wi-Fi 关闭开始, 启动前前台分别为 AutoJs6 与桌面; 未把不同前台观察的失败样本用于性能比较.

| 路径 / case | 终态 | 步数 / 工具 / 模型调用 | input / output tokens | 耗时 |
|---|---|---|---|---|
| JSON / p91-qv710-json-wifi-20260926-02 | failed / DECISION_UNPARSABLE | 1 / 0 / 3 | 34219 / 0 | 10095 ms |
| 原生优先 / p91-qv710-native-wifi-20260926-02 | failed / DECISION_UNPARSABLE | 1 / 0 / 3 | 41574 / 0 | 6486 ms |

两轮 usage.estimated 均为 false, 公开 final 未截断. 每轮 3 个 completed 的 text 均为 0 字节; Agent 已按现有上限做 2 次决策修复, 随后终止. 没有 tool_calls 事件或 submitToolResults 续轮, 因而原生优先配置不代表真实工具往返成功. 两轮独立观察均在人工恢复前确认 wifi_on=0 且系统开关 checked=false, 留存私有 XML 与截图.

两次 E4 instrumentation 均为 OK (1 test), 仅说明采集成功. 正式 R8 Agent 不提供 debug run-as, 本轮没有 full-run.json; 使用未截断的宿主公开 final/snapshot 和模型事件, 不虚构私有存档. 失败样本全部保留, 不增加预算或反复抽样直到成功. 耗时是失败耗时, 不作为两条路径效率优劣的比较.

G8441 既有计算器两路径成功及 XQ-AT72 先前原生 Wi-Fi 成功仍按原证据保留; 不用历史单路径成功替代本轮同条件双路径通过.

## 最小格式诊断

宿主提交 `1d2c03637d` 新增显式启用的 androidTest `AiAgentRealModelE4Test#modelFormatProbe`, 使用同一目标和固定合成文本, structured/plain 各一次, stream=true, 2048 输出 tokens 上限, 各 60 秒, 无工具和设备动作. 运行使用恢复后的已验证 Wi-Fi, Provider 计费网络仍关闭; 没有再次切换网络. case 为 `p91-format-20260926-01`.

| 模式 | 观察结果 | 正文 / token 证据 | 耗时 |
|---|---|---|---|
| structured | harness_timeout, 已取消请求 | 采集期限内未收到终态与 usage, 不把缺失当作 0 tokens | 60009 ms |
| plain | completed, 固定预期不匹配 | 正文 0 字节, input 5269 / output 0 / total 5269 tokens | 1579 ms |

本次 collectionComplete=false, instrumentation 报 FAILURES, 尽管 adb 进程退出码为 0. 结构化一轮的 60 秒是采集总期限, 不能伪称已经收到上游 TIMEOUT 或正常 STOP. 普通文本一轮则确实完成为空, 说明不使用结构化 schema 或设备动作也能复现空输出; 这不支持将问题仅归因于 Agent JSON 解析或结构化参数. 两个单样本的先后顺序与服务瞬时状态仍是限制, 不据此断言服务端或客户端映射中的具体根因.

探针支持有界 inline/FD 事件读取并关闭描述符, 只把原始正文写入应用私有证据, 正常状态输出仅含枚举/计数/匹配布尔. 每轮结束或超时均取消自己的请求并关闭 broker. 本次真实运行覆盖了采集期限和空 completed 分支, 没有正向非空或大 FD 响应样本, 不把源码审阅算作这些正向实测.

未传 opt-in 的同一方法另跑一次, 触发 AssumptionViolatedException / INSTRUMENTATION_STATUS_CODE=-4, 没有 probeMode 状态或模型生成. AndroidJUnit 的 OK (1 test) 在这里是带条件跳过的采集结果, 不是在线模型通过. 使用方式见 [E4 说明](e4/README.md).

## 构建与审阅

- 宿主测试包 `:app:assembleAppDebugAndroidTest` 最终构建成功, 5m 40s, 两名 agent 对调用界限和生命周期进行独立审阅. 只更新 XQ-AT72 的同签名 androidTest APK, 未替换其宿主主 APK 或 Provider.
- 测试 APK SHA-256: `81161b97203aea1528da52c5ce7d336fe162481ae5b89e434786234905097f31`.
- 第一轮因共享 Gradle journal 缓存由另一个 Java 进程持锁而失败, 没有终止该进程或删除共享锁. 使用隔离 Gradle home / project cache 与只读依赖缓存绕开; 第二轮任务名 assembleDebugAndroidTest 存在 App/Inrt 歧义而失败, 明确为 AppDebugAndroidTest 后成功. 保留三轮日志.
- 只改 androidTest 与开发文档, 未执行全量 JVM/Android/发行 lint 或构建新的 Agent 发布包; 不将旧完整回归数算作本轮新验证. 工作区补丁和新增文档标点扫描通过.

## 结论与诊断边界

- P9.1 真实 Wi-Fi 双路径验收保持未完成, 没有新增/分拆/丢弃路线图阶段. 当前不缺真机数量, SIM 或无障碍手动操作. 继续通过验收需要可稳定返回文本/工具调用的模型链路; 现有 Model8 的最小普通文本也返回空, 仍需定位调用兼容/上游响应, 或使用维护者确认的其他可用文本目标作对照. 没有要求图片/视频模型.
- 已核实宿主与 Agent 事件层没有主动丢弃收到的非空正文. Provider 文本适配器允许合法空正文成为 completed, 不足以判断上游确实为空或用了未识别的兼容字段.
- Provider StreamingOutputBuffer 的 finishReason 默认 STOP, 不能把本轮 finishReason=1 当成上游明确正常停止的证明. 上游细分错误在 Provider/宿主多处合并, 本轮未擅自修改生产错误契约或把推理内容替代为最终回答.

## 恢复与提交范围

两次 Wi-Fi 用例后 Wi-Fi 已恢复为 1 并重新获得 INTERNET/VALIDATED/NOT_METERED, Provider 允许移动/计费网络恢复 false. 字体仍 1.0, screen_off_timeout 仍 600000, mobile_data 仍 1, 已启用无障碍组件集合保持原值. 未修改代理, VPN, APN, DNS, 已保存网络或下载配置, 没有订单和付款. 格式探针与默认跳过检查结束后, 再次恢复因 instrumentation 退出而被系统标记 crashed 的原已启用 AutoJs6 服务, 最终实际 bound=true / crashed=false; 其他服务集合及已记录设置仍与原值一致. 本轮临时 UI XML 已删除, 私有验收证据保留.

本轮源码改动仅为宿主 androidTest 诊断夹具; Agent 仅更新 E4 说明, 证据, ROADMAP 与提交计数. 不发布 APK/npm, 不推送远程, 不改变生产 API 或 Rhino 同步文件. 原始目标, 界面, 模型事件, 截图和探针正文仅保存在忽略目录或设备应用私有目录.

# P2.3 运行状态机, 预算与确认证据

日期: 2026-09-23. 按原 P2.3 实施, 不改变阶段边界. 本节记录纯核心与注入假代理的验证; 真实模型和 Binder 接入属于后续原 P2.4/P2.5.

## Budget

- 默认: 40 步, 60 次模型调用, 普通任务 10 min / detached 30 min, 300,000 token, 工具 30 s, 确认 120 s, 提问 10 min. 参数受附录 B.5 上限约束.
- 串行调度器拥有计数器. 先准入再计数, 每次模型请求按 0.4 token/byte 估算输入并收紧输出 token; 优先结算真实 usage, 缺失字段按估算计数并标记 `estimated`. total 采用报告值与输入/输出之和的较大值, 加法饱和防溢出.
- 取消或失败的在途请求也结算已准入的输入估算, 不假定消耗为零. 时间使用单调时钟, 运行后包括用户等待时间, 不包括排队时间. 只有登记脚本可申请最长 5 min 的工具时限, 仍受剩余任务时长限制.
- `BudgetTest` 5 个 JVM 用例通过: 默认值/上限, 准入计数, 缺失 usage/取消结算, 超额/溢出, 时间与工具时限. 后续运行器提交补齐集成路径证据.

## ConfirmationGate

- 每个任务持有一个确认门. 默认只读/普通操作自动执行, 敏感操作请求确认; 审慎策略让全部非只读操作确认. 一次授权不缓存; 任务内授权只匹配同一工具和同一风险等级.
- 支付识别来自只读准备阶段的可信元数据, 支持支付包名和 10 语言支付关键词. 即使已有同工具敏感等级授权, 支付仍逐次确认并拒绝 `scope=run`. `memory_propose` 和登记脚本的强制确认也不可由整任务授权绕过.
- `ToolPolicy.fromAssets` 同时装载敏感词与支付词. 配置集合取副本, 调用方之后修改原集合不会改变在途策略. 后续宿主适配器必须绑定检查与执行的目标, 不接受模型自行声明风险或密码属性.
- 描述使用 `ToolSpec` 的英文/中文说明并附目标文本, 参数摘要由运行器控制大小; 密码字段的描述和 text 参数使用 `***`.
- `ConfirmationGateTest` 7 个 JVM 用例通过: 默认/审慎矩阵, 授权范围, 支付逐次确认, 10 语言关键词, 强制确认, 配置隔离, 密码摘要.

## StepJournal

- 私有任务内存日志保留每步编号, 决策摘要, 工具/参数, 确认, 观察, usage, 耗时和错误码; 模型尚未产生有效决策时只结算 usage 和终态, 不伪造步骤决策.
- 最多 200 条步骤, 整个序列化快照 (含 JSON 转义, 包装和终态) 不超过 1 MiB. 必要时移除最早记录并标记截断; 单步骤/终态事件分别限制在 12/24 KiB. 缩短正文不丢失步骤编号, kind, AgentResult 的 id/status/计数/usage.
- 密码节点的 `ui_set_text.text` 在记录中替换为 `***`, 同值在决策理由, 观察和终态摘要中也遮蔽, 已有历史会重新脱敏. JSON 转义形式也处理, 不改变 status/kind 等协议枚举. 对外读取返回副本.
- 日志仅为纯核心任务历史, 尚未增加 P6 的持久化存储, 不写普通日志.
- `StepJournalTest` 4 个 JVM 用例通过: 200 步和副本隔离, 1 MiB/JSON 转义/小容量边界, 密码与终态脱敏, 与协议词同名的密码. 后续运行器测试检查确认/step/done 事件的脱敏.

## AgentRunner 与 RunQueue

- 完成原 P2.3 状态机: queued, running, waiting_input, waiting_confirmation, cancelling 与五种终态. 一个串行调度器管理一条链路; 1 个活动任务 (包括等待用户) + 8 个排队任务, 其后拒绝. 已结束任务移出活动队列, 历史由持有句柄的调用方读取, P6 再接持久化.
- 每步依次经过上下文端口, 模型端口, P2.2 决策解析/校验/最多 2 次修复, 只读工具准备, 确认门, 执行与观察. 只有执行阶段可以产生副作用. `PreparedTool` 保持原准入调用的身份, 参数和 ToolPlan 的 JSON 取副本, 防止适配器改写后绕过确认.
- 预算覆盖队列开始后的所有等待. 每轮上下文包含剩余预算; 步数/调用次数/token/总时长及普通工具时限耗尽以 `BUDGET_EXCEEDED` 终止, 摘要说明具体维度. 已有成功工具则 `partial`, 否则 `failed`. 脚本单次超时回送 `SCRIPT_TIMEOUT`, 用户等待超时回送 `USER_TIMEOUT`.
- `respond`/`confirm` 按任务内唯一 requestId 匹配, 校验类型/选项/体积; 旧请求, 重复回答和终态回答均不执行操作. 回答记忆提问只形成待确认建议, 不写入记忆. 拒绝和超时作为下一步观察.
- 取消优先于尚未发布的终态并尝试取消在途端口, 已完成的设备操作不回滚. 真实线程测试将取消和完成同时放行 40 次, 验证取消返回值与唯一终态一致. 迟到/重复回调在结束后或调度器关闭后均无后续事件. 观察者异常不终止任务.
- 队列收到 hostUnavailable/detach 后, 活动和排队任务全部 `blocked`, 拒绝新任务, 不自动续跑. 模型或工具端口回报宿主失联时当前任务也立即 blocked. P2.5 将死亡通知接到真实 Binder.
- step 与 done 事件使用脱敏日志结果, 输入密码的实际执行参数仍传给受信适配器. 每任务事件序号单调递增, 仅一个 done, 其后无新事件. 固定终态摘要和预算维度覆盖 10 语言, 原始模型/端口异常正文不进入普通日志或错误摘要.

## 验证与边界

| 项目 | 结果 |
| --- | --- |
| JVM | 164/164, 相比 P2.2 增加 56 个用例. Budget 5, ConfirmationGate 7, StepJournal 4, AgentRunner 20, cancellation 9, queue 5, ports/scheduler 6 |
| 离线 D32(1) | 假模型 + 有状态假设备: 启动设置, 读取 Wi-Fi 状态, 点击开关, 等待匹配, 再读取 checked=true, 完成. JVM 6 次模型调用 / 5 次工具执行 / 6 步, 无修复重试 |
| Android | 私有只读 AVD, SDK 37 / Android 17 / x86_64 / PAGE_SIZE=16384, 10/10. 新增 2 个真实调度器测试: Wi-Fi 假代理闭环, 取消及调度器关闭后的迟到回调 |
| Android 契约基线 | AutoJs6 6.8.0 / 5285 + 插件 1.0.0 / 18, INFO/Wake/服务发现/占位 Binder/启动器与既有核心测试全部通过 |
| 构建 | Temurin 21.0.12.1+1, debug/androidTest/Release-R8 成功, 无新增依赖或 AAR |
| lint | 0 错误, 5 个既有警告: Gson/XZ 可更新, 未使用 round icon, 两项昼夜图标重复 |
| 文档 | 10 语言 changelog/README/插件说明同步, 36 产物一致性检查通过 |

复现核心构建:

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
py .python/generate_markdown.py --check
```

本轮 instrumentation 用定向 `adb -s emulator-5584 install -r` 安装宿主, 插件和测试 APK, 再执行 `adb -s emulator-5584 shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner`. 等价执行测试 APK 的全部 10 个用例, 避免 connected 任务同时部署到其他已连接设备. 私有 AVD 以 read-only/no-window/no-snapshot 启动, 验证后关闭, 未操作真机.

这是 E1/E2 核心/假代理证据. 没有请求真实模型, 没有执行真实 Android Wi-Fi 操作, 不代表 D32 的 E4 真机验收. 目前安装版仍是宿主状态启动页与占位 Binder. 原 P2.4 负责真正的 ContextCompiler/ModelClient, P2.5 接宿主链路和前台服务, P3/P4 接登记脚本/本地工具与 UI 执行适配器及完成证据校验, P5/P6 提供 JS API 和任务台. 路线图阶段未增加, 分拆或丢弃.

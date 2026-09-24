# P3 登记脚本真实模型与真机验收

## 范围与结论

2026-09-24 完成原 D32(3) / P3 验收: 在 Redmi 12C 上, Model8 Fable 5.1 从真实登记目录选中原始 "清理下载目录旧安装包" 项目, 询问 days, 经一次敏感操作确认后执行, 取得 ai.agent.result 的结构化结果并以 done 收尾. 任务终态 completed. 不增加或调整路线图阶段.

本次使用宿主原始示例, 没有修改脚本或模型响应. 实际删除仅限本次创建的下载子目录中的无效测试安装包, 没有清理用户已有下载文件. 目标明确要求先询问保留天数, 不将此次证据解释为所有自然语言目标下都会主动询问.

## 环境与复现

- 设备: Redmi 12C (22120RN86C), Android 13 / API 33.
- 模型: 3-Stone AI 1.1.4 / 214 中已配置的 Model8 Fable 5.1, 经宿主 AiAgentModelBroker 调用; 无本地模型替换或假模型注入.
- 宿主: 6.8.0 / 5292, 15ec044ffa. Agent 安装包: 1.0.0 / 47, c7aeb06 的生产行为; 后续开发驱动/文档提交不改变 APK.
- 测试入口: AiAgentRealModelE4Test.runRealGoal 与 docs/dev/e4/device_case.py. interaction=script, confirm=cautious, memory=false, toolGroups=[script,user], autoConfirmPackages=[]. 预算收紧为 15 步 / 20 次模型调用 / 600000 ms, token 上限仍为默认 300000.
- 在设备当前工作目录下创建临时登记项目, 逐字复制宿主 sample/agent/清理下载目录旧安装包/project.json 与 main.js. 源文件和设备文件 SHA-256 一致, 未改变工作目录或附加根设置.
- 下载目录的独立子目录内创建 6 个各 59 字节的测试文件: old.apk / old.apks / old.XAPK 的时间为 2020-01-01, fresh.apk 为当前时间, keep.txt 为旧文本, nested/old.apk 为旧嵌套安装包. 开始前读取实际 stat, 不假定共享存储允许设置时间.
- 目标为 "请调用已登记的清理下载目录旧安装包脚本, 帮我清理指定测试子目录中的旧安装包, 执行实际删除. 只处理这个专门的测试子目录, 不要操作其他目录. 保留多少天的安装包请先问我." 实际目标包含该隔离目录的绝对路径.

原始配置, 事件, 模型回复, 脚本结果, 文件前后状态和恢复记录保存在 Git 忽略的 build/e4-p3-20260924, caseId=redmi-registered-cleanup-01. 不提交原始设备证据或 Provider 凭据.

## 闭环证据

1. 模型从自动注入的真实目录选中 clean-download-installers, 产生 text 类型 input, 询问保留最近多少天的安装包. 操作者回答字符串 "30".
2. 模型请求 script_run, 参数为 days=30, directory=指定测试子目录, dryRun=false. 生产确认门发出 confirmation, risk=sensitive, allowRunScope=false, timeoutMs=120000; 没有自动批准规则.
3. 确认前重新读取全部 6 个文件, 大小与时间均未改变. 核对脚本 ID 和所有参数后仅允许该次确认, scope=once.
4. 真实宿主通过 agent.execRegistered 执行原始 main.js, resultReported=true, outcome=success, finished=true. ai.agent.result 上报 matched=3, removed=3, failed=0, bytes=177, names=[old.apk,old.apks,old.XAPK], namesTruncated=false.
5. 模型产生带脚本回执证据的 done/completed. 最终 AgentResult.script 包含登记 ID, 实际项目路径, executionId=0 和完整结构化结果.
6. 独立 adb stat 验证三个旧安装包均已消失, fresh.apk / keep.txt / nested/old.apk 的大小与时间保持不变. 确认一次询问和一次 confirmation, 两次回应都被正式接口接受.

| 度量 | 结果 |
| --- | --- |
| 最终状态 | completed |
| 步数 | 3 (ask, script_run, done) |
| 工具调用 | 1 |
| 模型调用 | 3 |
| 输入 / 输出 / 总 token | 25566 / 516 / 26082 |
| token 是否估算 | false |
| 任务耗时 | 83154 ms, 包含人工检查和回应等待 |
| 解析 / 修复 | 三步均 STRICT, repairs=0 |
| 决策协议模式 | degraded=true, 与该 Provider 公开能力匹配; 非受约束 JSON 模式 |
| instrumentation | OK (1 test), 驱动退出 0 |

这里的 177 字节仅为无效测试安装包总大小, 不代表真实设备空间回收基准. 测试驱动通过与 Agent 任务成功分别判定, 没有仅凭 adb 返回码宣称验收通过.

## 清理与后续

验收后只删除本次已知的剩余测试文件及两个临时空目录, 不递归删除未知内容. 熄屏时间恢复为 120000 ms; instrumentation 退出后仅重新绑定此前已启用的宿主无障碍服务, 保留其他服务. 未创建订单或执行付款, 未修改宿主或插件生产源码.

P3 的原 E4 gate 据此完成. 该证据覆盖一台真机和一个在线模型的一次成功调用; 不代替 P5 脚本侧 AgentRun 生命周期或 P7 稳定性验收. 下一步按原路线图推进 P5.

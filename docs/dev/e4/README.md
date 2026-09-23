# E4 真实模型与设备验收

本目录是 P4.4 的开发测试驱动. 使用已安装 Agent 的真实 Binder 接口, 经宿主的生产模型代理和能力代理执行. 不依赖尚未实现的 P5 `ai.agent.run`, 不替换模型决策或动作回执.

## 前置条件

- 安装同签名的宿主 debug APK, 宿主 androidTest APK 与 Agent debug APK. 宿主包含 `AiAgentRealModelE4Test`, 首次落地于提交 `153f5be3f2`.
- 宿主构建号至少 5289, 本轮深层界面和节点操作能力身份修复使用 5292 (`15ec044ffa`). 在 3-Stone AI 配置在线模型或导入本地模型; 凭据始终保留在 Provider 中.
- 操作者事先开启 AutoJs6 无障碍和文件访问权限, 解锁设备, 允许测试目标应用和所需后台运行. 测试会重启宿主进程, 如服务被标记异常, 仅重新绑定已经启用的 AutoJs6 服务并保留其他服务.
- `adb devices` 可见设备. 每台设备一次只运行一个宿主 instrumentation, 不在用户配置模型时启动测试.
- 安装可通过无障碍读取结果的计算器. 本轮使用 Fossify Calculator 1.4.0; HiPER 的部分显示区不暴露结果文字.

## 获取公开模型目录

以下命令均从本仓库根目录执行. `--adb` 可省略, 默认从 PATH 找到 adb. `--output` 指向被 Git 忽略的目录.

```powershell
py docs/dev/e4/device_case.py --serial DEVICE --output build/e4-private catalog
```

目录来自宿主 `AiAgentModelBroker.listTargets`, 保存为本地 `catalog.json`. 只包含公开目标信息, 不读取或复制 Provider 的账号存储. 将所需模型的完整 `targetId` 写入配置; 多模型配置不能只凭 profile 名称选择默认模型.

## 运行

先在 `build/e4-private/config-calculator.json` 写入配置, 替换公开目录中的 targetId. 每次必须使用新的 caseId, 驱动拒绝覆盖已有本地 case 目录或 instrumentation 日志.

```json
{
  "caseId": "calculator-01",
  "target": "profile:REPLACE_WITH_PUBLIC_TARGET_ID",
  "goal": "打开 Fossify Calculator (org.fossify.math), 用计算器界面计算 12*34, 观察并报告结果. 不要只做心算, 不要修改其他应用或设置.",
  "autoConfirmPackages": ["org.fossify.math"],
  "budget": {"maxSteps": 25, "maxModelCalls": 35, "maxDurationMs": 600000}
}
```

```powershell
py docs/dev/e4/device_case.py --serial DEVICE --output build/e4-private run build/e4-private/config-calculator.json
```

`run` 会等待 instrumentation 返回. 可在另一个终端观察进度或回应. 默认 `interaction=script`, `confirm=cautious`, `memory=false`, 工具组 `observe/act/user`. 可选 `toolGroups`, `context` 与 `budget` 均经正式契约校验, 不能突破预算上限.

`autoConfirmPackages` 仅允许系统设置与两个计算器包名. 只有风险为 normal 且真实前台无障碍根节点包名匹配时, 才逐次确认普通动作; app_launch 单独核对目标包名. 购物测试必须使用空数组, 每个变更动作人工审核, 付款确认拒绝. 测试回复不会改变生产确认门规则.

## 观察, 回应与取消

```powershell
py docs/dev/e4/device_case.py --serial DEVICE --output build/e4-private collect calculator-01
```

终端只输出状态, 步数, 度量, 错误分类和待回应的请求 ID. 完整 `pending.json` 与观察证据保存到本地私有目录. `pending.json` 是最后一次询问, 不保证仍待回应; 先检查最新 snapshot 的 pending/requestId.

普通动作的单次确认文件:

```json
{"runId":"CURRENT_RUN_UUID","requestId":"CURRENT_REQUEST_ID","allowed":true,"scope":"once"}
```

拒绝付款时将 allowed 设为 false. 模型 ask 的回答用 `value`: text/choice 为字符串, confirm 为布尔值, 不与 allowed/scope 混用. choice 必须逐字匹配一个原始选项, 不能附加说明; 驱动在发送前核对当前请求, 选项和 JSON 值类型, 只接受 once 范围的动作确认.

`ask(kind=confirm)` 是模型询问, 不等于动作触发的 `confirmation` 事件. 支付门验收必须核对事件类型, 风险及真实动作未执行的证据, 不能把模型在问题中自称 sensitive 当作分类结果.

```powershell
py docs/dev/e4/device_case.py --serial DEVICE --output build/e4-private reply calculator-01 build/e4-private/reply.json
py docs/dev/e4/device_case.py --serial DEVICE --output build/e4-private cancel calculator-01
```

驱动先推送临时回复文件再原子重命名. 不要直接 adb push 到最终 reply 文件, 否则轮询可能读到未写完的 JSON. 过期或不合约的请求回复只记录拒绝, 不覆盖终态; 仍待回应时可更正回复. cancel 通过当前 harness 取消真实任务; 若系统冻结进程, 取消也可能要等进程恢复.

## 证据与判定

设备端观察和模型回复放在宿主应用私有目录 `files/agent-e4/<caseId>`, Agent 完整运行存档位于插件私有目录 `files/agent-runs/<runId>.json`. 驱动通过 debug run-as 收集到指定输出目录:

- `started.json`: caseId, runId, 公开 targetId 与开始时间.
- `events.jsonl`: 真实运行事件, 动作及确认记录, 相对时间.
- `model-events.jsonl`: 完成/失败/用量事件, 包含原始模型回复以诊断 Schema 错误, 不复制提示词或凭据; 文件描述符承载的大结果不额外解码.
- `node-queries.jsonl`: findOne/findAll 的原始只读返回, 用于区分宿主响应与插件归一化失败, 不采集动作 token.
- `capability-errors.jsonl`: 宿主原始桥接错误, 保留节点失效的具体原因; 不记录成功的动作 token, 不消费或替换传输中的文件描述符.
- `snapshot.json` / `final.json`: 宿主读取的当前/最终快照, 受公共快照大小上限约束.
- `full-run.json`: Agent 的完整私有存档. harness 退出或迟到回复导致 snapshot 滞后时, 以可验证的存档终态为准.
- `harness.json`: 测试程序的耗时/错误; 无模型决策时不能填写虚构的模型指标.
- `<caseId>-instrumentation.txt`: 测试程序运行结果. adb 退出成功或 `OK (1 test)` 仅说明证据收集过程完成, 不代表任务成功.

验收必须检查最终状态及真实观察. 计算器要看到实际输入过程和界面 `408`; 购物车/付款页面不能单独证明已提交订单. 购物测试须人工核对真实订单状态并保留脱敏截图; 发生提交结果不确定时先查订单, 不重试提交. 当前运行是否允许下单及允许数量由操作者事先明确, 本轮最多一笔待付款且不付款.

原始目标, 地址, 联系方式, UI, 回复与截图都可能包含个人信息. 只保存在忽略目录, 不提交原始证据, 不输出到普通日志. `/sdcard/autojs6-agent-e4` 仅用于调试控制文件, 配置/回复读取后即删除; 不在配置中放 API 密钥. 对外证据文档只保留经过审核的指标与脱敏内容.

Wi-Fi 在线验收须在切断 Wi-Fi 后仍有独立网络连接. 没有该条件时如实记为待补测, 不通过人工恢复网络伪造连续模型闭环. 本地 LiteRT 使用公开目标的默认执行配置, 不假定已经启用 GPU.

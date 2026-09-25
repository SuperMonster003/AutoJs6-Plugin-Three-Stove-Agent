# P7 设备兼容矩阵

2026-09-25. 对应原 P7 兼容矩阵, 并闭合 P1.3 的正向生产附着广播待办. Agent build 68, 宿主 APK 5296, 宿主测试提交 `7bab4c5510`. 本项只修改测试驱动和证据, 不修改生产运行逻辑或 Rhino. 所有原始测试/模型/界面记录保留在忽略目录 `build/p7-compat-private/`, 不提交账户, 网络标识或原始观察.

## 设备记录

每台在线设备覆盖安装, Wake/INFO 激活发现, 真实宿主附着广播, 内联询问, 后台通知确认及拒绝后失效, 悬浮球展开/拖动/草稿保留/开始/停止/断开隐藏. 通知通过系统实际点击进入, 不调用 PendingIntent.send 代替点击. 模型和设备动作在 UI 夹具中是确定性返回, 真实模型用例另列.

| 设备 | API | 安装/激活/附着与引导 | 确认/悬浮球等插件检查 | D32(1) 本轮结果 |
| --- | --- | --- | --- | --- |
| AVD API 24, x86, 4 KiB | 24 | 5/5, 3.077 s | 8/8, 11.350 s | 未执行: 镜像未声明 Wi-Fi 硬件功能 |
| Sony G8441 / BH900ASK9E | 28 | 5/5, 3.076 s | 8/8, 57.773 s | 在线 completed, 运营商直连 |
| Redmi 12C / bek749scrwv4wo8h | 33 | 5/5, 5.745 s | 8/8, 14.343 s | 在线 completed, 临时 USB 代理 |
| Sony XQ-DQ72 / QV770340J7 | 33 | 5/5, 1.726 s | 8/8, 7.953 s | 在线 completed, 临时 USB 代理 |
| Xiaomi Pad / 968e9f18 | 35 | 5/5, 2.894 s | 8/8, 14.014 s | 本地 failed / DECISION_UNPARSABLE; 在线未执行, 无独立网络 |
| AVD API 37.1, x86_64, 16 KiB | 37 | 5/5, 5.878 s | 8/8, 21.130 s | 在线 completed, 临时代理, 正常系统快捷设置 |
| Sony XQ-AT72 / QV710AF65F | 31 | 未执行: 设备离线 | 未执行: 设备离线 | 未执行: 设备离线 |

用户于本轮补充 XQ-AT72, 预计在 2026-09-27 20:00 UTC+8 前上线. 已纳入同一兼容矩阵, 上线后补以上项目, 没有新增或拆分路线图阶段. 矩阵记录完成不等于所有设备的真实模型任务通过; Pad 的失败与两项未执行明确保留.

最终入口 30 项及插件 48 项全部通过. 六台实际广播请求均由安装的插件 launcher 创建身份凭据, 经生产接收器/连接所有者/代理附着, 无假身份或注入 transport. 312-1875 ms 是到 attached 的测量, 不等于首个 attaching 回调时间, 不据此评估 D41 的 500 ms 前台退路. 未覆盖 ColorOS. 结合前轮独立 conformance, P1.3 的剩余正向广播验收已完成.

## 驱动修正与失败留档

- 宿主旧断言把契约最初的最低宿主 5286 等同于安装插件的最低宿主 5289. 改为安装清单与 Binder metadata 一致, 并在支持范围内. 首轮每台 1 项断言失败保留, 不是产品附着失败.
- API 24/25 dumpsys 使用 `isReadyForDisplay()` 和 `Surface: shown` 判断实际已绘制的窗口, 没有新 Android 的 isOnScreen/isVisible 字段. TYPE_PHONE 的无障碍窗口枚举也不同; 在限定插件包的窗口查找后允许读取当前活动根. 前两种查找失败不作为悬浮窗产品失败.
- MIUI 通知点击的可点击祖先可能只展开通知组. 驱动现在等待真正的 ConfirmationActivity, 必要时点击当前可见行, 并继续验证真实确认/拒绝结果. 中间版本过早回退点击造成的失败和 G8441 换卡时离线的未执行日志均保留. 不以输入注入返回 true 冒充确认页面已打开.
- 复验只覆盖受影响用例/设备; 最终通过日志分别为 API 24 与 G8441 的 `plugin-instrumentation-4.txt`, Pad 的 `-3.txt`, 其余设备的 `-2.txt`. 宿主均为 `host-instrumentation-2.txt`.

## 真实模型 Wi-Fi 用例

在线目标均为用户配置的 Model8 / claude-fable-5-1, 本地为 Gemma 4 E2B IT. 每轮用新 caseId, 先建立 Wi-Fi 关闭的前置状态, Agent 从实际界面开启并观察状态, 完成后再用系统 wifi_on=1 独立验证. 不改网络密码/已保存配置. 普通系统设置动作使用 E4 驱动逐次确认, SystemUI 两次确认由操作员核对目标后回应 once. 成功轮保留完整单轮指标, 不拼接失败任务.

| caseId | 结果 | 步数 / 模型调用 | 时长 ms | 输入 / 输出 token |
| --- | --- | --- | --- | --- |
| p7-redmi-wifi-01 | failed / MODEL_FAILED, 未执行动作 | 0 / 1 | 2760 | ~9572 / 0 |
| p7-redmi-wifi-02 | completed | 10 / 11 | 80691 | 164249 / 829 |
| p7-g8441-wifi-01 | completed, 运营商直连 | 8 / 9 | 90913 | 129486 / 744 |
| p7-xq-wifi-01 | failed / MODEL_FAILED, 开关已开启但未完整收尾 | 6 / 8 | 44595 | ~111994 / 403 |
| p7-xq-wifi-02 | completed | 9 / 10 | 69729 | 153305 / 941 |
| p7-pad-local-wifi-01 | failed / DECISION_UNPARSABLE, 决策及两次修复未通过校验 | 1 / 3 | 46622 | 4266 / 196 |
| p7-avd37-wifi-01 | harness 未启动任务: 临时配置缺少文件访问授权 | 无模型调用 | 不计任务指标 | 不计 |
| p7-avd37-wifi-02 | failed / MODEL_FAILED, 开关已开启但未完整收尾 | 5 / 7 | 283853 | ~99501 / 605 |
| p7-avd37-wifi-03 | completed | 9 / 10 | 221957 | 147727 / 1105 |

`~` 表示包含估算 usage, 不代表精确 Provider token. 四个 completed 及 Pad 本地失败的 usage 均 estimated=false. 时间含操作员等待, 不能用来推导纯推理速度. Pad 的本地模型选择是本轮缺少独立网络时的补充尝试, 不替代 Model8 在线验收, 没有降低 schema/参数校验或增加修复额度.

Redmi, XQ 和 API 37 的成功复测使用 PC loopback + adb reverse 的受限 CONNECT 代理, 只允许 Model8 TLS 端点, TLS 保持端到端且不记录正文/凭据. 成功只能证明该路径下的真实 Agent 闭环; 不能作为运营商/模拟网络直连稳定性通过. MODEL_FAILED 仍是已知网络/Provider 路径限制, 本轮未变更其重试策略. API 37 沿用前轮已验证的正常快捷设置路径提示, 不更改系统或无障碍服务身份.

## 恢复与后续

四台真机屏幕超时/原有无障碍服务及临时通知授权已恢复, G8441 未进行锁屏. Pad 本地失败后由操作者恢复 Wi-Fi, 不计作模型成功. 临时代理/adb reverse 已撤销, API 37 的计费网络选项与宿主文件访问 AppOp 恢复原值. 保存真实任务历史, 不读取/更换模型密钥. 本轮没有购物/付款.

构建: 插件 debug/androidTest 通过, lint 0 errors / 6 既有提示; 宿主 androidTest 构建与测试源 lint 通过. 原 P7 安全清单, 全部新界面的字体/RTL/夜间/a11y 检查及 P8 仍由各自条目验收. 本项不宣称 P7/P8 gate 通过或发布许可. 后续确定性安全/UI 检查不需要 Redmi 或其他设备保留 SIM; 再跑在线 Wi-Fi 用例时才临时需要独立网络.

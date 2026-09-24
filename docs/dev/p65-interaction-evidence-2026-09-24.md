# P6.5 确认与询问验收

日期: 2026-09-24. 基线为 AI Agent `64f287a` / build 57; 本阶段版本为 1.0.0 / build 58. 宿主保持 `aeed8edcb9` / build 5293.

## 范围与行为

- 按原 P6.5 实现私有 `ConfirmationActivity`, 与任务台复用 `PendingCard`. 展示工具描述, 参数, 风险与剩余时间, 支持允许一次, 拒绝, 及确认门允许时的本次任务同一工具/同级风险授权. 支付, 强制确认和 memory_propose 不提供任务级放行.
- text / choice / confirm 询问共用回复卡片. 有 memoryKey 时显示 "记住此答案", 任务禁止记忆时禁用复选框并说明. 勾选后创建单独的 memory_propose 工具步骤, 显示实际作用域和值, 再经原 ConfirmationGate 确认. 默认使用允许的 global 范围, 仅允许当前预设时使用该预设. 用户步骤标记 source=user, 计入步数和工具调用预算, 不虚构模型调用. 拒绝/超时不写入, 原回答仍保留在任务历史.
- 前台任务台以内联卡片承接; 后台使用独立的高优先级通知频道 agent-interactions. 内容和动作入口都绑定特定 runId/requestId 的不可变 PendingIntent. 旧请求只显示结束状态, 不会回答新请求. 点击通知本身不批准操作.
- UI 可见性经私有 same-UID/oneway Binder 租约传到 :agent, 只抑制正在展示的任务/请求通知. 离开页面或 UI Binder 死亡后恢复后台提醒. 答复, 超时, 取消或任务终止后撤下提醒. script 交互归属的任务仍由 JS 承接, 插件界面无回答按钮且不发交互提醒.
- 剩余时间使用 runner 的同一单调时钟截止点. 截止点及可记忆作用域只加入私有 UI 投影, 不持久化或进入宿主/脚本事件契约. 页面重建保留草稿/复选框和当前请求, 不重置截止点. 确认窗口不导出, 使用 FLAG_SECURE 并排除最近任务; 锁屏通知使用通用公开摘要.
- 保留 P2.3 已实现的默认值: 确认 120 s, 询问 10 min, 并受任务剩余总预算限制. D25 原先笼统的 "默认 120 s" 补充为这两个值, 未更改运行时预算. 交互到期仍经 runner 产生 USER_TIMEOUT 观察, 由模型决定后续; 总预算先耗尽时由预算规则终止, 不强行追加模型调用.
- 原 P6.7 才引入悬浮球与 SYSTEM_ALERT_WINDOW. 本次准备共用卡片和请求入口, 悬浮球启用后的实际显示分支随原 P6.7 验收. 未增加, 分拆或前移路线图阶段.

## 验证

构建使用 JDK 21 Temurin 与仓库 Gradle wrapper. 运行 testDebugUnitTest, assembleDebug, assembleDebugAndroidTest, lintDebug 和 assembleRelease, 以及十语言文档生成器 --check.

| 项目 | 结果 |
| --- | --- |
| JVM | 435 项通过, 0 失败/错误/跳过 |
| debug / androidTest / release R8 | 构建通过, build 58 |
| lint | 0 errors / 6 个原有 warnings, 无新增 |
| 文档 | 10 语言 / 36 产物, 生成与漂移检查通过 |
| Sony G8441 / API 28 / arm64 | 57 项通过, 244.543 s |
| AVD_API_37.1_16K / API 37 / x86_64 | 57 项通过, 221.816 s |

新增 5 项 JVM 测试覆盖三类答案的独立记忆提议, 一次授权约束, 拒绝/超时不执行, 无 key 或工具关闭时拒绝记忆, 步数预算, 单调截止点和迟到回复. 新增 7 项 instrumentation 测试覆盖前台/后台往返, 实际通知入口, 旧入口失效, 真实 120 s 超时撤回通知, script 归属, 三种询问和 run scope 按钮, 多次回答分别确认, 重建后的记忆确认及阿拉伯语/夜间/2 倍字体的滚动布局.

UI 测试使用真实 Activity, 私有 Binder, runner, 前台服务, 历史和记忆存储; 仅模型/设备代理使用受控夹具. 设备代理拒绝实际设备操作, 不代表新增 Model8/Gemma 推理验收. 记忆夹具按独有 key 清理. 首轮测试修正了将 CheckBox 计入 Button 的筛选和系统通知节点搜索/点击方式; 通知测试改为遍历可见节点, 等待界面稳定, 优先使用通知行点击动作, 必要时注入触摸屏事件. API 37 首轮模拟器进程中断, 保留原数据冷启动后重测.

验收结束后恢复 G8441 屏幕超时, 两台设备的字体, 夜间模式和无障碍设置均与会话基线一致. 返回桌面并关闭本次临时启动的 AVD, 其余设备未操作.

本地日志位于忽略目录 build: p65-final-build.log, p65-final-test-build.log, p65-BH900ASK9E-final.log, p65-emulator-5586-final.log. 受控视图绘制图位于 build/p65-ui, 验证了完整参数, 允许/拒绝按钮和 RTL 长按钮换行. 这些图由测试绘制夹具视图, 未移除生产确认窗口的 FLAG_SECURE.

## 仓库与交付边界

只修改 AI Agent 插件. README/插件说明/changelog 的十语言源及生成产物同步更新. 宿主公开 AIDL, JS 参数和声明签名均不变; 官方文档, 离线文档, TypeScript 与 Ace 本轮无需接口同步. TypeScript 的用户 package.json 改动及 Ace 的用户 releases/ 保留. 未触碰 Rhino 同步成果, 未新增订单/付款, 未推送或发布.

下一起点为原 P6.6 设置/发行历史/更新, 再按原 P6.7 接入悬浮与系统入口. P7/P8 发布门禁尚未通过. 当前无需用户补充资料或设备.

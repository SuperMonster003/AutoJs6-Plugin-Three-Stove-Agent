# P9.1 Provider 原生工具验收

日期: 2026-09-25. 本次完成原 P9.1 的 3-Stone AI 模型条目. Provider 代码为 `AutoJs6-Plugin-Three-Stone-AI` 的 `4e887e8`, 版本 `1.2.0` 开发候选 / build `215`, 尚未发布.

## 行为与边界

- OpenAI 兼容 Chat Completions, Anthropic Messages, Gemini GenerateContent 支持工具定义, 流式参数拼接, 完整调用及结果续轮, 也覆盖非 SSE 的 JSON 响应.
- 同一轮的并行调用完整回送. Gemini 签名保留于原始 part, 缺失的 wire ID 生成唯一宿主可见 ID. 私有签名/思考块不成为工具参数或诊断日志.
- 原生工具经宿主交付与执行. Provider 核验工具名, ID, 完整批次, 16 轮与 32 个待处理调用上限, 输入/输出与累计数据额度. 后续 HTTP 请求重新检查网络策略和凭据; 显式输出 token 预算递减, 原始 deadline 不重置.
- 等待结果期间继续支持取消, 超时与 FD 关闭; 错误 ID 在读取 FD 前拒绝. 必需的 usage 缺失时不交付可执行工具. 请求最终 usage 累计各次模型调用, 不仅报告最后一次.
- 本地 LiteRT-LM 目标保持 `tools=false`. 原生工具请求暂不支持 `persistentSession=true` 和初始 tool-role 历史. Provider 的工具续轮使用同一个会话, 与持久 KV-cache 的 `ai.session` 机制分开.
- 本 Agent 的运行代码尚未切换原生循环. 仍需完成原 P9.1 插件条目以及 Wi-Fi/计算器两条路径对比. 原 P9.2/P9.3 未勾选.

## 实际验证

| 项目 | 结果 |
| --- | --- |
| Provider 全量 JVM | 373/373, 无跳过 |
| 新增 JVM 用例 | 三协议/会话 11 项, usage 累计 3 项 |
| API 24 / default x86_64 | 13/13, 2.025 s |
| API 37.1 / Google APIs x86_64 / 16 KiB | 13/13, 1.662 s |
| Debug, androidTest, R8 release, 签名归档 | 通过, 最后构建 2m 12s |
| Debug lint | 0 错误 / 97 警告 |
| ELF/ZIP 16 KiB 静态对齐 | Debug/release 全产物通过 |
| 十语言 Provider README/changelog | 生成与只读检查通过 |
| 归档 x86_64 release 安装和入口启动 | 两台通过, versionName 1.2.0 / versionCode 215 |

两台 Android 回归均为最终 debug 构建, 包含 11 项 NativeToolSessionAndroidTest 和 2 项现有 PluginDiscoveryAndroidTest. 新会话测试使用 AIDL proxy marshalling 与真实 Android FD, 后端和调用者身份是可控夹具. 生产代码继续使用包名, UID 和签名核验. 这些结果不代表真实云模型或跨 UID 宿主到 Provider 的端到端测试.

额外尝试把 debug instrumentation APK 用于 R8 release 时, 两台运行器均因 `NoClassDefFoundError: kotlin.jvm.internal.Intrinsics` 初始化失败. 原因来自测试 APK 对未优化 target runtime 类的依赖; release 自身入口在普通启动下正常. 失败记录保留, 不计为 release Binder 成功. 未为测试放宽生产 keep 规则; 正式发布前需 release 匹配的外部/Binder 验收.

完整实现说明, 协议官方参考和产物 SHA-256 在 Provider 仓库 `docs/dev/p91-native-tools-evidence-2026-09-25.md`. 私有日志在该仓库 `build/p91-model-private/`. 三个本地候选 APK 的 CRC32 为 universal `56B3E5C9`, x86_64 `F1A5BABA`, arm64-v8a `91B9A1BA`; 归档来自提交前已验证工作区, manifest 如实保留当时 revision 和 dirty 状态.

## 下一步和设备

先完成原 P9.1 Agent `ModelClient` 原生工具循环, 保留不支持 tools 的目标走结构化 JSON. 两条路径共用校验, 用户确认与步骤记录. 完成后再安排原 Wi-Fi/计算器实测对比, 并补齐 release 级外部验收.

本轮未使用真机, SIM, 真实模型接口或订单/付款. 接下来的插件开发也不需要 SIM; 在线 Wi-Fi 对比执行时再临时使用独立网络即可. `QV710AF65F / XQ-AT72 / Android 12` 仍记录为离线待补, 按用户预计 2026-09-27 20:00 UTC+8 前上线后安排.

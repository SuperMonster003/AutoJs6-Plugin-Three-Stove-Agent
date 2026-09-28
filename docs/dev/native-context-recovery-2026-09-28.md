# Native tool context recovery

## Observed failure

- Device: QV770340J7 / XQ-DQ72 / Android 13 (API 33).
- Installed Agent: 1.2.0 / build 143. Selected model label: AIGoCode.
- The existing task stopped after six tool steps, including two screen observations, in 42985 ms. The task detail reported six model calls, 51257 tokens and `LIMIT_EXCEEDED` with the context input limit explanation.
- After the debug upgrade, the five retained history entries included three `LIMIT_EXCEEDED` failures: two after six tools and one after nine. Only the entry above retained the explicit input-limit explanation; the older two used the generic failure message.
- The private task text, contact identity and message content are deliberately excluded from this repository, including regression fixtures.

## Cause and change

`ContextCompiler` bounds initial requests and can discard older observations and summarize history. Native tool continuations bypassed it: `NativeModelInvocation` accumulated the initial prompt, model output and every tool result until the input limit was exceeded. The run then terminated even when a newly compiled context would fit. A negotiated native round limit was also treated as the end of the conversation without preparing another bounded request.

The native continuation now distinguishes a valid completed batch that needs a new context from invalid or oversized result envelopes. At that boundary, `AgentRunner` closes the suspended host request, settles outstanding usage and recompiles the completed journal with the latest observation. The next request keeps native tools and receives a new model budget reservation. Already executed tool calls are not replayed by the runner.

The original model deadline, total task budget, decision repair allowance, confirmation state and loop evidence remain in force. A failure reported during closure remains a failure. Old request callbacks cannot terminate the replacement request. Actual broker failures do not trigger a JSON fallback after context recovery.

## Validation

- Targeted native model and runner tests: 39 passed, including seven new context recovery regressions. They cover large observations, negotiated round exhaustion, no action replay, original deadlines, budget admission, confirmation and late callbacks, no JSON fallback after recovery, and the shared repair allowance.
- Full JVM suite: 627 tests, zero failures or errors, one existing opt-in performance test skipped.
- Full Android instrumentation on the isolated `Three_Stove_Agent_Conformance_Context_33` AVD (API 33): `OK (129 tests)` in 428.015 seconds, 125 passed and four opt-in tests skipped (one local MCP interoperability check and three screenshot capture checks). The fake host was prepared through the guarded conformance script and installed only on this disposable emulator.
- `assembleDebug`, `assembleDebugAndroidTest`, `lintDebug`, `assembleRelease` and the fake-host debug APK build passed with one worker and Temurin platform properties. Lint reported zero errors and six existing warnings. Both application APKs have version 1.2.0 / build 152.
- `appendDigestToReleasedFiles` passed. The release directory contains the single expected APK, `autojs6-plugin-three-stove-agent-v1.2.0-70817961.apk`; its CRC32 is `70817961` and its bytes match the build 152 release output.
- Markdown generation and `--check`: all 10 languages and 36 artifacts passed. A scan of 602 project text files, including the new regression fixture and this note, found no private test text or contact identifier.
- The debug APK was installed over build 143 on the target device without uninstalling or clearing data. The existing history, selected model and full-access setting were retained.

The local build initially stalled while Gradle enumerated file systems for its watcher; `--no-watch-fs` avoided that stall. A subsequent AAPT2 daemon startup timeout matched the existing Windows build issue recorded in the roadmap; the single-worker retry passed. These are local invocation changes, not project build configuration changes.

The maintainer confirmed that other tests or manual operations were using the physical device, so further device interaction stopped. The first post-upgrade task observed there belonged to a concurrent system-settings test; the navigation check prepared for this repair was never started. Its result is excluded from acceptance evidence. Another test's UI Automation connection temporarily suppressed the ordinary accessibility services; those services reconnected after the connection closed. The original private task and any message-send check were not executed. Full physical-device navigation acceptance still requires an exclusive device window.

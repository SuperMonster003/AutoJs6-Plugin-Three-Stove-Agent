# P7 performance baseline

This records the original P7 performance item. It does not add or split a roadmap stage. Host APK: AutoJs6 6.8.0 / 5296. Agent APK: 1.0.0 / 65. The host changes in this item are instrumentation and documentation only; public APIs, AIDL/AARs and runtime dependencies are unchanged.

## Method and limits

Workstation: Windows x64, Intel Core i5-12400, 6 cores / 12 logical processors, Temurin JDK 21. The device test uses the private API 37.1 AVD with 16 KiB pages, 1080 x 2424 display and SwiftShader. Real models, carrier data, physical devices, commerce and payment are not involved.

Performance assertions use warmed p95, with the first-use sample, mean and maximum reported separately. These are repeatable baseline measurements, not hard real-time guarantees. CPU scheduling, GC, emulator load, accessibility caching and device hardware affect individual measurements. The final isolated JVM measurement ran after Gradle build/lint work finished; it did not overlap the device benchmark. The full correctness run also contains the opt-in benchmark and is recorded separately below.

## JVM step overhead and implementation

`StepPerformanceTest` uses production `ContextCompiler`, `DecisionParser` and `DecisionValidator`. It covers remote structured/local formats, English/Chinese goals and 0/32 previous steps, with an observation derived from 200 synthetic nodes. Asset/catalog/format setup and observation preparation are outside the measured interval, as are assertions and result consumption. Each case records its first invocation, warms 100 steps, and measures 300 individual compile/parse/validate operations. Returned messages must fit the real input budget and the parsed tool must remain `ui_dump`.

The initial implementation exceeded 20 ms for the local Chinese 32-record case: p95 23.5719 ms, p99 27.2166 ms, maximum 35.5743 ms. Repeated history trimming rebuilt identical system prompts, observation compaction and recent history messages. `ContextCompiler` now reuses those invariant fragments within one compilation. Truncation order, byte accounting, latest observation, format fallback, rule content and validation remain covered by correctness tests. Caches are local to the call; an added regression alternates observations, guidance and format on the same compiler and verifies prior returned messages remain unchanged.

Final isolated results, milliseconds:

| Target | Language | History | Mean | p95 | p99 | Maximum | First invocation |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Remote | en | 0 | 1.041 | 1.628 | 1.823 | 2.855 | 15.339 |
| Remote | en | 32 | 4.686 | 5.971 | 7.270 | 7.941 | 6.813 |
| Remote | zh | 0 | 1.029 | 1.329 | 1.736 | 2.125 | 9.538 |
| Remote | zh | 32 | 6.768 | 7.961 | 9.451 | 11.014 | 7.466 |
| Local | en | 0 | 1.082 | 1.758 | 2.001 | 2.593 | 13.511 |
| Local | en | 32 | 4.300 | 5.589 | 7.007 | 7.693 | 4.402 |
| Local | zh | 0 | 0.816 | 1.259 | 1.517 | 1.678 | 1.074 |
| Local | zh | 32 | 5.220 | 5.900 | 6.777 | 8.417 | 5.140 |

All 2,400 warmed samples in this isolated run were below 20 ms. The full correctness run overlapped host lint/build activity and had higher outliers: the slowest p95 was 19.9819 ms and the largest individual sample was 47.5855 ms. That run is not silently substituted for an idle-machine measurement and is not a universal latency guarantee. The benchmark is opt-in to avoid making normal correctness CI depend on workstation speed.

## Real 200-node round trip

The host's `AiAgentPluginRoundTripTest#performanceBaselineForTwoHundredNodesAndOneTaskMemory` binds the installed production plugin and the real capability broker. Its existing debug Activity contains a 20 x 10 grid of real text controls. All 200 control texts change before every read. Both the raw host response and the next model request must report exactly 200 accessibility nodes (including structural nodes), and the model request must contain the current frame's first cell.

One task performs 35 `ui_dump` calls: one first-use sample, four more warmups and 30 measured calls. The full interval starts before delivering the deterministic tool decision and ends when the next model request arrives. It includes decision parsing/validation, both Binder directions, host accessibility traversal, plugin observation processing and next-context compilation. Normal query grants apply. Fixture mutation, 120 ms pacing and memory probes are outside the interval. The fixed model performs no inference and reports zero usage; it does not consume a real model's token budget.

| Measurement | Mean | p50 | p95 | Maximum of measured samples | First use |
| --- | ---: | ---: | ---: | ---: | ---: |
| Full plugin round trip, ms | 188.489 | 189.438 | 218.337 | 220.059 | 208.409 |
| Host dispatch portion, ms | 91.679 | 93.876 | 114.458 | 116.738 | 111.301 |

The final fixture passed in 16.064 s. An earlier fixture changed only one cell and measured p95 142.133 ms, with a first-use value of 309.723 ms. That less demanding result is retained in the local log, but the table above uses the stronger all-cells-changing fixture. First-use variability remains observable; there is no claim that every future cold call finishes within 300 ms.

## Single-task memory

The Agent is force-stopped before binding, and the process PID must remain the same during the task. `/proc` gives RSS and its kernel high-water mark; `dumpsys meminfo` gives PSS. Ten probes include the pre-task baseline, every fifth model boundary and final completion.

| Metric | KiB | MiB |
| --- | ---: | ---: |
| Baseline PSS | 28081 | 27.423 |
| Final PSS | 35439 | 34.608 |
| Sampled peak PSS | 37286 | 36.412 |
| Baseline RSS | 135964 | 132.777 |
| Final RSS | 145736 | 142.320 |
| Kernel peak RSS | 151340 | 147.793 |

PSS is a sampled peak and can miss shorter spikes. RSS high-water includes plugin startup, persisted history and task-service overhead, rather than isolated allocation by one algorithm. Host shared process memory and real Provider/model inference memory are outside these numbers. The kernel peak exceeds baseline RSS by 15376 KiB; this difference is not a retained-memory or leak measurement.

## Store write amplification

`StoreWriteAmplificationTest` opens full-capacity stores (200 history records and 500 memories), updates one entry three times, and verifies atomic file replacements. Each pre-write snapshot includes file identity when available, a sentinel mtime and content digest, so identical-content rewrites of unrelated files are also detected. Updated values are decoded from disk and no temporary/backup files may remain.

| Store | Unchanged entries per update | Changed files | Logical payload written | Whole collection before first update |
| --- | ---: | --- | ---: | ---: |
| History, 200 records | 199 | One record and rebuildable index | 16155 bytes | 459114 bytes |
| Memory, 500 entries | 499 | One entry | 169 bytes | 140892 bytes |

History still rewrites its small metadata index, not all run bodies. These byte counts describe replaced file payloads, not filesystem journaling, flash write amplification or total lifetime writes. Existing store behavior met this requirement; no store implementation was changed. The first test attempt assumed a non-null filesystem file key; Windows returned null, so the fixture now also works there using its sentinel mtime and digest.

## Validation and reproduction

- Final full Agent JVM run: 473 tests, 0 failures/errors/skips, including the opt-in benchmark and two storage tests. The initial cache-isolation test incorrectly expected the full prompt's format spelling in the compact prompt; the assertion was corrected to the existing compact `Degraded=true` contract, with the isolation and byte-budget assertions retained.
- Agent debug/test APK assembly and native-library absence checks pass. Agent lint: 0 errors / 6 existing warnings. Ten-language Markdown generation/check: 36 artifacts. No dependency change, so Release/R8 was not rerun for this item.
- Host instrumentation assembly and Android-test-source lint analysis pass. Whole-host lint remains incomplete from the earlier grant work; this item does not close that gate.
- Final full Agent Android regression: 73/73 passed in 264.418 s, including the real confirmation timeout, hostile input, history recovery and floating controls.

Run from the plugin repository with the configured repository JDK:

```powershell
$env:AUTOJS_AGENT_PERFORMANCE='true'
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:testDebugUnitTest --tests '*StepPerformanceTest' --rerun-tasks
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' :app:testDebugUnitTest --tests '*StoreWriteAmplificationTest'
```

The XML `system-out` contains `P7_STEP` and `P7_STORE` JSON metrics. Full JVM XML is preserved locally in `build/p7-performance-full-jvm`; isolated results and raw logs use `build/p7-performance-*`. Host AVD commands and measurement boundaries are documented in its `test-apps/ai-agent-conformance/README.md`; its instrumentation emits `P7_PERFORMANCE` JSON. Ordinary tests need no performance environment variable. The environment switch is also a Gradle test input so toggling it invalidates a previously skipped/up-to-date test result.

The next original P7 item is power/idle service behavior. Full fake Agent/fake host conformance, the six-device matrix including QV770340J7, security/UI acceptance and P8 remain separate pending items. No additional user data or manual action is required for the next deterministic item, and Redmi carrier service is unnecessary.

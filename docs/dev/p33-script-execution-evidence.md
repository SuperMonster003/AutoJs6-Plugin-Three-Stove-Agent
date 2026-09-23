# P3.3 registered script execution and result evidence

Date: 2026-09-23. Plugin: 1.0.0 / 33. Minimum host: AutoJs6 6.8.0 / 5287.
The four original P3.3 items are implemented. The execution-side result/context
item already listed in P5.1 was necessary for the P3.3 public result round trip
and is also complete. No roadmap item was added, split or discarded. Task
creation, AgentRun and the workbench keep their original P5/P6 scope.

## Execution and cancellation

- ScriptInvoker consumes the same immutable PreparedScript used by confirmation.
  It passes the inspected canonical path, validated effective parameters, run ID,
  preset, timeout and expected manifest through agent.execRegistered. The host
  compares the current catalog entry with that snapshot before dispatching;
  changed registration produces SCRIPT_NOT_REGISTERED. Catalog invalidation
  still follows the existing registration metadata/mtime/length rules.
- Each execution gets a random invocation UUID owned by its authenticated link.
  engines.stop({agentInvocationId}) can arrive before an engine ID exists. A
  bounded host registry retains early cancellation for 310 seconds, and the
  dispatch/onStart gates prevent a cancelled invocation from evaluating source.
  IDs from another link cannot stop this link's scripts. Numeric engines.stop
  behavior is unchanged. No AIDL transaction or version changed.
- The runner's operation deadline is bounded by the registered timeout and task
  budget, with a 300-second maximum. The host reserves 750 ms of that deadline
  for dispatch/cleanup and allows up to 500 ms to observe forceStop completion.
  Host timeout produces SCRIPT_TIMEOUT; the plugin also sends the owned stop.
  Runner timeout, cancellation, malformed replies and transport failures likewise
  request stop. Late/duplicate callbacks cannot resume or settle the operation
  twice. The host's finished flag remains explicit when termination is unobserved.
- Observations retain outcome, result, resultReported, executionId, finished,
  a stable error code and the newest 40 console text lines. Multiline messages
  are split before applying that limit. Console JSON has an 8 KiB content budget
  (plus array delimiters), each line is bounded, string arguments are masked,
  and credential fields/assignments and bearer tokens are redacted. Results also
  redact credential fields; known password input is redacted before model use.
  Error messages do not echo private source, paths or exception text.
  Credential patterns are removed before argument replacement, and multiline
  arguments are removed before console messages are split into text lines.
- Console capture still uses the host's existing process-wide ID window,
  identified by consoleCaptureMode:global-window. Other concurrent scripts may
  contribute entries; this change does not claim engine-exclusive log capture.

## Reporting and terminal result

- ai.agent.context() returns a fresh {runId, parameters, presetName} snapshot.
  ai.agent.result(value) serializes JSON into the host-owned 64 KiB slot without
  ending execution. The last accepted report wins. resultReported distinguishes
  an explicit JSON null from no report. The public helpers are available in host
  build 5287; ordinary scripts have no context and reporting returns false.
- The model must still produce done and a summary. Only one executed script,
  a reported result and no other executed operation tool allow AgentResult.script
  to contain {id, path, executionId, result}. Script catalog queries, progress,
  questions and confirmations do not disqualify it. Cancellation before done,
  no report, multiple scripts and mixed operations omit the shortcut.
- Terminal results remain within 24 KiB. If the script result must be shortened,
  its identity fields remain and resultTruncated:true accompanies a bounded
  result preview. A large summary does not falsely mark a small script result.
- The host sample catalog exposes agent/统计剪贴板字数.js and the
  agent/清理下载目录旧安装包 project. The cleanup example requires days, defaults to
  dryRun:true, restricts paths to Downloads or its children, handles only direct
  regular APK/APKS/XAPK files, and reports counts plus at most 50 file names.

## Verification

| Check | Result |
| --- | --- |
| Plugin JVM | 288/288, including 18 new invoker/result-flow cases |
| Host JVM | 3182 discovered, 3176 passed, 6 existing conditional skips |
| New host invocation registry | 6/6, including early stop, isolation, disconnect, replay and bounds |
| Plugin Android, each API 24 / API 37 | 23/23 |
| Host execution Android, each API 24 / API 37 | 12/12 |
| Host/plugin round trip, each API 24 / API 37 | 12 passed, 1 existing optional Wi-Fi skip (runner reports 13 tests) |
| Repeated three-run startup scenario | API 24: 6/6 repetitions; API 37: 3/3 repetitions, followed by the full round-trip suites |
| Plugin builds | debug, androidTest, release/R8, lint; 0 lint errors, 6 existing warnings |
| Host builds | app debug, androidTest, three release API modules together |
| Plugin docs | 10 languages / 36 generated artifacts, freshness check passed |
| Public docs | full generation/sync and freshness check, 138 modules |
| TypeScript | aj6dts.bat -Publish local generation/copy and positive/negative execution API smoke passed |
| Ace | four completion verifiers, browser completer tests, LSP generation and runtime verification passed |
| Offline Docs | 2 JVM tests, debug assembly, lint (0 errors / 27 existing warnings), 138-page normalization and 25-artifact markdown checks |

Both devices were private read-only AVDs: API 24 x86 with 4 KiB pages and API 37
x86_64 with 16 KiB pages. Only the task's emulator serials were operated. No
physical devices or real model providers were used.

The scripted-model file and project runs each perform missing-parameter feedback,
an input question, one-call confirmation, real Rhino execution, structured result
observation and model done. The file verifies that context mutation cannot alter
execArgv or future snapshots. The project uses the shipped cleanup source and
only a newly created fixture under Downloads; it preserves a text file and a new
APK. API 37 verifies deletion of the old APK. API 24 emulated storage rejects
setLastModified, so the fixture verifies retention of that still-new APK instead.
Both platforms verify an acknowledged running engine is absent after timeout or
task cancellation. Separate host tests reject early cancellation and a changed
manifest before a source marker can be written.

Final APK verification exposed an intermittent API 24 startup failure in the
existing three-run catalog scenario. A retired foreground service's delayed
onDestroy completed the next startup's pending callback with false, producing
CAPABILITY_DENIED before any model call. Destruction now fails pending startup
callbacks only when that instance still owns the running service; normal idle
shutdown has already released ownership. The original failure log is retained
locally alongside the verification output. The three-run scenario then passed
six repetitions on API 24 and three on API 37, followed by both complete
round-trip suites, with no retries inside the tests.

Useful commands (JDK 21 Temurin was selected for Gradle):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
py .python/generate_markdown.py --check
adb -s <private-avd> shell am instrument -w -r -e class org.autojs.autojs.engine.AgentRegisteredScriptExecutionTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
adb -s <private-avd> shell am instrument -w -r -e autojs.agent.plugin true -e class org.autojs.autojs.core.plugin.agent.AiAgentPluginRoundTripTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
adb -s <private-avd> shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
```

The host commands run from the host repository; the plugin commands run here.
Raw local output is in ignored build/p33-*.log files. The Ace completer check used
an ignored copy of the existing host tools with its native bridge path adjusted
to the plugin's migrated package; assertions were retained. Full documentation
generation also synchronized previously committed mail source changes whose
HTML/JSON had not yet been regenerated.

## Commits and remaining acceptance

| Repository | Implementation commits |
| --- | --- |
| AutoJs6 | 42b82ca494 |
| AI Agent | ac62f30 (invoker), 78075b5 (terminal result), followed by this redaction/startup regression and evidence commit |
| Documentation | 3c50244 |
| TypeScript Declarations 4.20.0 | 08a89c7 |
| Offline Docs 6.8.0 / 55 | d13f179 |
| Ace Editor 1.12.1 / 110 | f10e0fa |

All three API AARs were assembled together from the committed host 42b82ca494,
copied and rehashed. The hashes are unchanged because the API modules did not
change; provenance and the host minimum were updated consistently.

This is E0/E1 evidence. Original D32(3) E4 acceptance still requires a physical
device and an online model, including a parameter question and confirmation. It
has not been run or marked complete. ColorOS activation and other script engines
are not established by these Rhino AVD checks. P4.1 remains the next implementation
section, while the original E4 and P7/P8 acceptance gates stay in place. No remote
push, package publication or release was performed. Pre-existing Ace releases/
and an unrelated declaration package publishConfig change were preserved.

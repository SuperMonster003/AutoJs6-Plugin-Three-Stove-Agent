# P7 follow-up: Sony XQ-AT72 / Android 12

Date: 2026-09-26 (UTC+8). Device: QV710AF65F, XQ-AT72, API 31,
arm64-v8a. The device became available before its previously estimated date.
This closes its deferred P7 installation/entry and plugin interaction checks;
the separate four-case UI layout audit also completed later in this follow-up.
Real-model tasks are recorded separately below. The original roadmap stages
are unchanged.

## Installed candidates and provenance

The device initially had host 6.8.0 / 5281, 3-Stone AI 1.1.3 / 190 and HiPER
Calc Pro 10.4.2 / 217, with no AI Agent. The following officially signed
development candidates and matching instrumentation APKs were then installed
without clearing application data. No fake host or fake Provider was installed.

| Component | Candidate used for these checks | APK SHA-256 |
| --- | --- | --- |
| AutoJs6 host | 6.8.0 / 5297, arm64 debug with official signer | `4112189a49177ebe187ed8fea04fcb83dfc8fa74bdbbf5b83eaefd486e7c7059` |
| AI Agent | 1.1.0 / 87, debug with official signer | `0c3090e31d052650de010b072be0e7f8502ff2c9912c3224327380d2d1376f38` |
| 3-Stone AI | 1.2.0 / 216, arm64 R8 release candidate | `d537a7ff8681321d0b769536939cab55223b34028461bb05e604dbbd36a6597c` |

The Provider APK's binary package/version metadata, SHA-256 and APK Signature
Scheme v2 verification were checked. Its public signing-certificate SHA-256 is
`31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
The hash matches the previously verified P9.2 arm64 candidate. This evidence
does not identify these unpublished development files as released versions.

## Results

| Check | Result | Duration |
| --- | --- | --- |
| `AiAgentEntryDeviceTest`, explicit `autojs.agent.entry=true` | 5/5 passed | 1.705 s |
| `AiAgentPluginContractTest` and four Workbench interaction methods | 8/8 passed | 8.878 s |
| `AiAgentRealModelE4Test#publicCatalog`, explicit `autojs.agent.e4=true` | Completed, zero advertised targets | No generation performed |
| Four UI layout audit cases, Arabic RTL / dark / font scale 2.0 | 4/4 passed, 28 states with zero layout issues | 12.598 s |

The installed Agent launcher created the real identity credential and reached
the production host attachment receiver. Its recorded attachment time was
413 ms; this is time to attached, not time to the first attaching callback.
The host batch also checked explicit launcher discovery, rejected foreign
identity requests, and rendered discovery/attachment guidance.

The plugin batch covered Wake and service metadata/discovery, launcher and
caller boundaries, inline reply and recreation, offline detail access,
background notification entry with denial invalidation, floating-window
expansion/dragging/draft retention/stop, and question/confirmation-card flags.
Controlled model replies and device-action fixtures were used for this UI
batch. These tests do not establish real model decision quality or real device
action success. Every batch was accepted only after its exact JUnit test count
and successful terminal marker were present.

The public catalog call bound the official Provider through the production
host model broker and returned `completed` with `targets: []`. It did not read
credentials, generate a model response or run a task. The Provider was therefore
reachable, but advertised no usable model target for the planned online cases.
The initial preflight also found AutoJs6 absent from enabled accessibility
services. Before real calculator/Wi-Fi acceptance, the device needs a configured
online model target and manually enabled AutoJs6 accessibility, followed by a
fresh public catalog and service readiness check.

The user subsequently confirmed both prerequisites ready. A fresh public
catalog advertised one configured, available Model8 target with native tools
support and no vision capability. The Provider's visible model selector later
confirmed `claude-fable-5-1`, without access to private model credentials.
Gemma 4 E2B IT was still downloading and was not required for the
online cases. Enabled accessibility was checked again before real-task work.

The separate UI audit used read-only copies of the installed debug Agent and
instrumentation APKs, verified against the recorded hashes, because a parallel
build had replaced the local output files. It covered Workbench/history/detail,
preset and memory editors, script roots, settings and dialogs, release/legal
pages, question/confirmation states and floating-window states. Its independent
baseline changed only font scale from 1.0 to 2.0 and restored 1.0 afterwards;
screen timeout remained 600000 ms throughout. All 28 measured states had zero
reported layout issues. No network or model setting changed in that batch.

## Network and cleanup boundaries

Wi-Fi had validated Internet during preflight. A connected, validated cellular
IMS network was also present, but did not advertise the INTERNET capability.
With Wi-Fi active this can be standby behavior; it is not proof that mobile
data is unavailable. Independent cellular Internet while Wi-Fi is off was
unverified in the initial compatibility batch. That batch changed no Wi-Fi,
proxy, APN, SIM, mobile-data or saved-network setting, and did not establish a
Wi-Fi real-model success. Later real-task work is recorded separately below.

The driver saved the original screen timeout of 120000 ms and temporarily
requested 1800000 ms. At cleanup the observed value was 600000 ms, so the
driver refused to overwrite a value it could not attribute to its own write.
The coordinating process confirmed it did not change that setting. Focused
source searches found no timeout setter in the exercised host/plugin tests or
the catalog driver, while SettingsProvider identified only `com.android.shell`
as the current setting's writer. This does not identify the responsible caller.
The 600000 ms value was therefore preserved and the cleanup limitation remains
explicit; full timeout restoration is not claimed. Font scale was unchanged.

No user screen contents, accounts, subscriber identifiers, IP addresses, SSIDs
or credentials are included here. Per-device APK hashes, exact commands, JUnit
logs, the empty public catalog and sanitized summaries remain in ignored
`build/p93-agent-private/qv710af65f/`. The serial-limited helper does not install
APKs or change networks. The UI batch's separate logs, exact installed APK
copies and restoration receipt are in its `ui-layout-20260926-115052/` child
directory. These compatibility results refer to the debug candidate above;
the subsequent R8 installation and real tasks have their own evidence below.

## Final R8 installation and online calculator

The exact final official-signed R8 Agent 1.1.0 / 87 replaced the debug variant
without clearing data. Archive `autojs6-plugin-ai-agent-v1.1.0-ee852c93.apk`
has SHA-256 `835227c3743400fe881cba2451d51e98d88d6d69481f68adac80fefd6ecea2b8`.
Its cold launcher start completed successfully in 391 ms (398 ms total wait).
The host and Provider artifacts remained as recorded above.

`p93-qv710-native-calc-01` used the real Model8 / Fable 5.1 target through native
tool calls, with the goal of computing 12 * 34 in HiPER Calc Pro and reading
408. It ended **partial / BUDGET_EXCEEDED**, not completed:

| Steps | Tool calls | Model calls | Total tokens | Task duration |
| --- | --- | --- | --- | --- |
| 12 | 10 | 14 | 302616 | 99993 ms |

The visible calculator still showed its prior expression and result rather
than 12 * 34 = 408. The accessibility observations lacked the display contents
and exposed incomplete button labels, including `<U>`-style markers; one
NODE_NOT_FOUND was recorded. This suggests an observation limitation in this
app, but does not prove that it alone caused the token exhaustion. No budget
increase or repeated attempt was used to replace this failure. The successful
100.638 s instrumentation wrapper result only means evidence collection
finished; it does not turn the task's partial result into a pass.

The model download was left undisturbed during this calculator run. The user
then confirmed Gemma 4 E2B IT downloaded/imported and authorized the Wi-Fi
checks. That confirmation removed the download conflict; it is not a local
model inference acceptance result. Private E4 logs are retained under
`build/p93-agent-private/qv710af65f/e4-final-20260926/`.

## Online Wi-Fi and generated-source results

The Provider's online metered-network setting was originally false. After the
user confirmed the large model download/import had finished, this setting was
temporarily enabled through the ordinary Provider UI for the explicitly
authorized SIM-backed Wi-Fi tests. A stable preflight confirmed INTERNET and
VALIDATED on the same CELLULAR network with Wi-Fi off, excluding the separate
IMS network, before the native sample. No proxy was used.

| Case | Result | Steps / tools / model calls | Input / output tokens | Task duration |
| --- | --- | --- | --- | --- |
| `p93-qv710-native-wifi-01` | completed | 8 / 7 / 10 | 156629 / 602 | 50879 ms |
| `p93-qv710-json-wifi-01` | failed / DECISION_UNPARSABLE | 1 / 0 / 3 | 34219 / 0 | 19178 ms |
| `p93-qv710-native-source-02` | completed | 2 / 1 / 2 | 16008 / 152 | 29473 ms |

All four real tasks, including the calculator, report `estimated=false` usage.
The calculator's input/output split is 301483 / 1133. Native Wi-Fi includes
actual native tool-call events and ends with independent system-settings XML
`checked=true`, a corresponding screenshot and `wifi_on=1`. JSON has three
model-completed events with no parsable text and no tool calls; independent
verification still found Wi-Fi off before operator restoration. The failure is
preserved, and no budget increase or repeated sample replaced it.

The JSON sample has a precondition-ordering limitation: its five-second
cellular check did not yet show VALIDATED, but the command sequence proceeded
to start the task without stopping on that failed check. The retained host
precheck timestamp is 12:02:33.340 UTC+8; the device task-start timestamp is
12:02:38.240; the host's supplemental check is 12:02:52.252. The latter found
Wi-Fi still off and INTERNET plus VALIDATED on the same CELLULAR network,
before any tool action. These timestamps come from two clocks, not a calibrated
latency measurement. Startup validation is not claimed, and the later check
does not prove the cause of the empty model output. The native/JSON Wi-Fi
comparison therefore remains pending rather than treating the native pass as
success for both paths.

The first dynamic-source attempt, `p93-qv710-native-source-01`, was rejected by
`startRun` with INVALID_REQUEST while Generated scripts was still globally
disabled. It took 0.632 s and created no run ID, model call or source execution.
Per-run tool groups can only narrow the globally enabled set. This rejected
preparation attempt was retained. The operator then temporarily enabled the
group through the normal Agent settings UI and ran the single real-model case
`p93-qv710-native-source-02`, limited to arithmetic and result submission:

```javascript
ai.agent.result(12 * 34);
```

The confirmation was `sensitive` with `allowRunScope=false`. The operator read
the entire pending source, verified that it contained only the requested
arithmetic/result call, recorded the review, and then sent `scope=once` through
the E4 reply path. Before approval, the run was waiting for confirmation and
had zero journal steps. Afterwards, exactly one `script_run_source` execution
was recorded, with `parseMode=NATIVE_TOOL`. The actual host Rhino execution
returned `outcome=success`, `finished=true`, `resultReported=true`, `result=408`
and no error. The complete source survives in the private step journal. This
real-model smoke is separate from the deterministic Android source-card tests.
No generated script file was saved, and the group was not left enabled.

At 12:08:33 UTC+8 the final restoration receipt verified:

- Wi-Fi on, with INTERNET, VALIDATED and NOT_METERED on the Wi-Fi network.
- Provider online metered-network setting restored to its original false.
- Generated scripts restored to false, all ten tool-group choices identical
  to their pre-test values.
- Screen timeout 600000 ms and font scale 1.0, preserving the current baseline
  rather than overwriting it with the earlier batch's 120000 ms value.
- Enabled accessibility service set unchanged from the online-test baseline,
  taken after the user's manual setup; AutoJs6 remains enabled.
- No proxy, APN, DNS, VPN or download-setting changes, and no shopping,
  account, order or payment actions.

The receipt is `acceptance-summary.json`, with `online-restoration.json`,
the JSON preflight timeline and per-case source-review/journal evidence in the
private E4 directory. The SIM is no longer needed for this completed batch.
A future online Wi-Fi test again needs independent connectivity during that
case; ordinary implementation and local-model work do not need a dedicated SIM.
Gemma's successful download/import is user-confirmed preparation, not a claim
that local inference was tested on this device in this batch.

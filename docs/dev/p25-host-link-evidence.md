# P2.5 host link and foreground service evidence

## Implemented scope

The original P2.5 items retain their order and scope. AiAgentPluginService now
implements the released IAiAgentPlugin contract in the :agent process. HostLink
owns the model broker, shared capability broker, serial runner, bounded queue and
event callbacks. Every host entry checks the calling UID, installed AutoJs6
package, minimum version and current signer set. The plugin's own UID cannot
attach as the host. The UI uses a separate non-exported, same-UID local service.

- Controls validate closed JSON shapes and enqueue work. Model discovery,
  capability calls, descriptor reads, disk writes and callbacks use separate
  bounded workers. Control payloads are limited to 32 KiB; a stalled control FD
  has a 75 ms read deadline. Events are oneway and limited to 32 KiB.
- One task is active and eight can wait. Cancellation, detach, broker death and
  link callback death settle admitted work without replaying it. Run observer
  death leaves the owned task running and retains its private step journal.
- The private AtomicFile archive keeps up to 20 terminal tasks plus admitted
  work. Startup loads history asynchronously; unfinished historical tasks become
  blocked/HOST_UNAVAILABLE. New tasks admitted during loading are preserved.
  getRun returns at most 50 recent steps within 32 KiB and marks truncation.
- The launcher sends an explicit host attach request with a request ID and
  immutable PendingIntent identity proof. It observes the real attached link,
  waits up to 15 seconds, then shows host guidance. Basic input/confirmation
  dialogs consume the existing runner events; the complete task UI remains P6.
- The task foreground service promotes before model/tool preparation and stops
  after the last active task settles. API 24-25 use startService followed by
  startForeground; newer versions use startForegroundService and API 34+ uses
  the specialUse type. Notifications show a private goal summary, step/progress,
  Stop and View controls. Service restart never resumes a task automatically.

The three staged API AARs were built together from host 5ae754e641, AutoJs6
6.8.0 / 5285, using their release variants. Their SHA-256 values are recorded in
locks/host-api-aars.lock. The minimum host remains 5285. This phase adds no public
JS API or host production change. The host repository adds opt-in instrumentation
and a test-only callback process in commit 8c8e89202e; it introduces no
authentication bypass.

## Integration findings

The released shared capability callback uses the KEY_BRIDGE_* response envelope;
it does not repeat contractVersion, which is negotiated through getBrokerInfo.
The adapter now accepts that envelope and rejects an explicitly incompatible
version. Descriptor replies contain only the result JSON, are size/MIME checked,
and are closed before publishing success. Both cases have Android regression
tests, in addition to the real host device_info and clipboard_set round trips.

Tests assert tool records have no errors as well as checking the final state.
A scripted model declaring completion alone cannot prove that a tool succeeded.
The Wi-Fi fixture also checks exact step/tool/model counts and independently reads
WifiManager, so decision repair cannot silently skip an intended tool.

## Final results

The final plugin is 1.0.0 / build 24. Host round trips on API 24/37 used build 23
with the same production code; their plugin suites were rerun with build 24.
The API 33 complete round trip used build 24. The final changes after build 23
only update test documentation, evidence and the commit-count version.

| Check | Result |
| --- | --- |
| JVM suite | 216/216, no failures/errors/skips; 7 added in P2.5 |
| Plugin instrumentation, API 24 / x86 | 18/18 |
| Plugin instrumentation, API 37 / x86_64 / 16 KiB | 18/18 |
| Host instrumentation, API 24 | 7 passed, 1 Wi-Fi hardware assumption skip |
| Host instrumentation, API 37 | 7 passed, 1 protected Wi-Fi switch assumption skip |
| Host instrumentation, API 33 / x86_64 / 4 KiB | 8/8, including real Wi-Fi |
| Debug, androidTest, release APK / R8 | Passed |
| Native alignment check | Passed; plugin contains no native libraries |
| lintDebug | 0 errors, 6 warnings, reviewed below |
| Markdown generation | 10 languages, 36 artifacts consistent |

The API 33 Wi-Fi run executed ui_dump -> ui_click -> ui_wait_for -> ui_dump ->
done, with exactly 5 journal steps, 4 tool calls and 5 model calls. All tool
records have no error. The final switch checked state and WifiManager agree
with the inverse of the initial state; finally restores that initial state.
Recorded duration: 5379 ms. Estimated usage: 57979 input + 182 output = 58161
tokens, estimated = true. These scripted remote-target figures are not Provider
billing measurements, real inference latency or local-model budget evidence.

The launcher 15-second timeout and host guidance were also checked on API 24.
Final startRun checks passed the 200 ms assertion. An earlier diagnostic run on
a freshly started API 33 AVD, while other AVD/build work was active, exceeded
that assertion. This is recorded rather than treated as a verified worst-case
latency guarantee; cold-start/load performance remains part of original P7.

Five lint warnings already existed (two dependency updates, one unused resource,
two icon duplicates). The additional StaticFieldLeak warning refers to the
process singleton AgentRuntime: its private constructor is called only with
context.applicationContext, so it retains no Activity/Service context.

## Validation setup

Validation date: 2026-09-23. JDK 21; plugin 1.0.0; host 6.8.0 / 5285.
All device operations use explicitly selected private, read-only, no-snapshot
AVDs. No connected physical device is used. No real Provider model, API key or
external inference request is involved.

The host instrumentation runs under the installed host UID and its real signing
identity. ScriptedModel supplies deterministic decisions through the public
IAiAgentModelBroker Binder. HostCapabilityBrokerCore/Stub supplies the real
capability broker, accessibility service, node snapshots and device operations.
The fake model advertises a configured remote target, UNKNOWN wire protocol,
plain decision JSON, output-token controls, 128 KiB input, 64 KiB output and a
1,000,000-token grant. Missing provider usage is explicitly estimated.

Host cases cover real attachment and private history, queue overflow and event
ordering, input and per-action confirmation, foreground service lifetime,
real Wi-Fi operations, real run observer process death, plugin force-stop and
history recovery, and real host link callback process death. The death helper
exists only in the test APK and checks the host UID before killing its own process.

Build commands: testDebugUnitTest, assembleDebug, assembleDebugAndroidTest,
lintDebug and assembleRelease in the plugin repository;
:app:assembleAppDebugAndroidTest in the host repository. Run the host fixture
only on a disposable AVD, with the matching host/plugin/test APKs installed:

```text
adb -s <private-avd> shell am instrument -w -r \
  -e autojs.agent.plugin true -e autojs.agent.accessibility true \
  -e class org.autojs.autojs.core.plugin.agent.AiAgentPluginRoundTripTest \
  org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
```

The accessibility option enables the real host service in that disposable AVD.
The Wi-Fi fixture restores the initial Wi-Fi state in finally. UiAutomation is
used for setup, service diagnostics and platform capability checks only; task
observations and actions go through the real host broker.

## Platform constraints and remaining roadmap work

The API 24 AVD has no FEATURE_WIFI and skips the Wi-Fi fixture. The API 37 AVD's
Settings Wi-Fi switch reports isAccessibilityDataSensitive = true and is absent
from the ordinary host accessibility tree, even with visibleOnly = false. The
fixture detects that platform condition and skips this particular operation;
it does not declare the host an accessibility tool or perform a privileged click.
This matches Android's documented restriction on sensitive views:
[Android Developers explanation](https://developer.android.com/blog/posts/enhancing-android-security-stop-malware-from-snooping-on-your-app-data).
An additional API 33 AVD supplies the accessible system Wi-Fi acceptance case.

This is P2.5 E1/E2 evidence with a scripted model and real host capabilities,
not real-model E4 success or the P7 compatibility/security release gate. P3 retains
script ranking, script_run and parameter completion. P4 retains trusted UI risk
inspection and richer recovery/OCR. Until then, all mutating tools require
per-action confirmation and ui_set_text values are always treated as secrets;
append text and script_run remain disabled. P5 retains the public ai.agent JS
API. P6 retains the full workbench, configurable presets, history UI and settings.

The shared capability contract has no per-call cancellation; cancellation fences
later operations and closes local reads but cannot roll back a host action already
admitted. Private journal observations are bounded previews, not lossless screen
captures. No goal, model input/output, node tree, argument or private filesystem
path is written to ordinary production logs. Physical OEM activation, real
Provider inference, full E4 scenarios and publication remain unexecuted here.

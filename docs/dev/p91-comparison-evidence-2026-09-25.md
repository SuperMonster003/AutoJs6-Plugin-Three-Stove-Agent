# P9.1 combined Provider and real-task comparison evidence

Dates: 2026-09-25 to 2026-09-26 (UTC+8). Scope: the original P9.1 test item.
The deterministic combined path is verified. Both real calculator paths completed
after the resource-ID fix; Wi-Fi comparisons and their limitations are recorded
separately below. No failed run is counted as an accepted task.

## Installed candidates and comparison method

- Host: AutoJs6 6.8.0 / 5297, initially `3e4e3a3cff`; later G8441 and XQ-DQ72
  received the resource-ID fix committed as `7ce99cc204`. Test infrastructure
  is committed separately as `eb86fda238`. These were development
  debug hosts with the official signer, not a newly published host release.
  The corrected arm64 APK SHA-256 is
  `0b99b725a540b0b05c7102d6fb21652a89c2fdd581211c161fa429a02544bfdb`;
  its runtime fix and test harness match those commits. Generated changelog
  text was updated after that test APK was built.
- Agent: official-signed R8 development candidate 1.1.0 / 82,
  `autojs6-plugin-ai-agent-v1.1.0-6ccf9b95.apk`, SHA-256
  `665238f325ce384333470b8e3ac0603c047063f8c4573e7e95294f6f9ddeb205`.
- Provider: official-signed R8 3-Stone AI development candidate 1.2.0 / 215,
  arm64 CRC32 `91B9A1BA`, SHA-256
  `db8755d6a1778fd8cbae87991989c884ca79fc300eecd79d1d7635fdb5e0db21`.
- The selected targets are the existing Model8 profiles, for the user's
  confirmed Fable 5.1 model. Credentials and profile settings were not changed.
- G8441 / Android 9 / API 28: Fossify Calculator, actual interface operation for
  `12*34` and reading the result. Budget: 25 steps, 35 model calls, 600 s,
  default 300000 total tokens.
- XQ-DQ72 / QV770340J7 / Android 15 / API 35: system Settings, Wi-Fi initially
  off, enable and read back the state. Budget: 30 steps, 40 model calls, 600 s,
  default 300000 total tokens. Validated cellular Internet was observed before
  the case; no Wi-Fi-off cloud run relied solely on Wi-Fi.

The existing host E4 harness now accepts `modelPath: "auto" | "json"` only in
opt-in instrumentation. Auto uses production negotiation. JSON hides the broker
native-extension version from that test link and otherwise delegates to the
same production brokers. Installed APKs, goal, target, tool grants and budgets
are identical between paths on each device. There is no production mode switch,
Provider direct binding, credential transfer or scripted replacement of model
decisions. Only normal confirmations within Calculator/Settings are automatically
accepted by the existing fixture; sensitive decisions are not auto-approved.

Two sequential trials per path were run. Initial calculator contents were not
randomized or reset by clearing application data; the task must inspect/clear
any previous expression. This small diagnostic sample does not establish a
success rate or a statistically meaningful latency advantage. All model calls,
including repair attempts, remain in the totals. The native initial output
ceiling, context ceiling and 16-round limit were not raised for the experiment.

## Initial real-model results, before the ID fix

Steps, tool calls, model calls, duration and tokens below are the terminal
`AgentResult` counters. A step counter can include the final failed admission;
it is not the count of successful UI actions. A successful instrumentation run
only means that evidence was collected.

| Device / task / path / trial | Steps | Tool calls | Model calls | Duration ms | Total tokens | Terminal outcome |
|---|---:|---:|---:|---:|---:|---|
| G8441 calculator native 01 | 15 | 11 | 16 | 80105 | 285481 | partial / BUDGET_EXCEEDED |
| G8441 calculator JSON 01 | 4 | 3 | 8 | 51420 | 102575 | failed / DECISION_UNPARSABLE |
| G8441 calculator native 02 | 16 | 12 | 16 | 87360 | 289939 | partial / BUDGET_EXCEEDED |
| G8441 calculator JSON 02 | 2 | 1 | 4 | 19206 | 46901 | failed / DECISION_UNPARSABLE |
| XQ-DQ72 Wi-Fi native 01 | 1 | 0 | 3 | 7476 | 41532 | failed / DECISION_UNPARSABLE |
| XQ-DQ72 Wi-Fi JSON 01 | 8 | 7 | 9 | 42873 | 137118* | failed / MODEL_FAILED |
| XQ-DQ72 Wi-Fi native 02 | 7 | 6 | 8 | 34575 | 109850 | failed / MODEL_FAILED |
| XQ-DQ72 Wi-Fi JSON 02 | 1 | 0 | 3 | 8048 | 34177 | failed / DECISION_UNPARSABLE |

`*` The JSON Wi-Fi 01 total includes a conservative estimate for missing usage
on the failed final call. Other listed totals are reported usage. Budget stops
below 300000 are expected when the remaining allowance cannot admit the next
bounded call; the quota was not silently exceeded.

There are **zero completed tasks in these eight initial trials**. Several
completed model generations contain empty text and zero output tokens, so the
bounded two repairs can still end in `DECISION_UNPARSABLE`. This observation does
not identify the upstream service's internal cause. Both paths retained these
failures rather than fabricating a decision.

Wi-Fi JSON 01 and native 02 did turn Wi-Fi on: an independent system read found
`wifi_on=1`. A subsequent model request failed with sanitized
`MODEL_FAILED / PROVIDER_FAILED`, before the Agent finished the required state
read-back. This is not a completed Wi-Fi task. Network switching and failure
occurred in that order; the available safe diagnostics do not establish the
precise transport/server cause. No automatic fallback replayed an executed
native action through JSON.

## Resource-ID mismatch found and corrected

The native calculator traces repeatedly used the observed `id=btn_2` or
`id=btn_multiply` in a selector and received `NODE_NOT_FOUND`. The host compact
formatter had removed the namespace from `org.fossify.math:id/btn_2`, while
`BridgeSelector.id` has intentionally literal, frozen matching semantics.
Thus the displayed value could not directly select its own node. This is a
host observation defect shared by both decision paths, not evidence that a
model invented an unobserved button ID.

The host now retains the complete resource ID in compact rows. Literal selector
matching, node identity, confirmation and grant checks stay intact. The fix also
preserves distinctions such as `android:id/button` versus
`example.app:id/button`; blindly prefixing the foreground package would be
incorrect for Android framework widgets. Existing row grammar and byte/node
ceilings remain in use. Longer IDs count toward the existing text/context caps.

Validation: 22 relevant JVM cases passed, including an observed-ID-to-selector
round trip and namespace non-aliasing. On API 24 and API 37.1, the real Agent
read a complete ID from the actual Settings node tree, sent it back through
`ui_find`, and verified matching node IDs. Both ten-call observation workflows
passed (2.579 s / 2.175 s). The shared observation/confirmation path was not
replaced by a mock for these device checks.

G8441 received the corrected host. Attempt `p91-g8441-native-calc-03` then stopped
in harness preflight because the enabled accessibility service did not rebind
after the upgrade. It never reached a model call and is not a ninth task result
in the table. Opening the host and toggling its existing secure-setting entry
did not recover the binding. The user manually re-enabled the service and its
actual binding was verified before the following runs. No device reboot,
lock-screen action or application-data deletion was performed.

## Real calculator comparison after the ID fix

| G8441 path / trial | Steps | Tool calls | Model calls | Duration ms | Total tokens | Terminal outcome |
|---|---:|---:|---:|---:|---:|---|
| Native 04 | 1 | 0 | 3 | 14252 | 41601 | failed / DECISION_UNPARSABLE |
| JSON 03 | 10 | 9 | 11 | 84224 | 160238 | completed / 408 |
| Native 05 | 12 | 11 | 13 | 77569 | 212690 | completed / 408 |

All three totals are reported usage, not estimates. Native 04 received three
empty generations and reached no tool. JSON 03 then entered `12*34` through the
calculator interface, pressed equals and read the complete result ID. Native 05
explicitly long-pressed Clear, read `0`, entered the expression, pressed equals
and read `408`; it did not just report the previous JSON run's result. Independent
UIAutomator reads after each successful run confirmed formula `12×34` and result
`408`. Private per-case XML and model/tool traces are retained.

The same goal, target, installed packages, grants and budgets were retained.
These are two observed task completions, not an estimate of reliability or a
demonstration that native is generally faster or more token-efficient. The empty
response failure remains part of the comparison. The calculator case's real
operation and read-back requirement is satisfied by each path.

## Real Wi-Fi comparison after the ID fix

XQ-DQ72 also received the corrected host. Its accessibility service rebound
without a manual intervention. Each of the following cases began with Wi-Fi off
and independently validated cellular Internet. Provider settings and the existing
VPN were retained. The same goal, target, grants and budgets were used.

| XQ-DQ72 path / trial | Steps | Tool calls | Model calls | Duration ms | Total tokens | Terminal outcome |
|---|---:|---:|---:|---:|---:|---|
| Native 03 | 1 | 0 | 3 | 7110 | 41532 | failed / DECISION_UNPARSABLE |
| JSON 03 | 2 | 1 | 4 | 10317 | 47981 | failed / DECISION_UNPARSABLE |
| Native 04 | 9 | 8 | 10 | 50132 | 154771 | failed / MODEL_FAILED |
| JSON 04 | 8 | 7 | 9 | 45490 | 127196* | failed / MODEL_FAILED |

`*` JSON 04 includes estimated usage for the failed final call. Native 03 and
JSON 03 exhausted bounded repairs on empty responses. Native 04 and JSON 04
enabled Wi-Fi, confirmed by independent system reads saved before any restoration;
both subsequently failed model continuation before completing the task's final
read-back. These are failed tasks despite the successful switch operation.

This follow-up reproduces the earlier network-transition limitation on both
decision paths. It does not identify a specific VPN, carrier, HTTP or upstream
server defect; validated system connectivity alone does not establish the model
endpoint's availability. No credentials, proxy, VPN or model configuration was
changed to force a pass, and no failed native operation was replayed through JSON.
Further blind repetitions were stopped after these two trials per path.

Across this session there are 15 real task outcomes: two completed calculator
tasks, two budget-limited partial calculator tasks, and eleven failed tasks.
The separate G8441 preflight failure is excluded from those counts. Original
P9.1's test item remains open because the Wi-Fi task has no completed comparison
pair. This is an explicit acceptance limitation, not missing or discarded data.

## Deterministic real host / R8 Agent / fake Provider

The production `AiAgentModelBroker` and `HostCapabilityBrokerStub` run under the
real host UID. The actual R8 Agent and a dedicated fake Provider run in separate
UIDs. All three use the standard Android test signer in an isolated checkout;
the fake Provider was never production-signed or installed on a physical device.

Four new integration cases pass: actual device observations in a two-call
native batch; rejection of an entire batch before its valid first call executes;
confirmation refusal without a clipboard write; cancellation while the Provider
is waiting for the confirmation. Tests check IDs/rounds, original-session
continuation, native journal metadata and model-call admission. The existing
14 broker cases also cover direct streaming/nonstreaming, quotas, deadline,
descriptors, cancellation, caller identity and sanitized errors.

API 24 x86 passed 18/18 in one invocation (15.912 s). API 37.1 x86_64 / 16 KiB
passed 18/18 with a fresh host process for each case. Its earlier combined runs
exposed deliberate deadline/hostile tests leaving the Provider fused for that
host process; subsequent healthy cases received `RATE_LIMITED / FUSED`.
`test-apps/ai-agent-conformance/run_native_provider_matrix.py` now isolates test
processes, preserves per-case logs and rejects non-disposable devices. It does
not reset or weaken production fuses. Final four-case reruns after fixture
selector scoping and the ID fix passed 4/4 on both devices (API 24: 0.923 s;
API 37: 1.820 s).

The fake Provider's 32 JVM cases, debug assembly and lint passed (0 errors,
2 existing warnings). Host debug and androidTest assemblies passed. Full host
lint and the complete unrelated host JVM suite were not newly rerun for this
bounded formatter/test change. Existing native-loop validation is documented
in `p91-agent-tools-evidence-2026-09-25.md`, not relabeled as a new full run.

Fixture preparation failures are retained: the original local fake target hit
the Agent's 7500-byte local input ceiling; a simulated remote target then exposed
the fixture's old four-round capability below the Agent's 16-round requirement.
The dedicated no-network remote target and descriptor now model the online
path's advertised limits. A test assertion initially used the wrong refusal
enum (`CONFIRMATION_DENIED` instead of `USER_DENIED`); the production behavior
was unchanged. Neither these failures nor the process-fuse failures were hidden
by relaxing runtime policy.

## Evidence and remaining resources

Private raw evidence lives under Agent `build/p91-comparison-private/` (per-device
catalog, configs, snapshots, terminal records, events, model events and continuation
timestamps) and host `build/p9-host-private/`. It can include UI/model data and is
not committed. The checked-in E4 collector includes continuation timestamps.

The temporary Wi-Fi and screen-timeout settings were restored after the initial
comparisons and again after the final corrected-host trials. Accessibility
component sets were also equivalent; Android rewrote some XQ-DQ72 component names
to their short form without changing the services. G8441's timeout is back to
120000 ms and the manually recovered service was used successfully. XQ-DQ72's
final read confirms validated Wi-Fi and cellular Internet. Provider configuration
was not edited. No shopping, pending order or payment was performed.

The Agent changes in this receipt are the collector, documentation and build
counter 83; the real task runs used the unchanged runtime in build 82. Ten-language
README and plugin instructions retain the observed limitations, 36 generated
artifacts pass the generator check, and the application text punctuation test
passes. No new Agent runtime/R8/full instrumentation claim is made for a
documentation-only receipt. Both private AVDs are stopped with data retained;
the host verification checkout is clean and fast-forwarded to `7ce99cc204`.

Original P9.1 remains open for the Wi-Fi comparison and the empty-response/network
failure limitations. P9.2's existing maintainer
decision is recorded in `p92-vision-protocol-proposal.md`: the maintainer accepted
a negotiated 2.1 image-input extension in the V2 family; it is not yet
implemented. No roadmap section was added, split or dropped. No push,
tag, npm/GitHub publication or replacement of published Agent v1.0.0 occurred.
Host Rhino work was not edited. QV710AF65F / XQ-AT72 / Android 12 remains absent,
with the user's expected availability before 2026-09-27 20:00 UTC+8 retained.

No manual action remains pending from this session. The test SIM is no longer
needed for the completed sampling; ordinary protocol/vision development needs
normal Internet only. A subsequent online Wi-Fi transition test will again need
an independent connection during that case, not a permanently assigned Redmi SIM.

# P8 final release gate evidence

Date: 2026-09-25. Last measured signed candidate: 1.0.0 / build 76.
Build 77 adds test-only failure diagnostics; no release is published yet.
Final device results and publication receipts are recorded below as they are
verified. A generated APK alone does not close the original P8 gate.

## Budget semantics corrected during the gate

The first real-model run on the release candidate stopped with `partial` after
4 decisions. Its decision described 37 remaining model calls and 26 remaining
steps as almost exhausted allowances, although the run had used only 4 model
calls and 40867 ms. The compiler had sent remaining values under the generic
`budget` section, without explaining their direction in the active context
template. This was a model interpretation failure enabled by ambiguous input;
the runtime's actual accounting and hard limits were correct.

The model-facing section is now `remaining_budget`. Both English/Chinese and
full/compact system prompts explicitly identify unused steps, model calls,
milliseconds and tokens. The older observation formatter uses the same label.
No JS, AIDL, default budgets, confirmation rules, model retry policy or host
API AAR changed. Ten-language release notes and reviewed prompt snapshots
were updated.

The new regression consumes actual `Budget` allowances, compiles a long
history under four language/size combinations, and verifies that packing and
compiler reuse preserve the current remaining values without changing an
earlier snapshot. This does not claim that every model will always interpret
instructions correctly.

## Build and artifact

- Temurin 21: debug, androidTest, JVM, lint, signed R8 release and
  `appendDigestToReleasedFiles` passed. Build 75 took 1m 35s; the final build 76
  took 1m 52s after the test-only Android 7 compatibility correction.
- JVM: 476 passed, 1 opt-in performance test skipped, 0 failures/errors.
- Lint: 0 errors, 6 existing warnings.
- All 10 languages / 36 generated Markdown artifacts match their sources.
- Single APK, no native payload, Android API 24 minimum / target API 37.
  Actual manifest checked for production components and permissions.
- Final APK: `autojs6-plugin-ai-agent-v1.0.0-6aa5a3d0.apk`, 642082 bytes.
- CRC32: `6aa5a3d0`.
- SHA-256: `2ab19c8e822e50427a3bddc6940aefbd57f7d6e705504f37630aa8b32617c257`.
- Signer SHA-256:
  `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.

Preflight build 73 and the initial build 75 candidate are preserved only in
ignored validation output. They are excluded from the release. The first
new unit-test compile used `copy` on the non-data `RunContext` class and failed;
it was corrected to construct a fresh context before the passing build.
The verified build 75 package (`34190ebf`, SHA-256
`fe5f43fa0090f4d6d17b01381db0d539e0b639faec4eead10305330dd822c0cb`) is also
archived privately. Build 76 changes only instrumentation, evidence and the
required commit-based version code; production task logic is identical.

## Device validation

The following initial device checks and real-model cases used build 75. Final
build 76 verification is recorded separately before closing the release gate.

Final API 24 contract: 4/4, 0.207 s. Final XQ-DQ72 / API 33 production entry:
5/5, 2.240 s, including the installed plugin's protected broadcast attaching
the real host controller in 257 ms. Tests run from the real host against the
signed R8 release. Full plugin instrumentation uses its matching debug APK,
then reinstalls the final signed release.

Before the budget correction, API 37.1 / x86_64 / 16 KB full instrumentation
reported 82 tests: 80 passed and 2 opt-in README capture cases skipped,
281.054 s. This is retained as an intermediate result, separate from the
final-source rerun.

The final-source API 37.1 / x86_64 / 16 KB rerun passed 80 tests with only the
2 opt-in README capture cases skipped (82 reported, 290.313 s). Its production
entry suite then passed 5/5 in 2.574 s against the signed release, including
protected broadcast attachment in 528 ms.

After power was restored, the unchanged signed candidate was installed on the
other three connected physical devices. Real-host production entry checks
passed 5/5 on each: Xiaomi Pad / API 35 (2.384 s, attachment 429 ms), G8441 /
API 28 (2.905 s, attachment 618 ms), and Redmi 12C / API 33 (4.887 s, attachment
1029 ms). These checks start no model task and do not switch network state;
their screen timeouts and notification permissions were restored.

## Real-model cases

All cases use the user's configured Model8 / Fable 5.1 target through the
production host broker and actual Agent release APK. The driver uses cautious
confirmation, confirms only ordinary actions in the system settings package,
and never replaces decisions, observations or completion results. Each case
has an independent task ID and starts with Wi-Fi disabled. Final success must
include the model's observed switch state and an independent `wifi_on` check.

| Case | Outcome | Decisions / model calls | Duration ms | Input / output tokens |
| --- | --- | --- | --- | --- |
| p8-xq-wifi-01 | Harness prerequisite failure, no model call | Not started | Not a task measurement | Not applicable |
| p8-xq-wifi-02 | Operator initially configured the base class rather than the actual AccessibilityServiceUsher component, no model call | Not started | Not a task measurement | Not applicable |
| p8-xq-wifi-03 | partial, premature budget interpretation on the first candidate | 4 / 4 | 40867 | 48551 / 522 |
| p8-xq-wifi-04 | MODEL_FAILED / PROVIDER_FAILED on the next request after enabling Wi-Fi | 5 / 6 | 30624 | ~79837 / 315 |
| p8-xq-wifi-05 | MODEL_FAILED before any action, first temporary-proxy attempt | 0 / 1 | 24136 | ~9661 / 0 |
| p8-xq-wifi-06 | completed, settings switch and readback verified | 11 / 11 | 68528 | 182743 / 956 |
| p8-avd37-wifi-01 | completed, quick-settings switch and readback verified | 6 / 6 | 258563 | 86537 / 716 |

`~` indicates a usage total containing estimates. Failures are kept separately;
the successful switch action in a failed run is not counted as a completed
Agent task. The first two harness failures were corrected before any model
call, with the original service configuration saved for restoration.

XQ case 06 used a loopback CONNECT proxy restricted to `model8.run:443` via
ADB reverse, after restarting the Provider to pick up that route. TLS stayed
end to end, and the proxy did not log request bodies or credentials. The model
observed the checked WLAN switch; an independent `wifi_on=1` read confirmed it.
This is a successful Agent task on the measured transport, not carrier-direct
network reliability certification. Case 05 remains a separate failed attempt.

API 37 used its existing emulated cellular connection directly. Its quick
settings path avoids the previously documented sensitive accessibility data
restriction on that image's Internet settings page. The two System UI actions
(open quick settings and click the observed Wi-Fi switch) were inspected and
approved once. Both the model's checked-node observation and `wifi_on=1`
confirmed completion. The duration includes waiting for manual confirmation;
it is not an inference-speed measurement. These two successful cases validate build 75. The final build 76 has its
own two-device smoke below, and remote CI still keeps publication open.

Before the power interruption, the temporary proxy, its ADB reverse mapping
and helper process were removed. XQ's Wi-Fi connection was independently
reported VALIDATED. Original screen timeouts and accessibility configurations
were restored on both devices. API 37's temporary metered-network permission
in 3-Stone AI and the host's external-storage app-op were restored to their
original values. No network passwords, account settings or SIM configuration
were changed.

## Final build 76 device and real-model results

The exact APK above was installed and verified through the real host's
signed-release entry suite on four devices. Each passed 5/5, including the
protected production broadcast and actual host-controller attachment:

| Device | API | Suite seconds | Attachment ms |
| --- | --- | --- | --- |
| Redmi 12C | 33 | 4.576 | 927 |
| XQ-DQ72 | 33 | 2.011 | 282 |
| AVD API 24 | 24 | 3.920 | 1312 |
| AVD API 37.1 / 16 KB | 37 | 4.075 | 1100 |

The final-package real-model tasks are independent attempts, without changing
production confirmations, budgets or completion decisions. Online cases use
Model8 / Fable 5.1; the Redmi case uses local Gemma 4 E2B IT. `Result steps`
includes a terminal error step where recorded; it is not an extra model
decision. An independent Wi-Fi read was taken before restoring device state.

| Case | Outcome | Result steps / model calls | Duration ms | Input / output tokens | Wi-Fi after task |
| --- | --- | --- | --- | --- | --- |
| p8-build76-redmi-local-wifi-01 | MODEL_FAILED / BINDER_DIED | 1 / 2 | 174178 | ~4045 / 74 | Off |
| p8-build76-avd37-wifi-01 | MODEL_FAILED / PROVIDER_FAILED | 3 / 3 | 52490 | ~35167 / 121 | Off |
| p8-build76-avd37-wifi-02 | completed | 6 / 6 | 143085 | 87275 / 800 | On, wifi_on=1 |
| p8-build76-xq-wifi-01 | MODEL_FAILED / PROVIDER_FAILED | 10 / 11 | 85456 | ~172656 / 713 | On, not task completion |
| p8-build76-xq-wifi-02 | completed | 10 / 11 | 62597 | 173938 / 1083 | On, wifi_on=1 |

Android's process exit record identifies LOW_MEMORY as the Redmi Provider's
termination reason (PSS about 2.3 GB / RSS about 1.8 GB), followed by the broker's
BINDER_DIED. No successful local-model result is claimed. The online Provider
failure callback does not establish a specific server or network root cause;
the failed attempts remain failures, including XQ case 01 after the switch
had already changed.

Both final-package successes used the temporary loopback CONNECT proxy over
ADB, restricted to model8.run:443 with end-to-end TLS. XQ case 02 ran alone
after restarting its Provider. API 37 case 02 used the observed quick-settings
Wi-Fi switch, with its two System UI actions separately inspected and approved.
Both the model's checked-switch observation and independent wifi_on=1 confirmed
completion. The AVD duration includes manual-confirmation wait time. These
results complete the original two-device release smoke on the measured route;
they are not certification of carrier-direct reliability or every model.

All temporary proxy settings, ADB reverse mappings and the helper process
were removed. Both network dumps report Wi-Fi VALIDATED after restoration.
Original Wi-Fi state, screen timeout, accessibility service list/enabled flag
and host storage app-op mode were compared with saved values on Redmi, XQ and
the AVD. The AVD Provider's metered-network option was restored to false through
its normal UI. No SIM, VPN, DNS, credentials, network passwords or account
configuration was changed. No shopping or payment task ran in this gate.

## Fresh CI environment

The first public build run at `6c28fd1` passed JVM/build/lint and Markdown, but
failed instrumentation on both fresh API 24/35 AVDs. The workbench correctly
refused task admission because those AVDs had no host package. The locally
tested AVDs already had compatible hosts, which had hidden this missing CI
prerequisite. API 35 also failed two visible injection-fixture cases and a
floating-window drag; their recovery must be verified, not assumed.

The CI AVD now has a dedicated `AI_Agent_Conformance_CI_*` name. The existing
fake-host runner's `--prepare-only` mode retains the physical-device, AVD-name
and real-host replacement guards, installs the test-only host plus Agent,
prepares notification permission, and keeps the disposable screen awake and
unlocked. Production package/version/signer checks and all test assertions
remain enabled. The original four independent broker tests are unchanged.
Final CI results are recorded after the new source commit has run remotely.

The build 75 CI rerun passed JVM/build/lint, Markdown and the complete API 35
suite (2 opt-in capture cases skipped). API 24 had one remaining failure:
the confirmation test searched only system-type accessibility windows, while
Android 7 reports its TYPE_PHONE overlay as an application window. It now
matches the actual card title, as the other floating-window test already did.
Local API 24 verification then reached the next assertion and exposed an
additional test assumption: older dumpsys prints hexadecimal window flags.
The test now checks the actual card's FLAG_SECURE bit in that format as well
as symbolic flags, rather than searching all windows for a string. No
production overlay or privacy setting changed.
Final focused regressions passed both floating-window cases on API 24
(2/2, 16.131 s) and API 37.1 (2/2, 19.626 s). The final test APK compiles,
lint still has zero errors, and punctuation/Markdown checks pass. Build 76's
signed entry check on Redmi also passed 5/5 (4.576 s, attachment 927 ms).

The resumed local isolated conformance AVD passed the complete Agent suite:
80 passed, 2 opt-in capture cases skipped, 318.756 s. The four independent
fake-host broker tests then passed through the unchanged default runner mode.
The disposable emulator was shut down without modifying any real host or
deleting its data directory. A first operator command used the wrong test APK
filename and stopped before instrumentation; it was corrected to the actual
`app-debug-androidTest.apk` output before the complete passing run.

## Publication scope

The public Agent source repository was created at companion-completion commit
`6c28fd1`. The four companion publications and exact validation boundaries are
recorded in [the companion evidence](p8-companions-evidence-2026-09-25.md).

The required host versions remain attach 5289 and full task API 5293. The
currently published stable host checked during this gate is AutoJs6 v6.7.0;
it does not provide the required Agent APIs. Agent 1.0.0 needs a compatible
6.8.0 development build until that host release is available. This work does
not publish the host's entire 6.8.0 release or alter its concurrent Rhino work.

Previously documented local-model failures, Ace's Redmi semantic timeout and
the absent XQ-AT72 / Android 12 remain recorded limitations. Native tools,
visual input and dynamic script generation remain the original P9 / 1.1.0
work. No shopping or payment task is performed in this gate.

## Build 76 remote failures and diagnostic follow-up

Build 76 passed its full API 24 CI suite, JVM/build/lint and Markdown. Its
[API 35 first attempt](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/actions/runs/36118973193/job/108020602509)
failed one floating-ball drag assertion. A complete rerun with unchanged
source and assertions also failed: the same drag check and both visible
injection-fixture tests. These are failed attempts, not a passing release
gate; the exact environment or implementation cause is not established.

The unchanged build 76 passed another full local isolated API 37.1 / 16 KB
suite with animations disabled as in CI: 80 passed, 2 opt-in captures skipped,
253.491 s. A focused left-edge-position hypothesis did not reproduce the
failure and is not claimed as its cause. The isolated AVD's animation settings
were restored and the emulator shut down without deleting its data.

Build 77 adds failure-time window/input/activity/power/accessibility dumps
and a screenshot, enabled only by an explicit CI instrumentation argument
on emulator hardware. Capture happens before fixture cleanup, stays in test
artifacts, and never enters release code or ordinary content logs. CI pulls
these artifacts before the emulator runner shuts down. Test selections,
assertions, budgets and production behavior are unchanged. A fresh local
API 35 / Google APIs / Pixel 7 AVD is being used to reproduce the CI condition.
GitHub Release and official index admission remain pending.

## Input and activity synchronization follow-up

The fresh local API 35 / Pixel 7 / Google APIs revision 9 emulator passed the
three originally failing cases (21.171 s), then the full suite after clearing
only its fixture applications (80 passed, 2 opt-in captures skipped, 278.032 s).
This does not explain away the remote failures.

Review of [Android 15's input command](https://github.com/aosp-mirror/platform_frameworks_base/blob/android15-release/services/core/java/com/android/server/input/InputShellCommand.java)
identified a timing hazard: shell swipe starts its duration budget before
synchronously dispatching DOWN. A slow dispatch can use that entire budget
and leave no MOVE events before UP. The floating test now injects all 20
pointer MOVE samples explicitly through public UiAutomation, checking each
dispatch result. The original window movement assertion is retained. This
removes the identified hazard without changing production drag handling;
failure artifacts remain necessary to establish the exact earlier CI cause.

The separate injection-fixture APK now launches through `am start -W` after
UiAutomation is connected, before the existing node-visibility deadline
starts. The actual canary, observed hostile text, denial and deletion
assertions remain unchanged. Local focused checks of these test changes
passed 4/4 on API 24 (16.969 s) and 3/3 on API 35 (6.889 s). An initial local
ADB invocation exited before instrumentation and has a separate harness log;
it is not counted as a test pass.

Build 78 contains only this test synchronization change, documentation and
the commit-based version counter. Its final APK and remote gate must still
be verified before release. No production API, budget, privacy setting or
model behavior changed in builds 76 through 78.

The build 77 CI run passed API 24 and both injection cases, but again failed
the API 35 drag check. Its recorded initial frame was Rect(912, 847, 1080,
1015), ruling out a left-edge starting position in that attempt. The first
diagnostic upload could not create its local destination because the root
build directory did not exist on the runner. Build 78 now creates that parent
before pulling; the missing screenshot is not claimed as observed evidence.

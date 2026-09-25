# P8 final release gate evidence

Date: 2026-09-25. Published version: 1.0.0 / build 78.
The original P8 release gate, GitHub Release and official index admission are
complete. Earlier build results and failed attempts remain labeled below.
The final build 78 section and publication receipts bind the released APK;
intermediate passes do not replace its validation.

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
  `appendDigestToReleasedFiles` passed. Build 75 took 1m 35s, build 76 took
  1m 52s, and build 78 took 1m 14s. After the workflow/evidence-only amendment,
  the exact published HEAD was rebuilt in 13s. Its embedded Git revision and
  signature changed; its executable code and resources remained identical.
  The rebuilt APK received its own final two-device validation below.
- JVM: 476 passed, 1 opt-in performance test skipped, 0 failures/errors.
- Lint: 0 errors, 6 existing warnings.
- All 10 languages / 36 generated Markdown artifacts match their sources.
- Single APK, no native payload, Android API 24 minimum / target API 37.
  Actual manifest checked for production components and permissions.
- Final APK: `autojs6-plugin-ai-agent-v1.0.0-185ddeb2.apk`, 642082 bytes.
- CRC32: `185ddeb2`.
- SHA-256: `c2bced292ff41d13dfbb370a58c56b9a9ce71cc687e8dba02631298c481ec6f9`.
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

This section records build 75 checks. Builds 76 and 78 are recorded
separately below; earlier checks are not reported as final-build measurements.

Build 75 API 24 contract: 4/4, 0.207 s. XQ-DQ72 / API 33 production entry:
5/5, 2.240 s, including the installed plugin's protected broadcast attaching
the real host controller in 257 ms. Tests run from the real host against the
signed R8 release. Full plugin instrumentation uses its matching debug APK,
then reinstalls the corresponding signed release.

Before the budget correction, API 37.1 / x86_64 / 16 KB full instrumentation
reported 82 tests: 80 passed and 2 opt-in README capture cases skipped,
281.054 s. This is retained as an intermediate result, separate from the
budget-corrected build 75 rerun.

The budget-corrected build 75 API 37.1 / x86_64 / 16 KB rerun passed 80 tests with only the
2 opt-in README capture cases skipped (82 reported, 290.313 s). Its production
entry suite then passed 5/5 in 2.574 s against the signed release, including
protected broadcast attachment in 528 ms.

After power was restored, the unchanged signed candidate was installed on the
other three connected physical devices. Real-host production entry checks
passed 5/5 on each: Xiaomi Pad / API 35 (2.384 s, attachment 429 ms), G8441 /
API 28 (2.905 s, attachment 618 ms), and Redmi 12C / API 33 (4.887 s, attachment
1029 ms). These checks start no model task and do not switch network state;
their screen timeouts and notification permissions were restored.

## Build 75 real-model cases

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
it is not an inference-speed measurement. These successes validate build
75. The separate build 76 smoke still left remote CI unresolved; the final
build 78 validation and publication are recorded at the end of this document.

Before the power interruption, the temporary proxy, its ADB reverse mapping
and helper process were removed. XQ's Wi-Fi connection was independently
reported VALIDATED. Original screen timeouts and accessibility configurations
were restored on both devices. API 37's temporary metered-network permission
in 3-Stone AI and the host's external-storage app-op were restored to their
original values. No network passwords, account settings or SIM configuration
were changed.

## Build 76 device and real-model results

The intermediate APK `autojs6-plugin-ai-agent-v1.0.0-6aa5a3d0.apk` (SHA-256
`2ab19c8e822e50427a3bddc6940aefbd57f7d6e705504f37630aa8b32617c257`) is archived
privately. It was installed and verified through the real host's
signed-release entry suite on four devices. Each passed 5/5, including the
protected production broadcast and actual host-controller attachment:

| Device | API | Suite seconds | Attachment ms |
| --- | --- | --- | --- |
| Redmi 12C | 33 | 4.576 | 927 |
| XQ-DQ72 | 33 | 2.011 | 282 |
| AVD API 24 | 24 | 3.920 | 1312 |
| AVD API 37.1 / 16 KB | 37 | 4.075 | 1100 |

The build 76 real-model tasks are independent attempts, without changing
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

Both build 76 successes used the temporary loopback CONNECT proxy over
ADB, restricted to model8.run:443 with end-to-end TLS. XQ case 02 ran alone
after restarting its Provider. API 37 case 02 used the observed quick-settings
Wi-Fi switch, with its two System UI actions separately inspected and approved.
Both the model's checked-switch observation and independent wifi_on=1 confirmed
completion. The AVD duration includes manual-confirmation wait time. These
results validate that candidate on the measured route. They do not replace
build 78's final smoke or certify carrier-direct reliability or every model.

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
These historical CI corrections are followed by the final build 78 results below.

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
At build 77, GitHub Release and official index admission remained pending.

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

Build 78 contains only these test/CI synchronization changes, documentation
and the commit-based version counter. Its final APK and remote gate are
verified below. No production API, budget, privacy setting or model behavior
changed in builds 76 through 78.

The build 77 CI run passed API 24 and both injection cases, but again failed
the API 35 drag check. Its recorded initial frame was Rect(912, 847, 1080,
1015), ruling out a left-edge starting position in that attempt. The first
diagnostic upload could not create its local destination because the root
build directory did not exist on the runner. Build 78 now creates that parent
before pulling; the missing screenshot is not claimed as observed evidence.

## Final build 78 verification

The first build 78 package (`a149479f`, SHA-256
`f6d2082d01996d22a16846638c61cbf68d4644257bc5d3b6c42cb9c2bdd2afd9`) was built
before the CI artifact-directory correction amended the commit. It embeds
the pre-amendment revision a8ce653. Both case 01 tasks completed on that file:
XQ 10 steps / 10 calls / 56202 ms, AVD 9 steps / 10 calls / 317748 ms. The AVD
included a confirmation timeout and a subsequent individually approved retry.
Those measurements are retained as intermediate results, not final receipts.

The publication guard stopped before creating a Release when it found both
build 78 APKs after the final rebuild. Comparison of every ZIP entry found
only META-INF/version-control-info.textproto changed, from a8ce653 to 20a2ecc;
DEX, resources, manifest and ZIP timestamps are identical. The earlier hash
check had read the old named file and did not establish rebuild identity.
The old APK was archived privately. The final package below was reinstalled,
signature-checked and retested on both devices. Publication now additionally
checks its embedded revision against the exact release source.

The released source is `20a2ecc25a5e3932f3996ff206d834daed8ba2d0`. Its local Temurin
build passed 476 JVM tests with one opt-in performance case skipped, lint with
0 errors / 6 existing warnings, debug/androidTest, signed R8 release and native
payload guards. Ten languages / 36 generated Markdown files match. The final
fresh isolated API 35 full run passed 80 tests with only the 2 opt-in README
captures skipped, 286.630 s. The disposable AVD was stopped; its data remains.

The exact signed release passed the real-host production entry suite 5/5 on
XQ-DQ72 (2.030 s, attachment 369 ms) and API 37.1 / 16 KB (3.517 s, attachment
1082 ms), including installation, activation and protected host attachment.

| Final build 78 case | Outcome | Steps / model calls | Duration ms | Input / output tokens | Independent Wi-Fi read |
| --- | --- | --- | --- | --- | --- |
| p8-build78-xq-wifi-02 | completed | 12 / 12 | 64287 | 197635 / 974 | wifi_on=1 |
| p8-build78-avd37-wifi-02 | DECISION_UNPARSABLE | 5 / 7 | 95829 | 98591 / 629 | wifi_on=1, not task completion |
| p8-build78-avd37-wifi-03 | completed | 6 / 6 | 87349 | 86871 / 631 | wifi_on=1 |

These cases use Model8 / Fable 5.1 through the production host, with exact
reported token usage. AVD case 02 emitted an unsupported checkable selector
argument three times. The original two-repair limit correctly stopped it with
TOOL_ARGUMENTS_INVALID records and terminal DECISION_UNPARSABLE. Its changed
Wi-Fi state does not count as completion. Case 03 is a separate run with the
same configuration and unchanged schema, validation and repair limits.

In each successful case, the model observed the checked switch. Both used
the restricted temporary CONNECT route described above, not a demonstrated
carrier-direct success. Opening quick settings and clicking its newly observed
Wi-Fi toggle were each reviewed and approved once. The AVD duration includes
operator waiting and is not a model-speed measurement. No production timeout,
budget, confirmation policy or completion result was overridden.

Afterward, both devices' original Wi-Fi state, screen timeout, accessibility
service list/enabled flag and storage app-op mode were compared with their
saved values. All six proxy keys, including the actual PAC URL key, were
restored; ADB reverse and the PC helper were removed. Both Wi-Fi networks
returned VALIDATED. The AVD Provider's metered-network setting was restored
to false through its UI and verified. No credentials, VPN, DNS, SIM, saved
network or account configuration was changed; no order or payment was made.

The exact source's [Build integrity run](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/actions/runs/36123770984)
passed all three jobs: JVM/build/lint and the full API 24 / API 35 suites.
Its [Markdown run](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/actions/runs/36123770974)
also passed. Both emulator suites retain only their two opt-in capture skips.
The earlier builds 76/77 failed remotely as recorded above; their results were
not replaced by local passes or by an unchanged successful rerun. The final
source includes the explicit input delivery and Activity launch synchronization
changes, with the original drag, canary, denial and deletion assertions intact.

## Official index metadata correction

The first complete index generation exposed two metadata problems before
commit. AI Agent's application name is in strings_donottranslate.xml, while
the generator read only strings.xml and fell back to the repository name.
Separately, a TLS handshake timeout fetching MLKit Barcode's declared
version.properties produced versionCode 0 and dropped its existing artifact
admission fields. The local diff check caught this; that output was not pushed.

The index generator now requires a complete published source tree and fails
if a file declared in that tree cannot be read. Genuinely absent optional
legacy files remain optional. It merges strings.xml and strings_*.xml within
each values directory and rejects duplicate names. Four added regression tests
cover failed version retrieval, unavailable/truncated trees, split app names
with localized descriptions, and ambiguous/unreadable string files. All 46
index tests pass. Regeneration adds AI Agent and corrects 27 existing display
titles from their published resources; all other existing item data, including
MLKit Barcode's version and admission receipts, matches the previous index.

## Publication receipts

- [GitHub Release v1.0.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases/tag/v1.0.0)
  is published, non-draft and non-prerelease, with exactly the one APK above.
- The release tag resolves to `20a2ecc25a5e3932f3996ff206d834daed8ba2d0`. That source's
  VERSION_BUILD equals its 78 reachable commits, and its worktree was clean
  before publication. GitHub's asset size and SHA-256 match the tested local
  APK; apksigner verified its certificate and APK v2 signature.
- Official index commit `8aaca1c4766c6f4d87a7e77326c1b865ff9df1f4` admits the actual
  release via `release-manifests/io.github.supermonster003.autojs6.plugin.ai.agent/78.json`.
  The required inventory has 45 projects and the generated index 61 entries.
  The entry retains requiresHostVersion 5289 (attachment), nativePageAlignment 0,
  exact source/tag, version 1.0.0 / 78, signer and final artifact size/hash.
  Full task usage requires host build 5293, as documented in the release.
  All 46 index unit tests passed, followed by full generation and remote
  readback of the committed entry and admission receipt.
  The index's [publication CI](https://github.com/SuperMonster003/AutoJs6-Official-Plugins-Index/actions/runs/36127609425)
  also passed its tests and full regeneration against the pushed source.
- This evidence/roadmap-only completion commit advances the branch counter to
  79. It does not replace or retag the tested build 78 APK. Historical failed
  CI/model attempts and absent-device limitations remain visible.

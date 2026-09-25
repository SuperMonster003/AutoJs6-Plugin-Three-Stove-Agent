# P8 final release gate evidence

Date: 2026-09-25. Candidate version: 1.0.0 / build 75.
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
  `appendDigestToReleasedFiles` passed. The final combined build took 1m 35s.
- JVM: 476 passed, 1 opt-in performance test skipped, 0 failures/errors.
- Lint: 0 errors, 6 existing warnings.
- All 10 languages / 36 generated Markdown artifacts match their sources.
- Single APK, no native payload, Android API 24 minimum / target API 37.
  Actual manifest checked for production components and permissions.
- Final APK: `autojs6-plugin-ai-agent-v1.0.0-34190ebf.apk`, 642082 bytes.
- CRC32: `34190ebf`.
- SHA-256: `fe5f43fa0090f4d6d17b01381db0d539e0b639faec4eead10305330dd822c0cb`.
- Signer SHA-256:
  `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.

Preflight build 73 and the initial build 75 candidate are preserved only in
ignored validation output. They are excluded from the release. The first
new unit-test compile used `copy` on the non-data `RunContext` class and failed;
it was corrected to construct a fresh context before the passing build.

## Device validation

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
it is not an inference-speed measurement. These two successful cases complete
the final package's required two-device case (1) smoke.

Before the power interruption, the temporary proxy, its ADB reverse mapping
and helper process were removed. XQ's Wi-Fi connection was independently
reported VALIDATED. Original screen timeouts and accessibility configurations
were restored on both devices. API 37's temporary metered-network permission
in 3-Stone AI and the host's external-storage app-op were restored to their
original values. No network passwords, account settings or SIM configuration
were changed.

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

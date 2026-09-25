# P9.1 host native tool continuation evidence

This change implements the original P9.1 host item. It does not complete the
3-Stone AI mappings, Agent native decision loop, or the Wi-Fi/calculator comparison
item. No roadmap item is added, split or discarded. The implementation is in
AutoJs6 6.8.0 / build 5297, commit
`3e4e3a3cffe8b8ebdcb1805e6895a72794828d4e`; Agent 1.0.0 retains its structured
JSON path. Agent build 80 records this host item and its compatibility note.

## Host behavior and compatibility

- Internal `AiPluginAskRequest` accepts declared tools and 1-16 tool rounds. The
  tool runner supports both streaming and non-streaming generation. Public script
  `ai.ask` and `ai.stream` options are unchanged.
- The existing Provider V2 tool messages now reach the model broker as
  `tool_calls`. The host validates declared names, unique call IDs, descriptor
  bounds, outstanding batches and round limits before materializing arguments.
- `IAiAgentModelBroker.submitToolResults` is appended after the original five
  transactions. Base contract V1 and their transaction codes remain unchanged.
  Consumers must negotiate `modelBrokerInfoJson.toolCallingVersion == 1` and the
  selected target's `tools` capability before using this optional extension.
- Results continue the original request/callback/sequence. Exactly one terminal
  event is allowed. Results must match the entire pending batch and its round;
  replay, premature completion and chunks while awaiting results are rejected.
- Each result is at most 64 KiB of UTF-8 text; the complete result JSON is at most
  128 KiB. Correlated file-descriptor transport remains available. Cancellation,
  original deadlines and callback/link death remain effective while awaiting
  tool results, including a sender that leaves a pipe incomplete.
- Every continuation rechecks the growing input context, reserves tokens and
  consumes a model-call rate entry. Cumulative usage replaces the earlier charge,
  including byte estimates and late usage reports; it is not charged again in
  full each round. Diagnostic strings contain sizes/names, not arguments/results.
- The host transports calls only. Tool execution and its validation, confirmation
  and journaling remain the Agent's responsibility in the next original item.

The host protocol document and ten language changelog sources are updated.
Twenty-two README/changelog artifacts were rendered with the existing renderer,
without refreshing unrelated online metadata. No JS signature or public type
declaration change requires another companion publication.
The Agent changelog's ten languages also add a compatibility hint, as required by
the roadmap for public host contracts. Existing 1.0.0 capability entries, release
date, published tag and APK are preserved. Its generator verifies all 36 artifacts;
this documentation-only Agent update does not rebuild or republish the release.

## JVM and build checks

The host test filters cover `core.plugin.ai`, `core.plugin.agent` and
`runtime.api.ai`; the two protocol modules are also checked:

| Suite | Passed | Failed / error / skipped |
|---|---:|---:|
| Host, 25 suites | 244 | 0 / 0 / 0 |
| ai-agent-api, 2 suites | 7 | 0 / 0 / 0 |
| ai-provider-api, 10 suites | 77 | 0 / 0 / 0 |

Coverage includes tool descriptor materialization/closure, matching result
continuation, wrong IDs, replayed calls, excess rounds, premature completion,
queued-result cancellation, schema/UTF-8 boundaries and cumulative quota
reconciliation. The AIDL-order test preserves the old transaction sequence and
checks the appended method.

Host debug, instrumentation, and the two conformance APKs assemble successfully.
The isolated test-signed checkout's first complete build took 8m 5s; the added
positive foreign-UID test was then rebuilt successfully in 1m 16s. Native page
alignment checks pass. No runtime dependency was added.

Strict whole-host lint (`--offline`, one worker,
`-Pautojs.sdk.lint.verify=true`, command-local 12 GiB heap/G1) and final test APK
assembly passed in 9m 49s. The report has 0 Error/Fatal, 2403 Warning and 3 Hint;
diagnostics match the previously verified lint snapshot when ignoring line-number
movement. No rule was disabled or baseline broadened. Final lint XML SHA-256:
`f61c497789bef78da1b39c4047b79eadef19e1dfcc43f1622225682891d9bd5e`.

## Real Binder tests with deterministic model fixtures

Only dedicated test-signed artifacts are installed into the private
`AI_Agent_Conformance_P9_*` AVDs. Host, instrumentation and both fake plugins use
the Android debug certificate, SHA-256
`2e64822e13a6c80c12e1c4b47e8fb32d1e9334526289da75777b7a79145de4b8`.
Production signing files are absent from the isolated checkout.

The tested APK SHA-256 values are:

| Artifact | SHA-256 |
|---|---|
| Host x86 | `d3a436cd6965ce735c3105a22d194c4e5ebfdb6431fa086ce7a7a45bb27b041d` |
| Host x86_64 | `5df01fef750d14480b74abf322ab696e34e17169cddeca0b1910ed8663160cc3` |
| Host instrumentation | `609185a1b9a892f0448737de0455b472429200bd9b37fe9d48a5097a30994ca4` |
| Fake Provider | `aa6c63fcb041abd665375eee37dfd36b8a953b49973a25d69df9a2718182b192` |
| Fake Agent | `e11dc5726934b6b7d18bdc747cd71e1eb8dcddb17e1aed8929e45434fe097954` |

| Android environment | Model broker | Foreign-UID grants | Agent peer | Provider session | Result |
|---|---:|---:|---:|---:|---|
| API 24, x86 | 14 | 10 | 6 | 11 | 41/41, 29.679 s |
| API 37.1, x86_64, 16 KiB pages | 14 | 10 | 6 | 11 | 41/41, 35.179 s |

The native model tests use a separate fake Provider APK through the production
host discovery/handshake/session path. They cover both stream modes, same-request
completion, invalid result IDs/rounds, cancellation, the original deadline,
continuation rate limits, descriptor correlation, and prompt cancellation of a
stalled result pipe. Several tests cover multiple scenarios.

The grant driver runs under another UID/process. Its new positive test calls
`generate` and the appended `submitToolResults` transaction, receives callbacks
from that same foreign UID and completes the original sequence. Its nine-entry
negative probe also verifies that an unauthorized UID cannot submit results.
Injected deterministic model runners in that grant test are distinct from the
real fake-Provider IPC used by the model/session tests.

These are protocol and lifecycle checks, not Model8 Fable 5.1 or LiteRT Gemma 4
E2B IT acceptance, and not a native-vs-JSON task comparison.

## Failed attempts retained

- The first JVM build exposed the old AIDL-order golden expectation. The test was
  updated to retain the original five methods and include the appended method;
  all protocol/JVM tests above then passed.
- An initial run on `AVD_API_24` had 18 passes and 11 failures in 3.922 s. The
  eleven failures were the test fixture's same-signer precondition: the locally
  signed host and default-debug Provider fixture did not match. Those cases had
  not reached Provider invocation. The fixtures were uninstalled and the AVD
  stopped; no signing check was bypassed and no production key was used for the
  Provider matrix. The successful run above uses an isolated test-signed checkout
  and private AVD data. The initial AVD's host was upgraded to build 5297, with
  its data preserved.
- The first whole-host lint run used a 4 GiB Gradle heap. It remained in analysis;
  diagnostics showed the old generation full and 512 full GCs consuming 613 s.
  Only this run's daemon was stopped, and the retry uses a command-local 12 GiB
  heap/G1 with one worker and the existing strict lint flag. No repository heap
  setting, rule or baseline was changed. A retry's PowerShell argument quoting
  error failed task selection in 16 s; the corrected command preserves all checks.

Raw build/device logs remain under the host's ignored `build/p9-host-private/`.
Test data is retained locally for review and is not committed.
Both private AVDs were stopped after their successful runs. The four real devices
were not operated on or changed; there were no model-network, order or payment
actions. Main and verification checkouts have identical contents for all 55
changed/new source candidates, including the final foreign-UID test.

## Remaining original work and device needs

Next is P9.1's 3-Stone AI item: three online protocol mappings and truthful
target-level LiteRT capability declarations. Then the Agent can adopt native
tools using the existing validation/confirmation/journal, followed by the original
Wi-Fi and calculator comparison. None of those items is marked complete here.

This host implementation does not require a SIM card, model credentials, shopping
actions or manual device configuration. Later online Wi-Fi comparisons need
independent connectivity only during those tests. QV710AF65F / XQ-AT72 Android 12
remains offline and untested, with the user's expected availability before
2026-09-27 20:00 UTC+8 retained.

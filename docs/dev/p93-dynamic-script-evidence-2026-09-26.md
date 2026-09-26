# P9.3 dynamic JavaScript execution

Date: 2026-09-26 (UTC+8). Development line: AI Agent 1.1.0. This document
records the original P9.3 implementation, final deterministic verification and
separately attributed physical-device evidence. No roadmap stage is added,
split or discarded. Local development candidates are not published releases.

## Original scope

| Original P9.3 item | Implementation |
| --- | --- |
| Plugin: default-off, SENSITIVE `script_dynamic`; `script_run_source(source, timeoutMs)`; source confirmation and full private step history; optional generated `@agent` file | Catalog/policy, source validator, execution adapter, per-call confirmation, source view, private history retention and user-initiated SAF save |
| Host: `engines.execScript` grant and optional write-path restrictions | The existing C.4 grant already included `engines.execScript` and `files.write`; this change adds owned inline execution/cancellation and explicit capability advertisement. Existing broker workspace/symlink restrictions remain in force. |

There is no new public `ai.agent.script_run_source()` JavaScript method.
`script_run_source` is a model tool, while `script_dynamic` is a selectable tool
group in existing Agent task options, settings and presets. It remains off by
default, including when the public host options merge an empty `tools` object.

## Negotiation and execution boundary

Agent requires `engines.execScript` in `availableOptionalMethods`, both
`engines.execScript` and `engines.stop` in the effective method grant, and the
effective `engines`, `engines.exec`, `agent` and `agent.exec` permissions. A
compatible host advertises this optional method only for a real Android Agent
broker with the required execution/stop support. The older grant's method name
alone is insufficient. Without the advertisement or required grant, the model
schema hides this tool and execution is unavailable, even if its group was
selected. Compatibility is not inferred from a newly invented minimum build
number; the development host still identifies itself as 6.8.0 / 5297.

The adapter calls `engines.execScript` through the existing host capability
broker with a fresh `agentInvocationId`, run attribution, bounded timeout and
console capture. The host's owned invocation registry rejects repeated tokens
and handles stop-before-exec, cancellation before engine start, explicit stop,
deadline and provider destruction. It stops the engine belonging to that
invocation; unrelated engines are not stopped. Registered file/project scripts
reuse the same execution lifetime while retaining their existing entry and
scalar-parameter checks.

Source must be nonblank valid Unicode, contain no NUL and fit within 8192 UTF-8
bytes. Agent additionally bounds the JSON-encoded source string to 8192 bytes,
including escaping, before confirmation and journaling. The host independently
validates its UTF-8 bound and restricts generated execution to the normal
background JavaScript engine. Engine-switch and UI/UI-thread directives are
rejected before evaluation. Caller-supplied working-directory, loop, delay,
argument and wait options cannot widen the owned invocation.

The tool's timeout defaults to 60000 ms and is bounded by the declared
1..300000 ms range, the effective host grant and the remaining task budget.
Cancellation and timeout use the owned token rather than a guessed global
execution ID. The existing script result slot, console fallback, error and
completion reporting are retained. A timeout does not undo side effects or
stop separately launched child engines.

**Generated JavaScript runs with the host script runtime's permissions. This is
not a JavaScript sandbox.** Other model tool-group restrictions and broker
file-path checks do not confine JavaScript's internal API calls. In particular,
turning off the model's file or shell tool group does not remove those abilities
from host JavaScript. The confirmation warning states this boundary. No AIDL
transaction, SDK AAR, permission declaration or Rhino upstream source was changed
for this feature.

## Confirmation, history and saving

Every generated-source invocation requires its own confirmation. Run-scoped
approval and a risk override cannot lower this SENSITIVE action or authorize
later source. No execution occurs while approval is pending or after its denial
or cancellation. The confirmation includes the exact validated source. The UI
shows its byte/line counts, timeout and a short preview; expanding reveals the
complete selectable plain text, in a monospace left-to-right view. Source is
never interpreted as markup.

Private step `arguments.source` retains the complete validated source instead
of the usual 2048-byte argument preview. A dynamic step has a bounded 24 KiB
record allowance; large ancillary decision/observation text is reduced first.
Existing total journal retention remains bounded to 1 MiB and the step limit,
so old steps can still be evicted with the truncation flag. Complete source in
a retained eligible record is not a claim of indefinite retention. Confirmation
and step-event tests also check the existing 32 KiB event envelope.

Sensitive-field redaction takes priority over exact-source retention. If known
protected text would change the source shown for approval, the runner rejects
the invocation with `TOOL_ARGUMENTS_INVALID`; it does not execute different
bytes behind a redacted preview. Protection learned later retroactively redacts
earlier history, marks the record `sourceRedacted`, and disables its exact-source
save action. Redaction expansion remains bounded while keeping runtime
attribution such as parse mode and repair count. Normal diagnostic history
export continues to redact arguments and arbitrary content; it does not export
the private source. Source-containing private records must not be printed to
ordinary logs.

The separate save action is initiated by the user from a retained, non-redacted
dynamic step. It uses `ACTION_CREATE_DOCUMENT` / `CATEGORY_OPENABLE`, MIME
`text/javascript`, and the URI chosen in Android's document picker. The model
does not supply an arbitrary output path. Cancellation creates no script through
the plugin, and saving does not execute it or grant access to a directory.

The generated file prepends a leading `@agent` header with a digest-derived
description/name, `@risk sensitive`, `@confirm before-run` and the bounded
timeout, followed by the exact source. A later source comment claiming
`@risk readonly` or `@confirm never` cannot override that first registration
header. To discover the file as a registered script, the user saves it into an
already approved host script directory, or adds the chosen directory through
the normal approval flow and rescans. Successful file creation alone is not
automatic host registration or execution.

## Committed host and companion work

| Repository | Commit | Development version / result |
| --- | --- | --- |
| AutoJs6 | `cdf1b6a564` | Owned execution, optional-method advertisement, public group defaults and generated-header parsing; host 6.8.0 / 5297 |
| AutoJs6-TypeScript-Declarations | `3404bdc` | 4.22.0; group declaration and positive/negative API smoke checks |
| AutoJs6-Documentation | `f9ed7afe41f2cdf2603dc281323be33869ced6d7` | Content 6.8.0 / versionCode 80; group, confirmation, host-permission and save guidance |
| AutoJs6-Plugin-Ace-Editor | `dd44432` | 1.14.0 / 114; declarations and generated completion/runtime metadata synchronized |
| AutoJs6-Plugin-Offline-Docs | `24b17b4` | 6.8.4 / 61; exact documentation provenance and offline assets synchronized |
| AutoJs6-Plugin-AI-Agent | This feature commit, parent `663febe` | 1.1.0 / 87; implementation, tests, ten-language guidance and exact local candidate |

The canonical declaration generator's `-Publish -SkipBuild` step was a local
synchronization operation, not an npm publication. Generated Java/resource/
dependency declarations were unchanged. All 69 Ace manual declarations match
the declaration repository; all five generated groups identify package 4.22.0.
Ace's 171 JVM tests and declaration/runtime checks passed. Documentation
generation/freshness checks cover 143 modules and 6225 search entries.

Offline Docs' two JVM and four Python tests, normalization/generation checks,
debug and signed R8 builds, packaged-content and alignment guards passed. Its
199-file document tree has canonical content SHA-256
`e2f0ae9fa59b7e569d8e802ee7af40c107b691e30763eb2892866168f2f0308a`.
These companion changes were not published to npm or as APK releases, and no
new companion device-runtime result is claimed. The pre-existing declaration
package publishing-configuration edit and Ace's untracked local releases were
preserved outside these commits.

## Verification

The host's committed receipt is
`AutoJs6/docs/dev/evidence/ai-agent-p93-host-20260926.md`. Its isolated verification
checkout left concurrent Rhino synchronization and the main signing setup intact.
Seven focused host JVM suites passed 41 tests. Private API 24 and API 37.1 / 16 KiB
AVDs each passed 26 Android tests: eight dynamic lifecycle cases, twelve
registered-script regressions and six broker cases. Assembly and 16 KiB native
alignment verification passed. Production-signed ARM64/test copies were compared
against those verified outputs, including non-signature ZIP entries.

Both final Agent emulator suites ran after the last source correction. The
receipts verify each test's unique terminal status, reported `numtests`, JUnit
terminal count, expected skips and absence of failures. Extra UI diagnostic
status messages are excluded from test counts. The two private Agent AVDs
were then stopped after their exact names were checked; logs and userdata
were retained, and other connected devices were not stopped.

| Agent check | Evidence observed | Final status |
| --- | --- | --- |
| Complete JVM suite | 73 suites, 537 tests, 0 failures, 0 errors, 1 opt-in performance skip: **536 passed + 1 skipped** | Passed; final build and XML reconciled |
| Final source UI / HostLink / Binder timeout tests, API 24 | 10/10, 2.520 s | Passed |
| Same targeted tests, API 37.1 / 16 KiB | 10/10, 6.853 s | Passed |
| Final full Android suite, API 24 / x86 | 90 passed + 2 opt-in README screenshot skips, 92 total; 246.939 s | Passed; 6 API-30 capture cases SDK-filtered |
| Final full Android suite, API 37.1 / x86_64 / 16 KiB | 96 passed + 2 opt-in README screenshot skips, 98 total; 286.569 s | Passed |
| External Binder conformance, API 24 Debug / R8 | Each 9 passed + 2 API-30 visual skips, 11 total; 1.115 s / 1.066 s | Passed |
| External Binder conformance, API 37.1 Debug / R8 | Each 10 passed + 1 old-Android-only skip, 11 total; 2.792 s / 1.621 s | Passed |
| Final Agent debug/androidTest/fake-host assembly | `build/p93-final-candidate-build.log`, successful 1 min 49 s build | Passed |
| Final Agent R8/release, signature archive and 16 KiB static alignment | Same final build | Passed |
| Agent lint | 0 errors, 6 existing warnings | Passed |
| Ten-language generated resources | 10 languages, 36 artifacts | Passed |
| Final Agent APK signature, version, SHA-256 / CRC32 | See exact artifact receipt below | Passed |

The focused dynamic integration uses the real plugin process and HostLink with
controlled same-UID debug fixtures for model and capabilities. It verifies
absence of the optional advertisement, schema availability, per-call approval,
source dispatch and complete history. It does not substitute for a model's
generation quality or the host's separate real-engine tests.

The first JVM/build attempt failed while concurrent UI source/resource work was
still being completed: `RunDetailActivity.kt` referenced the not-yet-available
`R.string.script_dynamic_redacted`. The compiler failure is retained in
`build/p93-unit.log`; it was not a passed JVM run. Later synchronized assemblies
completed, with their own logs rather than overwriting the failed attempt.

Both first full Android batches failed the same
`DynamicScriptConfirmationViewTest.fullSourceIsExactSelectableAndUsableInNarrowLargeFontRtl`
assertion, at test line 44: API 24 had 90 tests / 1 failure / 325.317 s, and
API 37.1 had 96 tests / 1 failure / 332.964 s. The logs are retained as
`build/p93-agent-private/api-5584-full.txt` and `api-5586-full.txt`. The successful
focused integration cases do not erase these full-suite failures. The failed
assertion compared a selectable TextView's Spannable buffer directly with a
String. Button height and source width assertions had already passed. The
test now compares the exact displayed string and separately verifies that the
warning remains selectable; all size, RTL, full-source and expansion assertions
remain intact. The explicit performance skip is
`StepPerformanceTest.compileParseAndValidateRepresentativeSteps`.

Review additionally found two private-journal edge cases: clipping a long
decision lost its parse attribution, and short protected values could expand
source text during redaction beyond the step bound. The final code preserves
bounded attribution, marks changed source ineligible for exact export, and
allows that redacted text to shrink. Context compilation treats a clipped
decision as a summary rather than replaying it as a complete assistant action.
The unredacted source in retained eligible steps remains exact.

The final broker correction recognizes owned dynamic execution when mapping a
generic host TIMEOUT to SCRIPT_TIMEOUT. It retains stable explicit error codes
before category mapping, including an explicit NODE_NOT_FOUND. Two Android
regressions exercise actual broker callbacks for dynamic/registered execution,
ordinary observation/launch, and explicit error-code precedence.

## Exact local Agent candidate

- Version 1.1.0 / build 87, R8 release, not debuggable.
- `releases/autojs6-plugin-ai-agent-v1.1.0-ee852c93.apk`, 702520 bytes.
- SHA-256 `835227c3743400fe881cba2451d51e98d88d6d69481f68adac80fefd6ecea2b8`.
- CRC32 `ee852c93`; classes.dex SHA-256 `8f266614a33cfd7ab56565faf4bf200b2992d13201f1a4ccb188e7c62a40d4a9`.
- APK v2 signing certificate SHA-256 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
- Built from parent `663febe` plus this feature change. Intermediate archives
  `15ab24d6`, `9ac07176` and `b208ee50` are superseded. No release/tag/push occurred.

For private emulator tests, five APK copies were re-signed using the standard
Android test certificate. Every non-signature ZIP entry matches its original
production-signed output. The guarded fake-host helper rejects physical
devices and installed real AutoJs6 hosts. Test copies are not presented as
production-signed installation evidence. The exact production-signed R8 Agent
was separately installed on QV710AF65F without clearing its data and launched
successfully: cold launch 391 ms, total wait 398 ms.

## Newly available XQ-AT72 and remaining evidence

QV710AF65F / XQ-AT72 / Android 12 joined the existing deferred device matrix.
With official-signed host 5297, Agent 1.1.0 / 87 debug and 3-Stone AI 1.2.0 / 216
R8, the host entry batch passed 5/5 (1.705 s; 413 ms to production attachment)
and the plugin compatibility batch passed 8/8 (8.878 s). These results cover
launch/discovery/attachment and deterministic UI interaction, not a generated
source task or final Agent R8 runtime acceptance.

The initial public model catalog completed with zero targets and initial
accessibility preflight was disabled. The user then prepared both requirements.
A fresh public catalog found an available Model8 target with native tools;
the Provider's visible model selector identified `claude-fable-5-1`. No private
credentials were read. The separate Arabic RTL / dark / font-2.0 UI audit passed
4/4 in 12.598 s, with zero issues across 28 measured states. Font scale returned
to 1.0 and this audit did not change the current 600000 ms screen timeout.

The exact final R8 candidate was installed for online task follow-up. Gemma
4 E2B IT was downloading; it was not needed for online inference, but Wi-Fi
switching was deferred until the download completed or paused, to avoid
interrupting it or transferring a large download onto the SIM. Calculator and
Wi-Fi results are recorded in the [XQ-AT72 follow-up](p7-xqat72-followup-2026-09-26.md).
The earlier screen-timeout cleanup attribution limit is retained there and
is not reported as complete restoration of that earlier batch.

The single online generated-source run completed on the exact final R8 Agent,
real host and Model8 / Fable 5.1. Its full source was
`ai.agent.result(12 * 34);`. The operator checked the pending source before
granting ONCE; confirmation remained SENSITIVE with no run-scoped allowance,
and no execution occurred beforehand. Real Rhino returned the structured value
408, with exactly one source execution and a completed task: 2 steps, 1 tool,
2 model calls, 16008 input / 152 output tokens (`estimated=false`), 29473 ms.
The prior attempt with the globally disabled tool group was rejected at
startRun without a run ID or model call and remains recorded as such.

The native Wi-Fi sample also completed, while HiPER calculator exhausted its
token budget without the required result and JSON Wi-Fi returned three empty
model responses. They are not presented as P9.1 acceptance for both paths.
No online vision target was used. Generated scripts and Provider metered access
were restored to false, Wi-Fi was restored and independently validated, and
the user-configured accessibility services were preserved. The device receipt
contains exact timing and preflight limitations. The SIM can be released until
another online Wi-Fi case requires it.

Private raw logs, source-containing fixtures and artifact receipts remain in
ignored build directories. No credentials or unrelated device data belong in
this document. The deterministic host, companion, UI and compatibility checks
do not establish a model's generation quality. Real tasks retain their own
status and observations, including failures, in the device follow-up.

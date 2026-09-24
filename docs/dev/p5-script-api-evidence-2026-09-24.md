# P5 script API evidence

Date: 2026-09-24. Host: AutoJs6 6.8.0 / 5293. Plugin: 1.0.0 / 53.
The original P5.1, P5.2 and P5.3 scope is implemented without adding, splitting
or removing roadmap items. P6 interface work and P7 reliability work remain open.

## Public behavior

- ai.agent exposes run/create/get/list/catalog/presets/status alongside the
  existing registered-script result/context. Arguments are validated before
  admission. Missing or disconnected plugins yield a failed handle with a
  rejected result and an enable/connect hint, without a synchronous run error.
- AgentRun has read-only metadata, on/off/once, respond/confirm/cancel, a result
  Promise, and join. Events and JSON conversion run on the creating script
  thread. join pumps that handle's event queue so input and confirmation can
  still be answered; UI-thread and recursive joins are rejected. JOIN_TIMEOUT
  does not cancel the task.
- Ordinary tasks keep their owner script alive and are cancelled when that
  script is stopped. Detached tasks survive script exit and can be observed by
  a later script. Reading a detached handle's result explicitly keeps the
  observing script alive. A get observer never becomes the task's owner.
- Task failure/cancellation resolves AgentResult; link/admission/control errors
  reject the handle without fabricating a successful task result. Cancellation
  cannot undo external actions already performed.
- The plugin's public Binder endpoint accepts answers only for script-owned
  interaction. The private launcher endpoint accepts only plugin-owned
  interaction. This is independently enforced beyond the public JS wrapper.
  List summaries retain the preset field, including a default for old records.
- Assistant options are independent snapshots. Per-run overrides cannot expand
  a fixed budget, tool group set, script root set or cautious confirmation policy.
- In-process detached runs retain the original callback in a host-owned registry
  without retaining a ScriptRuntime. Foreign/UI runs use bounded snapshot polling;
  transient states can be missed and old steps are not replayed. Process restart
  never automatically repeats a task or restores previous permissions.

## Verification

| Check | Result |
| --- | --- |
| Host focused JVM regression | 72 tests, 0 failures/errors/skips |
| AI Agent plugin JVM regression | 373 tests, 0 failures/errors/skips |
| Public JS API, API 37 x86_64 / 16 KiB AVD | 11/11 |
| Public JS API, Sony G8441 / Android 9 / API 28 | 11/11 |
| Plugin Android regression, API 37 | 32/32 |
| Plugin builds | debug, androidTest, release/R8 and lint passed |
| Host builds | app debug and androidTest passed |
| Editor completion | 4 static verifiers, browser completer tests, LSP generation and runtime verification passed |
| Browser TypeScript Agent completion | 4 cases: module, returned handle, assistant and inferred input event |
| TypeScript | local aj6dts.bat -Publish generation/copy and positive/negative API smoke passed |
| Documentation | normalization and generator --check passed, 143 modules; offline dry run has zero changes |
| Offline Docs | debug, check, 2 JVM tests, lint and asset normalization passed |
| Localized documentation | host 10 languages, plugin 36 artifacts, Ace/Offline Docs 25 artifacts each |

The JS Android fixture installs and binds the production plugin with the real
host capability broker. Only model decisions are deterministic test responses.
It is E2/E3 integration evidence, not another online-model E4 benchmark. It checks
progress/input/confirmation/done ordering, callback thread identity, once/off,
read-only plugin interaction (including a raw Binder rejection), script-stop
cancellation, observer isolation, detached reattachment, list/catalog/presets/
status, assistant narrowing, link loss, unavailable plugins and join timeout.

Both devices execute the three actual APK assets exposed by the app.listSamples
catalog: ai/agent-run.js, ai/agent-input.js and ai/agent-detached.js. The input
sample receives its answer through execution arguments. The detached sample
exits before the model responds and a new script observes the same task. The
normal sample launch creates no persistent daily task; scheduling is an explicit
installSchedule example branch. No shopping or payment operation is performed.

The tests use UUID directories under the host's private cache and remove them
afterward. G8441 retains its original accessibility service list; the already
enabled host service was rebound after instrumentation. The AVD keeps its prior
accessibility-disabled setting. No account, model configuration, Wi-Fi or screen
timeout setting was changed by P5.

## Artifacts and limits

Build and raw test logs are ignored workspace evidence under host build/agent-p5-*
and plugin build/p5-*. Public docs are ai.md and the five Agent type pages.
Declarations are 4.21.0, Ace is 1.13.0 / 111, documentation versionCode is 75,
and Offline Docs is 6.8.0 / 56. Generated dependency declarations reflect the
already synchronized Rhino version; this work does not edit the upstream engine.

The current plugin still offers only the default preset and its existing bounded
history. Configurable presets, the complete workbench, confirmation surfaces and
expanded history stay in P6. Real shopping reliability, model cost and the full
conformance matrix remain P7 concerns. The separate P3 E4 document records this
session's real-model cleanup test; none of these scripted-model tests replace it.

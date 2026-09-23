# P2.4 context compiler and model client evidence

## Context compiler

Implemented the original ContextCompiler item without changing roadmap stages.
The compiler retains system rules, the full goal and a marked current observation,
packs deterministic summaries before complete recent pairs, and places remaining
budgets last. All costs include JSON escaping, message envelopes and response
schema bytes. The effective ceiling is the minimum of the configured limit,
target context size, grant and the local 3000-token input allowance (0.4 token per
UTF-8 byte). An impossible minimum fails with LIMIT_EXCEEDED before model dispatch.

Local observations omit bounds and pure containers, prefer actionable/text nodes
and retain at most 70 complete rows. Selection runs before the journal's 24 KiB
observation cap, preserving useful nodes beyond a long container prefix. Node
references, snapshot identity and quoted screen text are retained. The compact
tool signatures derive from the same ToolCatalog schemas as runtime validation;
repeated selector schemas appear once. Historical pairs are removed before the
current observation is shortened. Optional memories and fixed context are marked
when trimmed. No model summary call is made.

Validation on 2026-09-23: JVM 183/183, including 19 new context, observation and
target tests. The default local empty-history fixture uses 5025 bytes / 2010
estimated tokens for English and 4839 bytes / 1936 tokens for Chinese, including
the response schema. Input plus admitted output stays within the local 4096-token
window. These are deterministic size estimates, not live model performance.

Public target metadata provides locality and structured-JSON capabilities but no
online wire protocol. Local targets are selected using locality; remote/hybrid
targets keep UNKNOWN protocol and plain decision JSON. Names, package names and
target IDs never imply a protocol. Explicit protocol variants remain available
for a future negotiated extension. No host contract or Provider binding is added.

## Model client and runner integration

ModelClient serializes the host generation request and validates the host event
envelope through a pure ModelBrokerTransport port. The port is the P2.5 seam for
IAiAgentModelBroker.generate/cancel, not a replacement Binder interface. It has
no Provider component, credentials or network implementation. API AAR staging,
Bundle/FD transfer and host-death attachment remain in the original P2.5 item.

- Correlation requires the request ID, contiguous event sequence starting at 1,
  started before payloads, consecutive chunk indexes, and the selected target ID
  on completion. Duplicate terminal events are rejected and cancelled at most
  once; an already published terminal result is never retracted or published again.
- The 64 KiB output limit counts UTF-8, including accumulated chunks. Streamed
  text must match the full terminal text. Strict JSON decoding rejects duplicate
  keys, invalid field types, negative/fractional/overflow usage and raw error text.
- Async cancellation and deadlines settle once. A separate worker can use await;
  thread admission is mandatory, waiting has an independent timeout, and an
  interrupted worker restores its interrupt flag. The production P2.5 adapter
  must exclude Android main, Binder and runner threads from blocking waits.
- Reported usage is retained on failures, timeouts, explicit cancellation and
  host loss, including when the runner's own deadline wins. Missing usage uses
  the existing bounded input/output estimate. No private payload enters logs.
- TARGET_UNSUPPORTED allows one structured-to-plain fallback. REQUEST_REJECTED
  alone permits one eligible online object-to-string Schema fallback. The runner
  recompiles context, reserves a fresh model call and preserves the step's two
  decision repair attempts. Normal model failures do not trigger retries. A
  link-owned client remembers format refusal for its selected target; discarding
  the link discards this state. The journal records the decision's degraded flag.
- Public capabilityIds and supportedControls negotiate streaming, explicit
  response schemas and output token limits independently. A target lacking
  streaming uses the ordinary ask path. A missing output-token control fails
  TARGET_UNSUPPORTED before dispatch and does not trigger Schema fallback, since
  silently dropping that ceiling would bypass token admission. Public target IDs
  follow the AiCommon grammar instead of an ad hoc local/profile name pattern.

## Final validation

2026-09-23, plugin 1.0.0 / build 20, host AutoJs6 6.8.0 / build 5285:

| Check | Result |
| --- | --- |
| JVM suite | 209/209, no failures/errors/skips; 45 added across P2.4 |
| JVM race test | 40 concurrent cancellation/completion interleavings, one callback each |
| Android instrumentation | 12/12 on private SDK 37 / Android 17 / x86_64 / 16384-byte-page AVD |
| Debug and androidTest APKs | Built and installed; installed plugin versionCode = 20 |
| Release APK and R8 | Passed, no native libraries |
| lintDebug | 0 errors, 5 existing warnings (2 dependency updates, unused round icon, 2 duplicate icons) |
| Markdown generator | 10 languages / 36 artifacts consistent |

Commands: testDebugUnitTest, assembleDebug, assembleDebugAndroidTest, lintDebug
and assembleRelease under JDK 21. Instrumentation used explicit
`adb -s emulator-5584 install -r` followed by
`adb -s emulator-5584 shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner`.
This runs the entire test APK without deploying to attached physical devices.
The private AVD uses read-only/no-window/no-snapshot and is stopped after validation.

The Android cases exercise packaged en/zh prompts, the real context compiler,
model client and serial runner through a scripted host JSON transport, plus a
real worker timeout/cancel and rejection of main-thread blocking. These are E1/E2
evidence, not real Binder attachment, real Provider inference or E4 task success.
The installed preview still shows host status; P2.5, P3/P4 and P5/P6 retain their
original host service, execution adapter, script API and workbench responsibilities.

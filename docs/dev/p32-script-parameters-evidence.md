# P3.2 script parameter and confirmation evidence

Date: 2026-09-23. Plugin: 1.0.0 / 30. Minimum host: AutoJs6 6.8.0 / 5286.
All three original P3.2 items retain their scope. No host, other plugin, public
JS API, AIDL, permission or API AAR changed in this stage.

## Admission and confirmation

- RegisteredScriptTools resolves a model-selected ID only in the catalog for
  this task's approved roots. Unknown or ambiguous IDs cannot reach manifest
  inspection. It calls agent.readManifest with the observed canonical path,
  checks that the returned ID/path still match, and uses the fresh schema,
  risk, confirmation policy and timeout. Model arguments cannot set those fields.
- DecisionValidator delegates the registration-specific check to ScriptParameters
  after the asynchronous read. It implements the host's scalar schema subset:
  strict string/number/integer/boolean types, finite numbers, numeric enum
  equality, defaults, inclusive numeric bounds and Unicode code-point lengths.
  Unsupported keywords, invalid schemas, nulls, containers and extra keys are
  rejected. Defaults precede the required check. Original and effective argument
  objects must each fit in 16 KiB; callers and cached metadata remain immutable.
- Missing values produce TOOL_ARGUMENTS_INVALID with SCRIPT_PARAMETERS_MISSING,
  bounded parameter names/reasons and an ask hint. Supplied values and parser
  exception messages are never copied into that error. This is a new tool
  observation and model turn, not a malformed decision or a JSON repair retry.
  Direct ask is also supported. Answers carrying memoryKey retain
  memoryProposalOnly:true; they do not write memory.
- Sensitive registrations and confirm:before-run require confirmation on every
  call. They cannot acquire task-wide permission for other script_run calls.
  PreparedScript owns an immutable registration plus effective parameters, used
  by the confirmation description, parameter table and future ScriptInvoker.
- The launcher renders a scrollable, plain-text two-column table with localized
  headers in all 10 languages. Values use JSON scalar notation, preserving type,
  quotes and escaped line breaks without interpreting markup. Confirmation
  contains the complete effective parameters, even above the old 4 KiB summary
  limit. Description packing counts JSON escaping to stay inside the 32 KiB
  Binder event limit together with a near-16 KiB argument object.
- Inspection cancellation and duplicate/late replies cannot complete preparation
  twice. Denial or a late confirmation after cancellation cannot call execution.

## Scoped memory injection

- HostLink takes one immutable memory snapshot on its worker at task preparation.
  It reads only the plugin-private agent-memory.json. No model, host argument,
  registered script or ask response can supply this file through the control API.
  Disabling options.memory or the memory tool group skips reading entirely.
- The read-side format is a closed JSON object with version:1 and entries:[...].
  Each entry contains exactly key, value, scope, sourceRunId, createdAt, updatedAt.
  Keys have at most 64 code points, string values at most 4096, scopes at most
  128; keys/scopes cannot contain control characters. Source IDs are task UUIDs,
  timestamps are nonnegative integers with updatedAt >= createdAt, and each
  (scope,key) pair is unique. The file is capped at 500 entries / 256 KiB.
- Only global and the current preset survive filtering. A preset-specific value
  wins an exact-key collision with global. Entries are then ordered by updatedAt
  descending with deterministic key ties. The prompt receives key/value/scope,
  up to 4 KiB of serialized UTF-8 JSON, retaining a prefix of complete entries.
  An entry too large for the remaining budget ends packing; no value is cut.
- The source reads AtomicFile with a streaming byte bound and strict UTF-8.
  A missing file means empty memory. Corrupt, oversized or invalid files produce
  memoryUnavailable:true and no entries; no partial recovery or implicit write.
  memoryTruncated survives later ContextCompiler packing, including local models.
- English and Chinese full/compact prompts describe exact-key parameter use,
  explicit task value precedence, type checks and proposal-only memoryKey.
  Injected memory is JSON data, never instructions or authorization. There is no
  automatic parameter overwrite or string-to-number conversion.
- P6 remains responsible for confirmed writes, credential exclusion at proposal
  admission, management/import/export UI and named presets. The current public
  entry accepts only preset:default. A new installation has no saved entries.
  These read-side tests seed isolated private fixtures; they are not evidence of
  a completed memory_propose persistence workflow.

## Validation

Temurin 21 validation completed on the final build 30:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:assembleDebugAndroidTest
:app:lintDebug
:app:assembleRelease
py .python/generate_markdown.py --check
```

- JVM: 270 tests, 0 failures/errors/skips. P3.2 adds 32 cases: 10 scalar-schema
  cases, 7 admission/confirmation cases, 8 memory cases, 1 control-option case
  and 6 complete runner flows. Type/enum/default/extra-key matrices include
  numeric equivalence, UTF-8 byte boundaries, Unicode code points, immutable
  copies, malformed registration, roots, late replies and cancellation.
- Runner flows verify missing parameters -> ask -> answer -> confirmation,
  effective defaults, direct ask, denial, repeated sensitive confirmation,
  ordinary/read-only automatic admission and late confirmation after cancel.
  Execution uses an explicit FakeTools adapter. No real script is started.
- Android API 24, x86, 4 KiB pages: 23/23 tests passed in 2.907 seconds.
- Android API 37, x86_64, 16 KiB pages: 23/23 tests passed in 4.173 seconds.
  Three new device cases cover the actual parameter table and private AtomicFile
  snapshots, including opt-out, invalid UTF-8, malformed and oversized input.
  Existing Binder, service, runner, model and settings coverage also passes.
- Debug, instrumentation and release/R8 builds pass. Native alignment guards
  confirm the unchanged no-native-library packaging. Lint reports 0 errors and
  the same 6 existing warnings. Documentation generation verifies 10 languages
  and 36 artifacts. Prompt snapshot changes were reviewed: only the English and
  Chinese memory-use rules were appended; other reviewed fields are unchanged.
- Both devices are private read-only AVD instances. Physical devices were not
  used. No real model, real host script round trip, external request, push or
  publication is claimed by these results. This is E0/E1 evidence, not E4.

## Next original stage

P3.3 supplies ScriptInvoker. The production execution adapter still returns
TOOL_DISABLED for a prepared script; confirmation is not evidence of execution.
ScriptInvoker must use PreparedScript's canonical registration and effective
parameters, retain the runner's invocation identity, and recheck/reject stale
registration while waiting for user confirmation. It must not pass the model ID
from the preliminary ToolPlan directly as a host file path.

Real host script execution, result mapping, cancellation/timeout stopping and
sample scripts stay in P3.3. Public ai.agent script APIs remain in P5; memory
writes and the workbench remain in P6; physical-device/real-model E4 remains open.

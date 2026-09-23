# P2.2 decision core evidence (2026-09-23)

P2.2 implements the original flat decision protocol, parser, validator and prompt
catalog. It does not execute a tool or contact a model. The P2.3 runner consumes
accepted decisions and applies live risk/confirmation; the P2.4 ModelClient
consumes schema selections and performs broker calls and fallback accounting.

## Decision admission

- Strict mode accepts one complete JSON object. Degraded mode extracts the first
  balanced object, respecting quoted strings/escapes, and records `EXTRACTED`.
  A malformed first object is rejected rather than skipped for a later one.
  A plain complete object records `STRICT` even in degraded mode.
- The 64 KiB response bound, duplicate-key, nesting, numeric and Unicode checks
  also apply in degraded mode. String-encoded arguments use the same strict
  decoder; they cannot contain a second object, a fence or duplicate keys.
- Only the `kind` branch may have values. Inactive null placeholders are allowed.
  Tool existence, group availability, input schema and handler admission all run
  before an accepted tool decision is produced. Coordinates cannot enter act
  tools; parameter strings are accepted only when that encoding was negotiated.
- OpenAI object arguments restore optional null placeholders using the selected
  tool's original schema. Unknown null keys and required null values still fail;
  real null-valued registered script parameters remain intact.
- `reasoning` is clipped at 600 Unicode code points. Ask question/memory key and
  done summary/evidence/unfinished limits are checked locally. Choice questions
  require 1-8 distinct choices, each at most 200 characters; other ask kinds do
  not carry choices. Blank required text is rejected.
- One `DecisionRepairSession` belongs to one step: at most two repair responses
  after the initial response, in structured and degraded modes alike. Success or
  exhaustion settles the session. Repair observations contain fixed diagnostics,
  attempt counts and remaining allowance, never fragments of rejected output.
- A structurally valid `done` is not verified completion. P4 retains the actual
  observation/evidence and order-state checks from the roadmap.

## Schema selection and protocol boundaries

Local decoding keeps object arguments, as measured in P0.2. Online protocols try
an object schema enumerating enabled tool shapes. If a tool contains dynamic
parameter names (`script_run`), the schema exceeds 16 KiB, or Anthropic complexity
limits would be exceeded, arguments use a JSON string while the local validator
continues to enforce the original object schema. Disabled tools do not appear
in the tool enum or object alternatives. An all-disabled catalog still supports
ask/done.

OpenAI schemas close every object, require every field, and represent omitted
optional fields using null alternatives. This follows the official
[Structured Outputs requirements](https://developers.openai.com/api/docs/guides/structured-outputs).

Anthropic schemas close objects, allow optional fields and limit the generated
schema to 24 optional parameters and 16 union parameters. Unsupported value
constraints are applied locally, consistent with its
[structured output limits](https://platform.claude.com/docs/en/build-with-claude/structured-outputs).

Gemini mapping follows the existing 3-Stone `generationConfig.responseSchema`
wire field: no `additionalProperties`, scalar `type`, and `nullable` where
needed. The newer `responseJsonSchema` field has different support and is not
silently substituted. See the official
[GenerateContent Schema reference](https://ai.google.dev/api/generate-content#Schema).

These sources were checked on 2026-09-23 against the local 3-Stone request
builders. No model name or backend was changed. All online variants omit length,
item and numeric bounds conservatively; local validation retains those bounds.
The generated schemas and format metadata have reviewed snapshots.

`SchemaFallbacks` remembers at most 32 provider/target/protocol identities per
link. Only an explicit online `REQUEST_REJECTED` can change an object selection
to a string selection, once. Generic failures, cancellation, timeouts, local
targets and an already selected string encoding do not trigger that retry.
The P1 host now exposes this stable reason, so the earlier P0 suggestion of
retrying any `PROVIDER_FAILED` is no longer necessary.

The current host target catalog has no public online protocol field. P2.4 must
supply trusted protocol metadata during ModelClient negotiation; neither opaque
target IDs nor the Provider package identify an HTTP protocol. Until known,
`UNKNOWN` selects the documented plain-JSON/degraded path with no response
schema. No provider credentials or private profile settings are read here.

## Prompts and validation

Eight packaged assets cover en/zh system, goal, observation and repair messages.
The observe/act/verify rules adapt the existing MCP `automate_task` guidance.
Tool descriptions come from `ToolCatalog`, not a second list. Context/memory and
observation values are JSON-encoded data; inserted strings are not expanded as
template placeholders. Fixed context, memory and goal limits are 8/4/4 KiB.
P2.4/P6 own scope filtering, recency truncation and total context compilation.
Templates normalize Git CRLF/LF differences and retain a memory-truncated flag.

Environment: Windows / Temurin 21, plugin 1.0.0 / 14, host 6.8.0 / 5285.

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease --console=plain
py .python/generate_markdown.py --check
```

- JVM: 108/108, no failures/errors/skips. Includes 25 parsing matrix cases,
  all 30 tools in both argument encodings, active-branch/length/null validation,
  repair exhaustion, provider selection/complexity/fallback and en/zh snapshots.
- Debug/androidTest and Release/R8 builds passed, including no-native-library
  guards. Lint: 0 errors, the same 5 dependency/icon warnings recorded for P2.1.
- Private read-only x86_64 AVD, SDK 37 / Android 17 / 16 KiB pages (AVD image
  label `AVD_API_37.1_16K`), serial `emulator-5584`: 8/8, 0.282 s.
  Tests cover actual packaged prompts, Android regex/template parsing, decision
  repair, online argument encoding, Unicode limits and existing plugin contracts.
- Ten language README/changelog sources and all 36 generated artifacts agree.
  Text-only finalization is checked separately with the punctuation test.
- No physical devices, real model calls, credential stores or external services
  were modified. Actual online model acceptance/quality and end-to-end tasks
  are not claimed by the JVM/Android schema checks.

Ignored logs: `build/agent-p22-validation.log`, `build/agent-p22-device.log`.
Initial snapshot tests intentionally produced review candidates under `build/`;
approved fixtures are in `app/src/test/resources/`. Tests cannot overwrite them.
No new dependency or API AAR was added in P2.2; AAR staging remains at P2.5.

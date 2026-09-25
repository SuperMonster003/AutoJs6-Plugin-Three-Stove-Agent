# P9.1 Agent native tool loop evidence

Date: 2026-09-25. Scope: the original P9.1 **plugin** item. Agent 1.1.0 development candidate / build 82. The original P9.1 test item remains open for the combined fake-Provider path and real Wi-Fi/calculator comparisons. This record does not claim cloud-model acceptance, a release publication, or new physical-device coverage.

## Runtime behavior

- `ModelTarget` requires both the target's `tools` capability and the host's known `toolCallingVersion=1` extension. No extension, an unknown version, or a target without tools keeps D7 JSON decisions. LiteRT targets that do not advertise tools remain on that path. No new public JS option or model credentials are introduced.
- `ContextCompiler` derives native definitions directly from the enabled `ToolCatalog`, accounts for their bytes, and retains the context/grant limits. Native response instructions have English and Chinese assets. The final rendered JSON-mode prompts remain identical to the reviewed pre-P9 snapshots; local packing was not weakened.
- `ModelClient` starts one broker generation, accepts bounded `tool_calls`, and continues the same callback through `submitToolResults`. IDs, declared names, event/chunk sequences, rounds, complete result batches, cumulative usage and transcript consistency are checked. Native text responses are limited to the existing ask/done decisions; printing a tool decision as text consumes the bounded repair allowance.
- The whole batch passes `DecisionValidator` before any preparation. Valid calls execute sequentially, each with the existing `ConfirmationGate`, `LoopRules`, prepared-node/script identity and `StepJournal`. A denied or expired confirmation returns an error result without execution. Payments cannot acquire run-wide approval. Rejected batches return fixed diagnostics, with at most two repairs in the same step.
- Each actual continuation needs another model-call/token admission. Additional tools in the same batch use separate steps without fictitious model calls. Cumulative usage becomes round deltas; an optional total appearing later does not charge already reported input/output again. Late usage while paused is consumed on termination. Missing usage retains conservative estimates.
- Paused host failures, cancellation, the original model deadline and the run deadline terminate the conversation. The callback-to-run ownership transfer is explicit, including cancellation before the runner accepts a delivered pause. No action is replayed through JSON after a native continuation fails. An initial unsupported/rejected native request may select the existing JSON fallback before any tool action.

## Limits retained from the host contract

One native conversation has at most 16 tool rounds and 32 pending calls. Negotiated result limits are at most 64 KiB per output and 128 KiB for the complete JSON batch. Output escaping and per-call compaction count toward the batch limit; a batch too small to carry bounded feedback is rejected before preparation. Tool results carry remaining allowances and verification counters as data.

The original request's deadline and output-token ceiling apply across its tool rounds, including time spent confirming or running tools. The runner's current default output ceiling is 2048 tokens. Context grows within the compiler/target/grant ceiling; it is not silently trimmed or restarted during a native conversation. The host continuation method cannot lower the original output ceiling, so a later reservation too small to cover it stops with `BUDGET_EXCEEDED` before submission. These limits may end a task before its overall step budget; real-task behavior still needs the original P9.1 comparison item. An ask ends the current model conversation, and answering starts a fresh bounded generation with the journal context.

## Host API artifacts

All three release AARs were assembled in one invocation from clean AutoJs6 `3e4e3a3cffe8b8ebdcb1805e6895a72794828d4e`, 6.8.0 / build 5297, then staged together. Their hashes are enforced by `locks/host-api-aars.lock`; provenance and notices are updated. The plugin build never resolves a sibling checkout.

| Artifact | SHA-256 |
|---|---|
| common-plugin-api.aar | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |
| host-capability-api.aar | `23024fd981b7846936ef59f595f4da2208adf38d7c2d35594c20c7dfc1a86bd1` |
| ai-agent-api.aar | `4f97269b45904181e7fa1cde27c819eeaa8f9ca47f71092b3281efe0ed2dd57b` |

The base Agent contract stays V1; the original five model-broker transactions keep their order and the continuation method is appended. Attachment still supports build 5289+, task APIs require 5293+, and native negotiation needs host build 5297+. `AgentStepEvent.decision` is already a `JsonObject` in the public TypeScript declarations, so the internal `NATIVE_TOOL` diagnostic does not require a JS/types API change.

## Validation

Temurin 21.0.12.1+1, published platform build plugins 1.8.3. Main-checkout evidence is in ignored `build/p91-agent-private/`; isolated-device evidence is in `D:/idea-projects/AutoJs6-Agent-P91-Verification-20260925/build/p91-agent-private/`.

| Check | Result |
|---|---|
| Final complete JVM suite | 506 passed, 1 existing performance-switch skip; 507 total, including 30 new native regression tests |
| Debug APK and Android test APK | Passed |
| Agent lint | 0 errors, 6 existing warnings |
| Fake-host build and lint | Passed, 0 errors / 2 existing warnings |
| R8 release, signature and single-APK digest archive | Passed |
| Ten-language generator / read-only check | 10 languages, 36 artifacts, passed |
| API 24 / x86 full Android suite | 80 passed + 2 opt-in screenshot skips, 371.893 s |
| API 37.1 / x86_64 / 16 KiB full Android suite | 80 passed + 2 opt-in screenshot skips, 350.330 s |
| Final API 24 external conformance, debug / R8 release | 8/8 in 1.364 s / 8/8 in 1.135 s |
| Final API 37.1 external conformance, debug / R8 release | 8/8 in 2.141 s / 8/8 in 1.353 s |

The full Android runs preceded the final native-only fix for a later-reported cumulative token total. After that fix the complete JVM/build/lint/R8 checks ran again, and both devices repeated the external eight-test matrix against **both** debug and R8 release. That matrix now deliberately omits the first total and supplies it at completion, asserting 34 tokens rather than 49. The unchanged JSON/UI tests are not represented as newly rerun after that last fix.

The disposable AVDs are `AI_Agent_Conformance_P91_API24` and `AI_Agent_Conformance_P91_API37`, in a separate data directory. Installation uses `test-apps/fake-host/run_conformance.py`, which refuses physical devices and real host installations. The fake host keeps the real host package/version checks and uses a separate process/UID from the Agent. Eight tests include native result submission, whole-batch repair before execution, old-host fallback, paused cancellation and real broker-process death. These are deterministic **fake-host** tests; they do not replace a real host plus fake Provider or the online-model comparisons.

All fake-host/device builds use an isolated checkout and the standard Android test key. The external instrumentation targets the fake-host APK and invokes the Agent through Binder, so the R8 result does not rely on loading debug instrumentation into a release app. Production signature checks and shrinker rules were not relaxed. The production and test-signed release APKs have identical DEX, resources and manifest; the only non-signature payload differences are LF/CRLF in 12 existing text assets, verified byte-for-byte after newline normalization. JSON readers and the prompt loader normalize these existing files.

Final production-signed local candidate:

- `releases/autojs6-plugin-ai-agent-v1.1.0-6ccf9b95.apk`, 660334 bytes.
- SHA-256 `665238f325ce384333470b8e3ac0603c047063f8c4573e7e95294f6f9ddeb205`; CRC32 `6ccf9b95`.
- `classes.dex` SHA-256 `a19251f1f11dc45b4d0cff83f54fdba78633a00dde981fae2ce5bec9280f203a`.
- APK v2 signer SHA-256 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
- Test signer SHA-256 `2e64822e13a6c80c12e1c4b47e8fb32d1e9334526289da75777b7a79145de4b8`.
- Built before the feature commit from `4e0f46f` plus this working-tree change, with build 82; this is a development archive, not an exact-tag release. Earlier `66f09e5b` is superseded by this candidate.

## Failed attempts and corrections

- The first host API command omitted the `plugin-api` project prefix and failed task selection. The corrected three-module release build passed in 25 s.
- The first prompt refactor changed a reviewed snapshot and exceeded one local-context fixture's budget. The JSON output was restored exactly through separate response fragments; the original snapshot and packing assertions now pass without relaxed limits.
- The first complete 1.1.0 build found a curly French apostrophe in generated text. Sources were corrected to ASCII punctuation and regenerated; the final full suite and generator pass.
- Final native review found the late-total accounting case above. It has JVM and cross-UID debug/release regression coverage. Earlier successful full-Android evidence is retained with its scope rather than relabeled as a final-code full run.

## Remaining original work and user resources

Original P9.1 host, Provider and plugin implementation items now have evidence. Its test item remains unchecked: complete the combined fake-Provider round trip and compare native/JSON Wi-Fi and calculator cases using real models. P9.2 vision and P9.3 dynamic scripts are unchanged. No remote push, tag, GitHub/npm publication or physical-device install occurred in this session. Published Agent v1.0.0 remains unchanged.

Host and Provider source workspaces stayed clean; the concurrent Rhino work was not edited. No Wi-Fi change, real order or payment was performed. No SIM or new manual action is needed for this completed implementation; a later online Wi-Fi comparison needs one device with independent network access only during the case, not specifically Redmi. QV710AF65F / XQ-AT72 / Android 12 remains absent and untested, with the user's expected availability before 2026-09-27 20:00 UTC+8 retained.

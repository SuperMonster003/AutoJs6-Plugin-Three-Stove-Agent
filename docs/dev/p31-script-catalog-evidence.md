# P3.1 registered-script catalog evidence

Date: 2026-09-23. Plugin: 1.0.0 / 27. Minimum host: AutoJs6 6.8.0 / 5286.
The three original P3.1 items retain their scope. P3.2/P3.3 parameter completion,
confirmation and script invocation, P5 public JS APIs and P6 workbench remain pending.

## Implemented behavior

- ScriptCatalogClient reads agent.listScripts through the granted host capability
  broker. One client belongs to one authenticated link. Completed catalogs have
  a 60-second TTL, every task start refreshes them, and configuration updates or
  detach invalidate them. Root sets partition the cache, with at most four cached
  snapshots and nine pending reads. Cancelled/late replies cannot repopulate it;
  one failed observer cannot prevent other requests from being invalidated.
- A catalog contains at most 500 records / 256 KiB. The parser rejects malformed
  records and duplicate paths; every record sharing an ambiguous ID is omitted.
  Catalog parsing has a bounded 131072-node allowance because legal dense
  catalogs can exceed the ordinary 16384-node model/observation allowance. Other
  requests retain their existing limits. The plugin never reads host files.
- ScriptRanker uses normalized lexical overlap in IDs, descriptions, tags and
  examples, with deterministic tie-breaking and Chinese bigrams. It selects at
  most 24 records. Query searches filter nonmatches; initial goal ranking retains
  fallback candidates. Ranking makes no model call.
- Compact JSON includes exact IDs, bounded descriptions and parameter summaries,
  risk/confirmation policy, and the first two examples. Complete records are
  removed to meet the 12 KiB presentation budget; omitted fields/records and
  ambiguous IDs are reported. Script paths and source text are never presented
  to the model. Parameter summaries do not replace full-manifest validation.
- Task preparation injects the registered-script section into both English and
  Chinese system prompts. Metadata is explicitly data and cannot change the
  goal or grants. Context packing can remove more whole candidates for a smaller
  model budget, including the existing 3000-token local input limit. Discovery
  errors remain explicit and differ from an empty catalog; host loss blocks the
  task. The read-only script_catalog tool uses the same cache and checks grants
  before reading cached results.
- The launcher opens a private script-directory settings page, with 10-language
  text, one absolute path per line and an explicit empty selection. The plugin
  checks syntax and stores the proposal in its UI process. An authenticated
  attachment request sends it to the host, which checks existence and canonical
  scope, preserves its existing grants, and saves only accepted configurations.
  The launcher compares saved settings to the accepted roots in link status.
  Changes cancel existing tasks and invalidate catalogs. No new storage,
  accessibility or network permission is requested.
- Task options.scriptRoots may only select a subset of the host-approved roots.
  Discovery sends roots:[".", ...selectedExtraRoots] to retain the working
  directory and its deeper agent/ subtree. The host accepts up to 33 selectors
  for one work directory plus 32 configured extras. The model cannot supply roots.

## Validation and findings

Temurin 21 plugin validation:

```text
:app:testDebugUnitTest
:app:assembleDebug
:app:assembleDebugAndroidTest
:app:lintDebug
:app:assembleRelease
```

- JVM: 238/238, including 22 catalog/ranking/context/root cases. Tests cover
  English/Chinese ranking, deterministic ties and locale independence, JSON
  presentation snapshots, truncation, duplicate IDs, TTL/task refresh, root
  partitioning, cancellation, late replies, invalidation, grant narrowing and
  local context packing. A 400-entry dense catalog proves that the catalog's
  parser allowance accepts data rejected by the ordinary model parser.
- Debug and release/R8 builds passed, including no-native-library alignment
  checks. Lint reports zero errors and six existing dependency, icon and
  application-context singleton warnings. No new settings/catalog warning.
- All 10 documentation languages and 36 generated artifacts match their sources.
  All three release API AARs were rebuilt together from host 57fbffaeff and
  restaged. Hashes match locks/host-api-aars.lock and THIRD_PARTY_NOTICES.md.
- Private read-only AVDs: API 24 / x86 / 4 KiB and API 37 / x86_64 / 16 KiB.
  Plugin instrumentation passed 20/20 on each, including real Bundle/Binder/FD
  handling, a 400-record dense catalog through a pipe and the real catalog tool
  adapter, descriptor cleanup, and settings save/reopen/reject/clear behavior.
  Final build 27 was rebuilt with debug/androidTest/release-R8/lint and the JVM
  suite in 48 s, then reinstalled and verified: API 24 20/20 in 3.398 s; API 37
  20/20 in 3.833 s. The real host catalog round trip was repeated against build
  27 on both: API 24 1/1 in 0.628 s; API 37 1/1 in 1.204 s.
- Host tests: 3176 JVM cases, zero failures/errors and six conditional skips;
  12/12 contract-module tests. Each AVD ran 27 host Agent entry, controller,
  ownership and installed-plugin cases: 26 passed and one Wi-Fi case skipped
  because its separate accessibility opt-in was not enabled. The original P2.5
  Wi-Fi evidence is unchanged. Host details are in
  [the host report](../../../AutoJs6/docs/dev/evidence/ai-agent-p31-20260923.md).
- The cross-process scripted-model fixture verifies 30 real registered scripts
  across 32 approved extra roots, Chinese goal ranking, an exact search result
  outside the first 24, cache reuse within a task, new-file visibility on the next
  task before TTL expiry, work-directory-only selection and rejection of widened
  roots. It asserts exact model/catalog call counts and no recorded tool error.
- Visual inspection on API 37 confirmed that the settings title, instructions,
  input and buttons fit below the status bar. The launcher now also respects
  system-window insets. Complete appearance integration remains P6.

The large-payload device fixture initially asserted a smaller inline ceiling than
the shared contract actually defines, then called the generic tool reply path
instead of the catalog decorator used in production. Both fixture mistakes were
corrected; the passing result above exercises the real catalog path. These runs
do not claim that the host must use a descriptor at a particular smaller size.

The host whole-suite run also exposed an existing mail test timing race. Commit
0d6f53677d waits for actual close-event delivery before checking the event order;
no mail production code changed. The final full host suite passed.

## Commits and remaining boundary

- Plugin 6beeca9: cache, ranking, compact presentation and initial JVM tests.
- Plugin 5636288: task/prompt/tool integration, root settings, host compatibility
  and translated resources/docs. The following test/evidence commit closes the
  original P3.1 test item and sets build 27 to the reachable commit count.
- Host 57fbffaeff: validated/persisted root proposals, selector boundary,
  integration tests, protocol docs, changelog and build 5286.

This is E0/E1/E2 evidence. Model target conformance:scripted is an injected test
broker; registered scripts contain a marker that must never execute. No real
model task, script execution, E4 acceptance, physical-device change or repository
push was performed. P3.2 is the next implementation point, with the original
roadmap stages and remaining acceptance gates preserved.

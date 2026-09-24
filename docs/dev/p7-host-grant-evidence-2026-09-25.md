# P7 host grant boundary evidence

This report belongs to the original P7 host grant matrix. No roadmap phase or acceptance item is added, split or discarded. The production change is in the AutoJs6 host; this repository records the result and preserves the remaining P7/P8 gates.

## Cross-process coverage

The host now includes `test-apps:ai-agent-conformance`, a debug-only, test-only APK with a distinct UID/process. Its driver accepts only the same-signer AutoJs6 host. It calls the production capability and model Binder stubs from its own worker and relays callbacks from its own UID, which the host tests verify. It has no network, storage or accessibility permissions.

`AiAgentGrantConformanceTest` covers method/permission denial, omitted permission declarations, grant/global UTF-8 size ceilings, actual Android accessibility query rate limiting and recovery, pending-call concurrency/revocation, all eight broker UID entry points, model input bytes, calls/minute, token reservation and settlement, private-body exclusion from host logs, and three non-Agent attach-broadcast identity cases. Successful requests provide positive controls before/after rejection. Capability failures check the stable error code and category; model quotas return `QUOTA_EXCEEDED`.

Model runners are deterministic fixtures injected into the real host model broker. The Android provider is real for permission enforcement, read-only accessibility rate queries and toast logging. This is host boundary verification, not online Model8 / LiteRT Gemma E4 acceptance.

## Actual defect

The pre-fix device run had eight passing tests and one failure: a plugin could set `log: true` on `toast.showToast`, causing its text to enter the host log. AutoJs6 6.8.0 / build 5295 (`ac7dc53444`) records only UTF-8 byte count and duration in the host-owned plugin bridge context. Toast display remains functional. Direct Node.js scripts retain their explicit text-logging behavior.

The attach receiver also records only a fixed identity-rejection diagnostic. Real ordered broadcasts from the same-signer conformance package, carrying missing, mistyped or foreign-created identity tokens, reach the manifest receiver and are rejected. A claimed Agent package-name extra does not authorize attachment.

The original shared host capability AIDL and Agent JS/AIDL contracts are unchanged. No API AAR refresh, minimum-host API change or Rhino source change is required. The host's 10-language changelog and protocol/testing documents record the log fix.

## Final checks

- Host JVM: 23/23 passed, no failures, errors or skips, covering grants, dispatch lifetime and model request/quota/event policy.
- Privacy regression: the first nine-case device run failed only the toast log assertion (2.063 s). The same case passed after the fix (0.353 s).
- Final device suite: 14/14 passed in 1.986 s on API 37.1 / 16 KiB pages. This is the nine-case foreign-UID matrix plus the five existing capability Binder tests, including descriptor ownership. Exact stable error codes and categories are checked in the final matrix.
- Host debug, instrumentation and conformance APKs assembled; host native alignment check passed. No runtime dependency change or release/R8 rebuild was required. Agent runtime source and binary are unchanged in this turn; build 63 belongs to this repository's roadmap/evidence commit.
- The conformance APK has 0 lint errors and 2 test-application warnings (`DataExtractionRules`, `MissingApplicationIcon`). Full host lint is tracked separately: its parallel Android-test analysis initially failed and other analyses had not finished after nearly 10 minutes; only that single-use Gradle daemon was stopped. Isolated serial Android-test analysis passed in 1m 27s without suppressions. The full serial report is recorded below.
- The test-only AIDL copies match byte-for-byte. Host 10-language changelog generation does not refresh unrelated online metadata; Agent generated Markdown remains synchronized.
- Only this session's API 37.1 AVD was operated on. Screen timeout, enabled accessibility services and accessibility_enabled were captured before testing, restored and verified. The driver APK was uninstalled and the emulator was closed. The four real devices were not used or modified. There was no real model generation, order, payment, push or publication.

Full serial host lint remains incomplete: after 10.52 minutes the single-worker retry was still analyzing main sources without a complete report, so this session stopped its own daemon. The resulting daemon-disappearance diagnostic is from that explicit stop. No lint rule or source set was disabled. Isolated Android-test analysis passed, but the whole-host lint result is not counted as a pass, and P7/P8 gates remain open. The final packaging/conformance-lint check passed separately in 28 s (`build/p7-grant-packaging-final.log` in the host). These tooling checks require no user data, device or manual operation.

## Remaining roadmap and user needs

The new module currently supplies the independent grant driver. The original later P7 conformance item still requires the full fake Agent `attach` / `startRun` / hostile-callback scenarios and the plugin-side fake host APK. P1.3's pending independent attach/detach/death evidence is also still open. None is marked complete merely because the test module now exists.

The next original item is the lifecycle matrix, followed by performance, power, the six-device matrix, security review and full UI/lint checks. P7/P8 gates remain open. QV770340J7 stays in the compatibility device list. Secure real-device lock screens must be avoided without a user available to unlock them.

This turn and the next deterministic lifecycle checks require no Redmi SIM or carrier data. A later online Wi-Fi-toggle case needs connectivity independent of Wi-Fi only for that case; a SIM, USB or Ethernet can provide it. There is no need to keep a SIM installed throughout development. No extra user data, model key, real order or payment is needed for the grant matrix.

Host source/evidence: `AutoJs6/test-apps/ai-agent-conformance/README.md`, `AutoJs6/docs/dev/evidence/ai-agent-p7-grants-20260925.md`, and `AiAgentGrantConformanceTest.kt` in the host Android tests.

# P2.1 tool catalog evidence (2026-09-23)

The packaged `assets/catalog/tools.json` contains the 30 first-version tools
from appendix C. README tables and model tool descriptions derive from this
catalog. Input schemas are closed at their roots and validated by the same
bounded `InputSchema` implementation used by tool admission. Registered script
parameter names remain manifest-dependent; their nested scalar map is bounded
to 16 KiB and will receive manifest validation in P3.

`ToolHandlers` prepares admitted bridge requests or explicit composite plans.
Polls, repeated scrolls, append-text reads, script-manifest resolution and local
memory operations do not execute in P2.1. Execution/confirmation, registered
script handling and UI flow handling remain in the original P2.3/P3/P4 stages.
Risk context must come from trusted host observations or script metadata;
model-supplied risk claims cannot override the catalog or lower sensitive risk.
The runner will load the ten-language keyword asset when assembling its policy.

`AgentJson` rejects duplicate keys, trailing JSON, malformed Unicode and
unbounded nesting/numbers. Observations remain valid UTF-8 JSON within their byte
budget, including escaped text and supplementary characters. Host error details
are reduced to stable categories; raw exception bodies do not enter prompts.

Two notification-panel actions in the catalog exposed a missing host grant.
The host now admits only `keys.notifications` and `keys.quickSettings` with their
required token. Appendix C.4 is synchronized with this mapping correction.

## Validation

Environment: Windows / Temurin 21; plugin 1.0.0 / 13, host 6.8.0 / 5285.

```powershell
.\gradlew.bat :app:testDebugUnitTest --rerun :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease --console=plain
py .python/generate_markdown.py --check
```

- JVM: 55/55, including all 30 tool mappings, catalog/keyword snapshots, schema
  rejection/defaults, disabled groups, sensitive risk, argument/observation
  bounds, Unicode and sanitized error mapping.
- Debug, androidTest and Release/R8 builds passed. No-native-library guards
  passed. Gson 2.13.2 matches the host; source/hash/license and its annotations
  dependency are recorded in `THIRD_PARTY_NOTICES.md`.
- Lint: 0 errors, 5 warnings (Gson/XZ newer versions, unused round icon and two
  duplicate icon configurations). None describes a new runtime correctness error.
- Private read-only AVD API 37.1 / x86_64 / 16 KiB pages, serial `emulator-5584`:
  6/6 instrumentation tests, 0.209 s. Includes packaged catalog/keyword loading,
  Android JSON/Unicode behavior and the four existing INFO/Manifest contracts.
- Host grant regression: 3/3, debug build and native alignment checks passed.
- Ten language documentation, 36 generated artifacts: `--check` passed.

Ignored logs: `build/agent-p21-validation.log`, `build/agent-p21-device.log`,
`build/agent-p21-runtime-dependencies.log`. No physical device was modified.
No real model, payment/order flow, task runner or cross-process Agent session
is claimed by this evidence. New API AARs remain scheduled for P2.5.

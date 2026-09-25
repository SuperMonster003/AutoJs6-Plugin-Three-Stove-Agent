# P7 UI accessibility and layout evidence

## Scope

Original P7 last item, Agent build 70. This closes the plugin UI/lint item without
adding or splitting roadmap stages. It does not certify every real-model task,
the offline XQ-AT72, or the host's whole-repository lint/release gate.

`UiAccessibilityAudit` inspects actual laid-out views, including descendants below
the scroll viewport: control labels, 48dp touch bounds, horizontal containment,
text height and unintended ellipsis. Activity configurations must match the
requested language, night mode and device font scale. The compact floating
progress preview intentionally uses ellipsis; its full task remains available in
the workbench. Representative private rendered screenshots were also inspected.
This is a view/layout audit, not a claim of a manual TalkBack speech evaluation.

Four instrumentation methods cover 28 presentations:

- Workbench, history, detail, preset list/editor, memory list/editor/import review,
  and approved script roots.
- Settings, clear/default/update dialogs, release history, license and notices.
- Actual `ConfirmationActivity` text/choice/yes-no questions and memory review,
  driven through the real private Binder and deterministic model fixture.
- Actual WindowManager floating windows: idle, running, entry, text, choice,
  yes-no, payment confirmation and script parameter confirmation. This layout
  fixture supplies controlled presentation snapshots through test-only reflection
  to an isolated runtime; it does not stand in for host authentication, a model
  invocation or a device action. Those paths retain the separate P6/P7 IPC tests.

Share/shortcut routing and system speech/permission/document-picker screens have
no additional plugin form to audit; their entry/cancellation tests remain in the
full suite. Screenshots stay in ignored private build output because management
screens can show existing device records. They are not public P8 screenshots.

## Reproduced problems and fixes

1. At normal font size, some checkboxes were 32dp high and edit fields 45dp high;
   an empty history filter could be 24dp high. Shared styling now establishes
   48dp minimum width/height even when host appearance is unavailable.
2. Platform spinner rows ellipsized long preset and scope labels. All plugin
   pickers now reuse the bounded multiline choice row, including model targets.
3. A fixed 64dp collapsed floating window clipped the stop label at font scale
   2.0. It now measures the header's natural height within the available display.
4. Android 7 RTL idle floating windows let the unused progress label displace the
   AI button. The idle collapsed window now hides that label.
5. Visual inspection found script parameter names squeezed to single characters
   beside long values. Parameter/value rows now share bounded column widths;
   a dedicated narrow-screen regression verifies a readable parameter column.

Initial English and Arabic audits failed on the first two problems. After those
fixes, Arabic floating and API 24 idle audits exposed the next two. The initial
script table passed the generic geometry assertions but failed visual review;
the additional regression covers that gap. Failed runs are retained separately.

## Device matrix

All following final layout runs passed 4/4. The same debug code was tested;
the later asset rebuild only synchronized the bundled changelog.

| Device | API | Language / theme / font scale | Result / seconds |
| --- | --- | --- | --- |
| AVD, 16 KiB pages, 320dp width | 37.1 | ar / dark / 2.0 | 4/4, 68.684 |
| AVD, 16 KiB pages, 320dp width | 37.1 | ar / light / 2.0 | 4/4, 27.929 |
| AVD, 16 KiB pages, 320dp width | 37.1 | en / light / 1.0 | 4/4, 40.992 |
| AVD, x86 | 24 | ar / dark / 2.0 | 4/4, 38.283 |
| Sony G8441 | 28 | ar / dark / 2.0 | 4/4, 14.656 |
| Redmi 12C | 33 | en / dark / 1.3 | 4/4, 21.220 |
| Sony XQ-DQ72 / QV770340J7 | 33 | ar / light / 2.0 | 4/4, 9.678 |
| Xiaomi Pad | 35 | zh / light / 2.0 | 4/4, 15.630 |

G8441 rebooted during the first APK installation; the user unlocked it and the
complete rerun above passed. No test deliberately locked this secure device.
After host APK/instrumentation changes, API 37 English runs also recorded
`HOST_UNAVAILABLE` and an interaction-card readiness timeout. The stable rerun
above passed without relaxing assertions. These failures are not UI passes.

Example, after setting and later restoring the device font scale:

```powershell
adb -s <serial> shell am instrument -w -r -e agent.ui.locale ar -e agent.ui.dark true -e agent.ui.font 2.0 -e class io.github.supermonster003.autojs6.plugin.ai.agent.ui.FloatingAccessibilityTest io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
```

Other audit methods are in `WorkbenchActivityTest` and
`SettingsActivityTest`. No production test endpoint, exported component or
authentication bypass was introduced.

## Build and host boundary

- Plugin JVM: 475 passed, one opt-in performance test skipped. Debug, androidTest,
  release/R8 and lint builds passed; lint remains 0 errors / 6 existing warnings.
- Ten-language changelog generation and all 36 generated artifact checks passed.
- Final API 37.1 / 16 KiB Android regression: 80/80, 332.465s. The API 24 script
  rendering regressions also passed 2/2, 0.076s.
- The host's first complete strict lint run finished in 9m 24s with 552 errors,
  2590 warnings and 3 hints. Errors: MissingTranslation 470, NewApi 67,
  AppCompatCustomView 6, WrongConstant 4, MissingPermission 2, and one each of
  MissingSuperCall, GestureBackNavigation and RestrictedApi.
- Two NewApi findings pointed to Agent model payload `Os.fcntlInt`, public only
  since API 30. The host now uses a nonblocking private file/pipe reopen on API
  24-29, preserving regular-file offsets and serializing reopen with cancellation.
  API 30+ retains the public fcntl path. API 24 and 37 each passed all nine model
  broker device tests, including the new file/pipe completion and offset case.
  The final `O_CLOEXEC` SDK-field-to-constant substitution was checked against
  SDK API history and the NDK definition (`0x80000`); the device APKs above used
  the equivalent flag value before that substitution. The final host source
  therefore still needs a clean combined build after concurrent work settles.
- The first model test round rejected the debug-signed fake Provider before
  exercising the model path. Re-signing the private fixture with the host key
  restored the intended test identity. An API 24 fake Provider teardown crash
  exposed a concurrent-set `toList()` race; direct iteration fixes it. Its 32 JVM
  tests passed. After an additional transient failed model completion, API 24's
  complete unchanged rerun passed 9/9 in 8.170s; API 37 passed 9/9 in 9.885s.
- The host's post-fix whole-lint attempt failed in Kotlin compilation while
  deleting a classes directory, concurrent with other workspace changes. No
  post-fix zero-error host report is claimed. Other processes' lint/Rhino/source
  changes are outside this commit. See the host's model broker testing document.

No real model, SIM, Wi-Fi/proxy setting, shopping account, order or payment was
needed. QV710AF65F / XQ-AT72 remains unexecuted until the user's expected
2026-09-27 20:00 UTC+8 availability. P8 stays in its original position.

## Final artifacts and cleanup

- Host commit `bb9c91aa28`, runtime APK build 5296. The commit includes only the
  payload change, its tests/fixture repair and matching ten-language changelog
  insertions. Concurrent source, translation and lint work remains outside it.
- Agent build 70 debug APK: 2267818 bytes, SHA-256
  `161aa29ab5d0dc934d280e8af752e32678740e5c0e315d7585fc95d393a1a53f`.
- Agent release APK: 650954 bytes, SHA-256
  `f23662d61d777daf996ebd8d2c0e1713fa3b7f3e492cfa79ad29a7dafbe77d60`.
  APK Signature Scheme v2 verification passed. The actual binary manifest has
  neither debug fixture components nor `testOnly`.
- All four physical devices have the final build 70 APK. Font scale, screen-off
  timeout, display size/density and applicable notification grants were checked
  against the saved values. Android normalizes `1` or an absent font setting to
  `1.0`; cleanup compares the effective scale and still restores the other keys.
- Test finally blocks restore overlay AppOps, appearance cache and temporary
  preferences, and delete only their own named records/private fixture stores.
  Temporary fake Provider packages were uninstalled from both AVDs. Both AVDs
  started for this turn were closed after settings restoration.
- No push or release was performed. Whole-host lint is a separate unresolved
  validation result; the first report is retained rather than labelled clean.

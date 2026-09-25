# P8 companion publication evidence

Date: 2026-09-25. This closes the original P8 item for the four companion
repositories. Agent installation, two-device real-model smoke and the Agent
1.0.0 Release remain separate gates in their original positions.

## Published versions

| Repository | Version | Source | Publication |
| --- | --- | --- | --- |
| AutoJs6-Documentation | Content 6.8.0, documentation build 79 | `5d3ec6e5eab6d8174581fa02bee401dcb91eacc5` | GitHub Pages deployment succeeded |
| AutoJs6-TypeScript-Declarations | 4.21.1 | `2841c40`, tag `v4.21.1` | npm package verified after the operator completed 2FA |
| AutoJs6-Plugin-Offline-Docs | 6.8.3, build 60, content 6.8.0 | `c19f2d7cff88553627c9c61c4676492187622414` | GitHub Release `v6.8.3`, one universal APK |
| AutoJs6-Plugin-Ace-Editor | 1.13.1, build 113 | `31490c19a4dab0d4466a86fca16af6608b56e1b1` | GitHub Release `v1.13.1`, five APKs |

Public destinations:

- [Online AI API documentation](https://docs.autojs6.com/#/ai)
- [Successful Pages deployment](https://github.com/SuperMonster003/AutoJs6-Documentation/actions/runs/36102667409)
- [Declarations 4.21.1](https://www.npmjs.com/package/@sm003/autojs6-dts/v/4.21.1)
- [Offline Documentation 6.8.3](https://github.com/SuperMonster003/AutoJs6-Plugin-Offline-Docs/releases/tag/v6.8.3)
- [Ace Editor 1.13.1](https://github.com/SuperMonster003/AutoJs6-Plugin-Ace-Editor/releases/tag/v1.13.1)

The independent Offline Documentation plugin release version is not the
documentation content version. Existing historical 6.8.1/6.8.2 records were
preserved; the published 6.8.0 tag was not replaced.

## Documentation and declarations

- Updated the AI module's Android API 24, host attach 5289, complete task API
  5293 and currently implemented 3-Stone AI requirements. Explicit model
  selection does not silently change targets; an available model is not a
  guarantee that it can complete a task. Native tools, visual model input and
  generated scripts remain planned 1.1.0 capabilities.
- Ran the canonical documentation BAT dry run and full offline verification.
  All 143 modules validated. The search index has 6224 entries and 2283816
  bytes. The online/offline inventories match; five otherwise unchanged JS/CSS
  files differ only by pre-existing CRLF/LF conventions.
- Offline provenance points to the exact documentation commit above.
  Plugin build, bundled content, licenses, manifest and publishability checks
  passed. Python tests 4/4, JVM tests 2/2, lint 0 errors / 27 warnings.
- Ran the canonical `aj6dts.bat -Publish` host export and TypeScript validation.
  The generated main, resource and library declarations had no byte changes.
  Updated Agent JSDoc for preset snapshots, target selection, budgets,
  cautious confirmation and the scope of `memory: false`.
- Removed the declaration package's erroneous dependency on itself. All 10
  existing TypeScript smoke files passed. An isolated tarball consumer and a
  second isolated consumer installed from the official npm registry each
  installed only one package and compiled the positive/negative Agent API
  fixture successfully.
- npm initially returned 401, then EOTP after login. The operator completed
  interactive authentication. Publication processing temporarily returned
  404; the final official registry metadata matched the validated tarball:
  SHA-1 `7dc6e2979831a087e1a23fec9c068e67e4160e64`, integrity
  `sha512-3egyTRdoBcwSyjNwKHlTI+qJQ97HXh73vYOZ0LtCGC/JMiXpBBVjNEDzZw4i2Rei18xX+l+tq3mTQoApCy/JUw==`.
- All 69 manual internal declarations and the aggregate declaration were
  synchronized into Ace. Five LSP groups were generated from package 4.21.1.
  Completion verification passed for 71 modules / 4391 members; the host's
  existing generated completion indices had no tracked changes.

## Ace ABI correction and validation

The 32-bit Ace APK on G8441 exposed a real LuaLS verification defect. Runtime
selection used the device's preferred 64-bit ABI, then compared the installed
32-bit executable with the 64-bit record. The fix reads the installed ELF
header, validates its ABI against device support, and retains the pinned
size/SHA-256 verification. Native binaries and their lockfile did not change.

The declaration update is commit `7862a10`; the ABI correction, malformed ELF
tests, test diagnostics and matching ten-language notes are commit `31490c1`.
The rename instrumentation now selects the dialog root explicitly.

- Temurin JVM: 171 passed. Debug, androidTest, signed R8 release, five ABI
  artifacts, native 16 KB alignment, LSP runtime and 25 generated Markdown
  artifacts passed. Lint: 0 errors / 47 existing warnings.
- API 37.1 / x86_64 / 16 KB: 11/11 contract, LuaLS and editor tests.
- G8441 / API 28 / armeabi-v7a, API 24 / x86 and Xiaomi Pad / API 35 / universal:
  3/3 contract/LuaLS tests per device. x86 checks the documented static Lua
  completion fallback, since no x86 LuaLS executable is supplied.
- Redmi 12C / API 33 / arm64-v8a: contract, LuaLS, startup, assets, read-only
  replacement, profile and project snapshot checks passed. Extended
  TypeScript diagnostics intermittently exceeded the existing 2000 ms budget
  (observed 2106-2158 ms), causing timeout and static fallback. Earlier
  individual diagnostic/rename passes do not turn the later failed diagnostic
  run into an all-green suite. The production budget was not increased.
- The optional ECJ compiler service R8 warning remains. Actual signed-release
  editor loading through the host passed on every tested device.

## Signed release execution

Host test commit `145a94eeeb` adds opt-in `CompanionReleaseSmokeTest`. It starts
the actual protected Wake activity and binds the separate release APK's INFO
service from the real host UID, checking its installed version and identity.
Existing host tests also read the offline content and load/run the Ace editor
across the APK boundary. These four tests passed on each environment:

| Environment | Ace artifact | Result |
| --- | --- | --- |
| Redmi 12C, API 33 | arm64-v8a | 4/4 |
| Sony G8441, API 28 | armeabi-v7a on a 64-bit device | 4/4 |
| Xiaomi Pad, API 35 | universal | 4/4 |
| Sony XQ-DQ72 | arm64-v8a, after reconnection | 4/4 |
| AVD API 24 | x86 | 4/4 |
| AVD API 37.1 / 16 KB | x86_64 | 4/4 |

Total: 24/24. Offline debug instrumentation also passed 2/2 on Redmi and
API 37. The first attempt incorrectly paired its debug test APK with an R8
release and failed with a missing Kotlin class. Release validation was
corrected to use the real host, rather than weakening R8 to accommodate a
debug-only test dependency. No ColorOS activation test was available.

Every final APK was checked for version, CRC32, signature, ABI inventory and
SHA-256. GitHub asset sizes/digests matched the local artifacts. The signer is
`31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
Exact APK receipts are recorded in the companion release evidence and the
official index's build 60 / build 113 admission manifests.

Official index commit `7f31dce` was pushed after generation. Only the two
companion entries changed, with all six APK admission receipts bound to the
published assets and source commits. Native payload measurement reports
16384-byte alignment for Ace and no native payload for Offline Documentation.
The index's 42 unit tests passed. An initial missing Ace ABI list was rejected
by admission validation; the receipt was completed from the actual four-ABI
release inventory before the successful full 60-entry generation.

## Scope and remaining work

The existing declaration `package.json` publish configuration edit and Ace's
older untracked `releases/` artifacts were preserved. No Rhino source was
modified. Companion testing did not invoke models, change networking, create
orders or pay for anything. Screen timeouts were restored after each run.

XQ-AT72 / Android 12 remains offline with the user's expected availability
before 2026-09-27 20:00 UTC+8. It is not recorded as passed. XQ-DQ72 returned
during this work and passed the additional signed companion tests above.

Agent's final publication gate must use its own final APK and real-model
evidence. Its official inventory entry requires a published Agent APK and
must not be replaced with a placeholder. Redmi does not need a retained SIM
for companion tests; the forthcoming online Wi-Fi case needs independent
networking only on the devices selected for that case.

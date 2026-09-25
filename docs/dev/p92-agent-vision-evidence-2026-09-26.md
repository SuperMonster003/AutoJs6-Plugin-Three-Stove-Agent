# P9.2 Agent screenshot observations

Date: 2026-09-26. Scope: the original P9.2 **plugin** item. Agent 1.1.0 development candidate / build 86. The host and Provider implementations remain at AutoJs6 `52ce694f92` / build 5297 and 3-Stone AI `d0ad293` / build 216. This receipt records deterministic implementation checks, not acceptance by a real online visual model.

## Behavior and compatibility

- The existing `observe` group gains `screen_capture`, with a closed empty parameter schema. The fixed conversion follows the original item: longest edge at most 1280, JPEG quality 70, no upscaling. Capture is exposed only when the host advertises the known `visionVersion=1` extension, the exact selected target advertises `vision`, Android is API 30+, and the effective grant contains `accessibility.screenshot`, `accessibility` and `screen_capture`. Disabling `observe` also disables capture. API 24-29 and text-only targets retain text observations.
- The Agent asks the existing host capability broker for a PNG screenshot. It validates the response ID, owner UID, schema, descriptor marker, byte count, MIME and dimensions; it then checks the decoded content before bounded JPEG conversion. Source dimensions are retained as `screenWidth` / `screenHeight` alongside image `width` / `height`. Model-controlled paths, quality and scaling are not accepted; the appendix's tentative scale parameter is replaced by the fixed P9.2 conversion.
- Capture requires an interactive, unlocked screen before dispatch and again before delivery. No new accessibility, screen capture, storage or network permission is added. The Agent has no direct Provider binding, API credentials or screenshot implementation; its image work is decoding, scaling and encoding the host result.
- English and Chinese visual instructions treat image text as untrusted observation data, explain coordinate scaling, prefer current node references, preserve gesture confirmations and require fresh observations after actions. The screenshot receipt alone is not completion evidence.

## Transport, retention and budgets

- JSON model requests attach the current image through `imageRefs` and `AiAgentContract.KEY_MODEL_IMAGE_FDS`, associated with the current observation message. Repair attempts retain that observation. A subsequent tool observation, error, user answer, denied confirmation or interaction timeout replaces it. Task termination clears the transient image reference. Step history contains dimensions and sizes, never the image bytes or base64.
- Native calls opt into vision at generation even before the first capture. The screenshot is attached to its own tool result, with global descriptor indices across the batch and no initial-message index. Native sessions keep their existing images in the host/Provider conversation; the Agent retains counts and estimates for admission. No action is replayed through JSON after a failed continuation.
- Incoming encoded screenshots are bounded to 8 MiB, with each edge at most 4096. The converted image obeys the negotiated ceilings, capped at 4 MiB per image, 4 images / 8 MiB per batch, and 16 images / 32 MiB per native session. Exact dimensions, MIME, SHA-256 and byte counts travel in the image references and are checked again by the host. Image bytes are separate from the existing text/context byte budget.
- The image estimate matches the host: `1024 + 4 * ceil(width / 32) * ceil(height / 32)`. A 1280 x 720 image reserves 4704 tokens. Every native model round reserves the retained images again, plus new ones and the existing text/output allowance. Actual reported usage replaces the estimate during settlement. Insufficient tokens reject the call before dispatch; session/context limits also fail before submission.
- The descriptor reader duplicates the received descriptor, sets nonblocking mode, preserves its current offset, checks the declared length through EOF, observes cancellation/deadlines, and checks reliable-pipe producer errors. This one-shot payload has one consumer; sharing its open-file flags with the sender is intentional. It does not reopen the foreign app's private inode through `/proc/self/fd`. A stalled producer cannot indefinitely occupy a cancelled worker. Late, duplicate, malformed and wrong-UID callbacks close received descriptors. The model adapter writes task-private temporary files, unlinks their image paths before Binder dispatch, and closes its descriptors after sending; large JSON payloads keep their separate existing descriptor path.

## Host API artifacts

All three release AARs were assembled together from clean AutoJs6 `52ce694f92`, then staged with matching provenance and notices. The common API bytes are unchanged. Gradle continues to verify local hashes and never resolves a sibling repository.

| Artifact | SHA-256 |
|---|---|
| common-plugin-api.aar | `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` |
| host-capability-api.aar | `0c9233f43848cc3a9d9a87071f46ecb71935964693a53de017529dfa2de79c28` |
| ai-agent-api.aar | `c47c46244346d78c6e05897f150f68ffe7b095802e88488d0b934eb67231e53f` |

The base Agent contract remains V1 and the existing build 5289+ attachment minimum is unchanged. Visual requests depend on negotiated image support, including the host P9.2 implementation and an explicitly enabled supported target. No public `ai.agent` JS signature or TypeScript declaration changes are needed.

## Validation

Temurin 21.0.12.1+1 and published platform build plugins 1.8.3. Logs and test APKs are in ignored `build/p92-agent-private/`; build logs are in ignored `build/p92-agent-*.log`.

| Check | Result |
|---|---|
| Complete JVM suite | 517 passed, 1 existing opt-in performance skip; 518 total, including 11 new visual regressions |
| Debug / Android test / fake-host builds | Passed |
| Agent lint | 0 errors, 6 existing warnings |
| Fake-host lint | 0 errors, 2 existing warnings |
| R8 release, signature and single-APK digest archive | Passed |
| Ten-language generator and read-only check | 10 languages, 36 artifacts, passed |
| API 24 / x86 Android suite | 83 passed + 2 existing opt-in README screenshot skips; 85 total, 6 API-30 capture cases SDK-filtered; 242.353 s |
| API 37.1 / x86_64 / 16 KiB Android suite | 89 passed + 2 existing opt-in README screenshot skips; 91 total; 276.262 s |
| API 24 external debug / R8 conformance | Each 9 passed + 2 API-30 visual skips, 11 total; 1.353 s / 1.205 s |
| API 37.1 external debug / R8 conformance | Each 10 passed + 1 old-Android case skip, 11 total; 2.631 s / 1.441 s |

The nine new Android tests use synthetic pixels. They cover portrait/landscape/small-image conversion, actual MIME/dimensions, regular-file offsets, short/long payloads, cancelled and timed-out stalled pipes, reliable-pipe failures, duplicate/late/wrong-UID responses, screenshot metadata, and read-only unlinked model descriptors in both initial and continuation requests, including a large JSON body. Six capture/reader tests require API 30, matching the actual capture feature and the public nonblocking `Os.fcntlInt` API. Conversion and model forwarding are also checked on API 24. The fake-host matrix adds real cross-UID screenshot-to-model round trips for JSON and native decisions, using an unlinked host-private 0600 file, plus pre-API-30 text compatibility.

Only newly created disposable `AI_Agent_Conformance_P92_API24` and `AI_Agent_Conformance_P92_API37` AVDs receive test APKs. The guarded `run_conformance.py` rejects physical devices and existing real host installations. Every installed test APK uses the standard Android test key. Debug and R8 Agent APK copies are re-signed without rebuilding; every ZIP entry outside signing metadata is verified identical to the corresponding production-signed APK. External instrumentation targets the fake host and invokes the actual R8 Agent through Binder. No production identity check or shrinker rule is relaxed.

Both complete Android suites and all four external debug/R8 combinations were rerun after the final descriptor correction. The earlier pre-correction full-suite runs and failed API-37 external run remain in the ignored logs. Both new AVDs were shut down with their data retained after validation; physical devices were unchanged.

Local development candidate, before the feature commit:

- `releases/autojs6-plugin-ai-agent-v1.1.0-f87369bf.apk`, 677904 bytes.
- SHA-256 `8232579c2f08cb1470b3d91dfb7d637a26b74a5d0f949b1f58c98594bc72f220`; CRC32 `f87369bf`.
- `classes.dex` SHA-256 `fce020b7c8a476ab839935791d0b55b51d335e34a8088564615024db56ae379b`.
- APK v2 signer SHA-256 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.
- Test signer SHA-256 `2e64822e13a6c80c12e1c4b47e8fb32d1e9334526289da75777b7a79145de4b8`.
- Built from `ec072d8` plus this change, with build 86. This is a local development archive, not a published or exact-tag release.
- Intermediate archives `a35a56a7` and `b3c584fa` are superseded by this candidate.

## Corrections and remaining acceptance

Review found that an old image could accompany a new failed tool observation, denied confirmation or timed-out interaction. These paths now clear it, as do all terminal states.

The first cross-UID visual test failed in both JSON and native mode with `TOOL_ARGUMENTS_INVALID` at capture, despite passing the same-process tests. The reader reopened `/proc/self/fd`, which rechecks access to the other application's private inode. It now uses `dup` to retain the capability actually granted through Binder, with an independently owned lifetime. Both formerly failing cases pass after the change, including R8, and the fixture explicitly checks a 0600 source file. No file permission or identity check in the production path was weakened.

Lint caught the API-30 floor of public `Os.fcntlInt` during this correction. Capture and the reader now enforce that floor directly at runtime; matching tests are SDK-filtered on API 24. An attempted annotation import was unavailable in this dependency set and was removed in favor of those runtime guards, with no added dependency or lint baseline. Initial test compilation also found incorrect fixture access to the tool plan and a nullable Parcelable generic; both were corrected. Final builds and lint pass. Existing resource limits and expected successful behaviors were not relaxed.

P9.2 real online model acceptance remains pending. The three recorded Model8 synthetic-image attempts are still failures with empty text; this turn makes no Model8 or PoloAPI call. A future suitable target needs image **input** and text/tool **output**. Neither image generation nor video support is required. These tests do not claim a real host plus online model visual task, nor do they replace the outstanding original P9.1 Wi-Fi comparison.

No host or Provider source changes, Rhino changes, physical-device installs, Wi-Fi changes, orders, payments, pushes, tags or publications are part of this item. No SIM or additional manual action is needed for this implementation. Original P9.3 dynamic scripts can continue while the real visual model remains unavailable. QV710AF65F / XQ-AT72 / Android 12 remains absent and untested, with the user's expected availability before 2026-09-27 20:00 UTC+8 retained.

# P9.2 Provider image-input implementation receipt

Date: 2026-09-26. The original P9.2 model item is now accepted for the
explicitly selected AiGoCode / `gpt-5.6-sol` target: both real initial-image
and native tool-result-image probes passed on Provider `7138fd0`, 3-Stone AI
1.2.0 / build 218. The final section records the measurements and limits.

The implementation and earlier failed attempts below are preserved as
historical evidence from Provider `d0ad293` / build 216 and host `52ce694f92`
/ build 5297. No original roadmap item is added, split or removed.

## What changed

- Provider V2 negotiates the maintainer-approved 2.1 extension. Initial
  JPEG/PNG images and images returned by native tools share the existing
  online session. All catalog pages preserve the 2.0 text-only view.
- Online profile schema 4 has an explicit per-model image-input selection.
  Old schema 2/3 profiles load with this selection disabled. Cloning and
  import/export preserve explicit selections; no protocol or model-name
  heuristic advertises vision. LiteRT and persistent `ai.session` remain
  text-only.
- OpenAI-compatible, Anthropic Messages and Gemini GenerateContent requests
  carry the images in their respective formats. For OpenAI, every text tool
  result precedes a labeled user image observation. Anthropic uses image
  blocks in `tool_result`; Gemini uses sibling user `inlineData` after all
  `functionResponse` parts. Gemini's mapping is covered by deterministic
  protocol tests, not real Gemini backend acceptance.
- Images retain the host SDK limits: 4 MiB each, 4 images / 8 MiB per batch,
  each edge at most 4096, and 16 images / 32 MiB across the initial request
  and continuations. Text limits are independent. Target, IDs and declared
  quotas are checked before FD reads; bytes, hash, MIME, dimensions and
  decodability are checked before model execution.
- Private nonblocking reads release workers after cancellation or timeout
  and preserve regular-file offsets. Ownership transfer now preserves
  reliable-pipe producer error status. New tests also check actual worker
  termination; existing exact-text output assertions remain unchanged.

The Provider's detailed implementation, ownership explanation, references and
failed intermediate runs are in its
`docs/dev/p92-online-vision-evidence-2026-09-26.md` at `d0ad293`. Bundled release
SDK hashes and source/license records were updated together from host
`52ce694f92`; neither repository reads sibling outputs during builds.

## Verification and limits

| Check | Result |
| --- | --- |
| Provider full JVM suite | 379/379, no errors/failures/skips |
| API 24 / x86_64 | 22/22 Android tests, 4.425 s |
| API 37.1 / x86_64 / 16 KiB | 22/22 Android tests, 4.396 s |
| Debug, androidTest, R8 release and signed archive | Passed |
| Provider lint | 0 errors / 97 warnings |
| Native ELF/ZIP alignment | 16,384-byte checks passed |
| Provider ten-language generation and repository checks | Passed |
| Archived R8 release install / launcher | API 24, API 37.1 and XQ-DQ72 passed |
| New picker on API 24 R8 release | Disabled by default, selection updates summary, parent cancellation saves no profile |

The Android suite contains 9 new vision cases, 11 native-tool cases and 2
discovery/metadata cases. It uses Android codecs, AIDL proxy marshalling,
real file descriptors, a deterministic backend and a test-only UID verifier.
Production signature/UID checks are unchanged. It is not a cross-UID real
host-to-cloud test or a release Binder acceptance result. No new local Gemma
inference was run. The Agent runtime, its AARs and installed APK are unchanged
by this documentation-only receipt. Its ten-language 36 generated artifacts
pass the read-only generator check, and `ApplicationTextPunctuationTest`
passes for the updated packaged documentation and roadmap. Provider lint was
also rerun after the final Android-probe metrics change and passed.

The final installed x86_64 APK hash was read back on API 37.1, and the arm64
hash on XQ-DQ72; both match the archive below. API 24 installation and launch
succeeded, but an additional hash attempt found no `sha256sum` command and
the subsequent ADB pull did not retrieve the package path. No installed-byte
hash equivalence is claimed for API 24. The two private AVDs were then shut
down with their data retained.

## Earlier Model8 attempts and the original pending gate

The original receipt recorded three deliberately enabled synthetic-image
probes on XQ-DQ72 / API 35
against the configured Model8 / `claude-fable-5-1` target. They used the real
credential/network/HTTP layer and Provider session, with an in-memory image
opt-in for that exact target. Saved settings were not changed. The image is
a generated 512 x 192 JPEG containing six random digits; no device screenshot
or personal data was uploaded, and no real device tool was executed.

| Attempt | Result |
| --- | --- |
| Initial image 1 | Failed, empty text, full test time 4.285 s |
| Native image-tool request 1 | Failed, empty text, full test time 2.399 s; tool count was not recorded before the assertion, so image continuation is not established |
| Initial image 2 | Failed, empty text; session time 5277 ms, 8395 input tokens / 0 output tokens, 0 output characters, output limit 1024 tokens |

These failures neither establish supported image input nor isolate the cause
of the empty output. They remain separate from passing deterministic tests
and from the earlier P9.1 text/tool task failures.

The user subsequently reported that no suitable online image/video model is
currently available. Further online vision calls were stopped, and PoloAPI
was not called. P9.2 needs an image-input model returning text, not a model
that generates pictures or video. This distinction was explained in the
conversation; the configured services' actual image-input capabilities are
still unverified. Future acceptance requires successful initial-image and
tool-result-image probes against an explicitly selected supported target.

## Initial candidate and historical remaining work

| ABI | CRC32 | SHA-256 |
| --- | --- | --- |
| universal | `3701DBAC` | `a207695776d11d589c81dab24bf3176de929500e064ef8cf539f074b8ca2e35b` |
| arm64-v8a | `39FF33F2` | `d537a7ff8681321d0b769536939cab55223b34028461bb05e604dbbd36a6597c` |
| x86_64 | `A9558EE6` | `49758e23e65ed7f4d3ab276365824f0fab8f428078a7e063c6835dbf83980b0c` |

The Provider archive is a local signed candidate built before its logical
commit; the private manifest correctly records parent `4e887e8` with
`sourceDirty=true`. No push, release or tag changed. XQ-DQ72 was left on the
arm64 R8 candidate / build 216, with application data retained and without
profile, credential, network or SIM changes. The other physical devices,
host sources and concurrent Rhino work were untouched. No order or payment
was made.

At the time of the initial receipt, the next item was the original P9.2
Agent `screen_capture` item: host-owned capture,
longest edge 1280, JPEG quality 70, visual prompting and image-token budgets.
Its implementation and deterministic validation can continue while a real
image-input target is unavailable. The P9.1 Wi-Fi two-path comparison remains
pending independently. Neither the present work nor the next implementation
requires Redmi to keep a SIM card; a later online Wi-Fi toggle test needs
independent connectivity only for that test.

QV710AF65F / XQ-AT72 / Android 12 remains absent, with the user's expected
availability before 2026-09-27 20:00 UTC+8 retained. No additional architecture
decision or manual device operation is required to continue implementation.

## Accepted AiGoCode follow-up

The maintainer explicitly authorized the newly configured AiGoCode /
`gpt-5.6-sol` target for text comparison and an image-input trial. The public
catalog confirmed that exact model before the probes. This device now
reports serial QV770340J7, model XQ-DQ72, Android 13 / API 33. Earlier receipts
labeled this device API 35; that historical value has not been reverified
and is not evidence of an OS change. This follow-up uses the current API 33
reading.

Both probes used the matching officially signed Debug and androidTest
builds of Provider `7138fd085660edfa0b8b7e918b5eb06c33982ffe` / 1.2.0 / 218.
Each generated a 512 x 192 JPEG at quality 70 containing six random digits;
the prompt did not contain the answer. The exact configured target was
vision-enabled only in the test's in-memory profile. Saved profile data,
credentials, default target and persistent image-input selection were not
changed by the probes. No device screenshot or personal image was uploaded.

| Probe | Result | Session time | Input / output tokens | Tool calls | Output |
| --- | --- | --- | --- | --- | --- |
| Initial image | Passed, instrumentation OK (1 test) | 17263 ms | 138 / 6 | 0 | Six characters, exact image match |
| Native tool-result image | Passed, instrumentation OK (1 test) | 26480 ms | 308 / 39 | 1 | Six characters, exact image match |

Both runs kept the 1024-token maximum and the existing bounded deadline.
The second probe first requested `observe_image`, submitted the image as
that call's result, then received the matching final answer. Its reported
usage covers the native conversation; 39 output tokens are not claimed to
be the length of the six-character final text. These are bounded real HTTP
probes, not repeated samples selected for success.

The probe uses the real credential repository, network policy, HTTP backend
and Provider session, with a test-only same-UID owner verifier. It closes
the existing P9.2 Provider model gate requiring both image paths. It is not
a new real host-to-Agent cross-UID visual task, does not establish every
protocol or every model's support, and does not turn on image input for
future user tasks. The Agent integration and cross-UID deterministic tests
were completed separately in the existing Agent implementation receipt.
The original Model8 empty-response failures remain failures.

The initial attempt to run the test against the previously installed R8
build 216 crashed before a model probe. It is retained as an instrumentation
setup failure, not counted as model acceptance or folded into the two
successful 218 probe results.

### Matched build and archive

Debug, androidTest, R8 release and signed archive tasks completed successfully
in 3m 3s, with 126 actionable tasks and 16 KiB native alignment verified for
Debug and Release. The archive records the exact clean source above with
`sourceDirty=false`. The tests and all app APKs use the official certificate
SHA-256 `31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`.

| Artifact | SHA-256 |
| --- | --- |
| Debug arm64 / 218 | `d44607b11f51eaf760397d03ed48fbf5be460f424bd7ac9d657263ad31e19cc7` |
| Matching androidTest | `0261fcd30f98f562267e4272a5892f6f00b263f35954669949801301475fb927` |
| R8 arm64 / 218, CRC32 A931BB09 | `cad1df45c6bcd79fc3de635a6eadada305689c809318484064e99a8dac071bc5` |

An initial Windows command launch error, native filesystem-enumeration stall
and resulting cache-lock failure are preserved in ignored build records.
The successful build used a separate Gradle home and project cache with
file watching and Gradle native services disabled; the native-service
property was verified from the installed Gradle bytecode. No shared locks
were deleted or unrelated build processes terminated. These setup failures
are separate from the successful APK build and model probes.

This Agent change updates documentation and the next commit count to 90,
without a runtime/API change or a newly built Agent APK. The read-only
Markdown generator check passed for all 10 languages and 36 artifacts, and
Git whitespace checks passed. Ten-language current
status text distinguishes this selected-model acceptance from the remaining
P9.1 Wi-Fi comparison. Subsequent Provider error-classification changes and
Wi-Fi tests are recorded separately and do not change the build identity of
these vision results. No new test device, SIM, saved vision toggle or
architecture decision is needed for this completed model gate.

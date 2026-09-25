# P9.2 Provider image-input implementation receipt

Date: 2026-09-26. Provider source: `d0ad293`, 3-Stone AI 1.2.0 development
candidate / build 216. Host source remains `52ce694f92` / build 5297. The
original P9.2 model item is implemented at the Provider layer, but remains
unchecked until a supported online model passes real image-input acceptance.
No original roadmap item is added, split or removed.

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

## Online vision remains unaccepted

Three deliberately enabled synthetic-image probes ran on XQ-DQ72 / API 35
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

## Candidate and remaining work

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

Next is the original P9.2 Agent `screen_capture` item: host-owned capture,
longest edge 1280, JPEG quality 70, visual prompting and image-token budgets.
Its implementation and deterministic validation can continue while a real
image-input target is unavailable. The P9.1 Wi-Fi two-path comparison remains
pending independently. Neither the present work nor the next implementation
requires Redmi to keep a SIM card; a later online Wi-Fi toggle test needs
independent connectivity only for that test.

QV710AF65F / XQ-AT72 / Android 12 remains absent, with the user's expected
availability before 2026-09-27 20:00 UTC+8 retained. No additional architecture
decision or manual device operation is required to continue implementation.

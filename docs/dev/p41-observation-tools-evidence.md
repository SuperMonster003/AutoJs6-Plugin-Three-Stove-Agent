# P4.1 observation tools evidence

Date: 2026-09-23. Final plugin: 1.0.0 / 36. Minimum host: 6.8.0 / 5288.
The three original P4.1 items are complete. No roadmap item was added, split or
discarded. P4.2 actions and P4.3 verification rules retain their original scope.

## Node observations and references

- CompactNodeText parses the host compact format with sequential node references,
  bounded depth, recognized flags, escaped labels and required locations. It
  rejects malformed counts, duplicates, bounds and oversized input. A host label
  clipped inside an escape remains display text and cannot identify a live node.
- Each run owns an eight-snapshot NodeRefRegistry. Host snapshot IDs remain intact;
  package/activity changes invalidate old entries, and capacity eviction is FIFO.
  Display fingerprints include window metadata, class, resource ID, text and
  description. Reordering does not change that fingerprint. Relocation requires
  a unique candidate and constrained bounds; password placeholders and clipped
  labels are ineligible. The host must still validate actual node identity before
  an action. This registry does not authorize actions or bypass stale references.
- ui_dump records the snapshot before compiling a bounded model observation.
  Its change summary compares text as a multiset, handles repeated labels, and
  notices state/position changes even when text is unchanged. Each side retains
  at most 16 labels of 80 bytes. A first snapshot is a baseline; partial marks
  incomplete source or summary data. Missing text in a bounded sample is not
  proof that the full screen no longer contains it.
- ObservationTools normalizes successful broker replies before journaling and
  model use. Dump output preserves whole compact rows within 20 KiB. ui_find
  defaults to 10 nodes with explicit total/truncated, bounded string fields and
  complete node entries. ui_wait_for retains matched/state/node and the existing
  bounded polling path, including appear/disappear, deadline and cancellation.
- app_current and device_info remain bounded JSON. screen_state returns screenOn
  only, not an inferred lock state. console_tail splits messages before choosing
  the newest requested lines, masks credential patterns, bounds each line and
  the total content, and identifies the host's process-wide global-window capture.

## Optional OCR

The shared V1 broker info now includes optional availableOptionalMethods metadata.
Missing metadata means no optional method advertised. For accessibility.readScreenText,
the host checks installed, authorized, enabled, compatible OCR candidates and the
current method grant plus accessibility/screen_capture/ocr permissions. Discovery
does not bind an OCR service or capture a screen; a destroyed broker reports none.

HostLink intersects this metadata with the link's narrowed methods and permissions
at each task start. Its resulting policy drives the model prompt, decision schema,
client cache and runner admission before the first decision. A user-disabled OCR
group or unavailable-tool restriction remains effective. Removal during a run can
still produce an ordinary tool failure; discovery is not a promise of recognition.

OcrScreenObservation accepts bounded text-only host results and emits at most 400
whole lines in 20 KiB. Nearby words on the same line are joined with union bounds
and conservative confidence. Distant columns and overlapping boxes are not joined.
Unknown bounds/confidence remain null. Multiline source blocks keep their original
shared box and boundsScope:source-block, without inventing per-line coordinates.
Credential patterns and oversized lines are redacted/clipped, with truncation
reported. Coordinates remain in host screen space; no image crosses into the plugin.

## Verification

| Check | Result |
| --- | --- |
| Plugin JVM | 311/311, including 23 new compact/registry/observation/OCR/policy cases |
| Host JVM | 3182 discovered, 3176 passed, 6 existing conditional skips |
| Shared capability API JVM | 4/4, including optional-key contract coverage |
| Plugin Android, each API 24 / API 37 | 27/27, including four new wait polling cases |
| Host capability broker Android, each AVD | 5/5, including availability, narrowed grants and destruction |
| Real host/plugin round trip, each AVD | 15 passed, 1 existing optional Wi-Fi skip; runner reports 16 tests |
| Plugin build | debug, androidTest, release/R8 and lint; 0 errors, 6 existing lint warnings |
| Host build | app debug, androidTest and three release API modules together |
| Documentation | 10 languages / 36 generated artifacts, freshness check passed |

The Android runs used plugin build 35 with host build 5288. Final build 36 adds
only the already tested polling test, roadmap/evidence and build metadata; its JVM,
assembly and lint checks also pass. The small later documentation/comment edits
do not change the tested production behavior.

Both AVDs were private read-only instances: API 24 x86 with 4 KiB pages and API 37
x86_64 with 16 KiB pages. No physical device was operated. The real accessibility
scenario reads the system Settings page through the installed host/plugin pair:
current app, two dumps with changes, bounded find, appear/disappear waits, screen
state, device info, console tail and model done (10 scripted model calls). It
does not toggle a setting and restores the previous enabled accessibility services.

Separate host-UID controlled brokers verify OCR omission from the real prompt
when absent, and actual plugin admission/line merging when present. No real OCR
recognizer, model provider or MediaProjection permission dialog is involved.
ObservationPollingAndroidTest exercises the real serial scheduler and Binder
adapter: delayed appearance, disappearance, a finite deadline, and cancellation
without subsequent polling or completion. These are correctness tests, not P7
performance or cold-start acceptance.

Useful commands (Temurin JDK 21 was selected for Gradle):

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:lintDebug
py .python/generate_markdown.py --check
adb -s <private-avd> shell am instrument -w -r io.github.supermonster003.autojs6.plugin.ai.agent.test/androidx.test.runner.AndroidJUnitRunner
adb -s <private-avd> shell am instrument -w -r -e class org.autojs.autojs.core.plugin.hostbroker.HostCapabilityBrokerStubTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
adb -s <private-avd> shell am instrument -w -r -e autojs.agent.plugin true -e autojs.agent.observe true -e class org.autojs.autojs.core.plugin.agent.AiAgentPluginRoundTripTest org.autojs.autojs6.test/androidx.test.runner.AndroidJUnitRunner
```

Use explicit private AVD serials; the last scenario temporarily enables host
accessibility and opens Settings. Raw local output is in ignored build/p41-*.log
files in both repositories. The P7 test-apps:ai-agent-conformance module does not
yet exist; these checks use the current real installed-plugin conformance suite.

## Commits and remaining acceptance

- Plugin 6efc062: compact observations, registry and bounded change summaries.
- Plugin c338321: optional OCR policy, line normalization and API dependency update.
- This test/evidence commit: polling regression cases and original P4.1 completion.
- Host 0a472f7fee: optional OCR discovery, broker/real-plugin tests and host evidence.

All three release API AARs were assembled together from committed host 0a472f7fee,
copied and rehashed. The capability API hash changes for the new optional key;
common-plugin-api and ai-agent-api remain byte-identical. No AIDL transaction,
contract version or public JS API changed. Manifest, constants, instructions and
tests consistently require host 5288.

This is E0/E1 evidence. Real OCR recognition, screen-capture permission handling,
physical devices and online-model E4 remain unverified. The next implementation
section is P4.2; P3/P4 E4 and the P7/P8 gates remain open. No remote push, release
or package publication was performed. Pre-existing Ace releases/ and the unrelated
TypeScript declaration publishConfig change were preserved.

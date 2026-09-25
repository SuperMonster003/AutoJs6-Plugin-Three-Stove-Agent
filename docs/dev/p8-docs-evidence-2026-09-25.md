# P8 documentation preparation - 2026-09-25

## README and images

- Completed the original first P8 item in ten languages. The README now contains
  interface and JavaScript quick starts, standalone and project registrations,
  the generated ToolCatalog table, presets/memory, compatibility and three FAQs.
  The plugin-center instructions share the compatibility and Provider guidance.
- Distinguished attachment minimum 5289 from complete task API minimum 5293.
  The production host broker currently selects 3-Stone AI; other Providers need
  host integration. The connection label is not the model selector.
- Kept development-preview status and the recorded Gemma 4 E2B IT decision
  validation failure. Text/OCR observations are the current implementation;
  vision, native tools and generated scripts remain in the original P9.
- Four actual-view images and reproducible capture instructions are in
  `docs/images/`. A new empty API 37.1 / 16 KiB AVD was used. No existing device
  data, model credentials or real model/device operations were used.

## Validation and corrections

- Ten languages / 36 generated artifacts pass the Markdown check.
- Punctuation and ToolCatalog JVM tests: 12/12.
- Debug and androidTest builds pass under Temurin JDK 21.
- Opt-in capture: 2/2, 8.623 s. Existing workbench interaction and floating
  presentation regression: 2/2, 8.294 s.
- The first instrumentation command used slashes in class names and loaded no
  tests. Corrected names ran successfully. The first image review found that
  capturing the content root omitted the window background; final captures draw
  the real decor instead. No production theme or security flags were changed.
- No runtime/public API/AIDL/dependency change. This documentation step does not
  substitute for the final release/R8/CRC and two-device smoke item.

## Host lint follow-up

The parallel host commit `433472897a` records strict appDebug lint success in an
isolated worktree. Checked its final successful log and XML: 0 Error/Fatal,
2403 Warning, 3 Hint. XML SHA-256 is
`cc6f325819697ad186599326924943eeae0ecceb1b60ca1ef6debc0c1ea2af34`.
The recorded source snapshot matches the current source, except for the audit's
own README. Legacy compatibility paths have scoped suppressions, described in
the host's `tools/lint_audit/2026-09-25.md`. This closes the prior outstanding
strict host lint evidence; it is not a claim that every warning was fixed or
that all host variants/device behavior were retested here.

## 1.0.0 changelog

The second original P8 item consolidates 30 incremental feature entries into 11
user-facing capabilities in all ten languages. It retains the observed fixes,
performance/compatibility notes and dependency sources. The preview hint and
Android/host/3-Stone AI requirements are explicit. The existing 2026/09/25 date
is unchanged; this is release-copy preparation, not a published release.
The Markdown generator/check passes for ten languages and 36 artifacts, and the
packaged punctuation test passes. The dedicated README emulator was closed.

## Host documentation and index preparation

Host commit `fc1a9423d8` marks the two protocols versioned V1, reconciles the
compatibility floors and current API/entry behavior, and adds the exact Agent
package to the optional Tools installation catalog. Existing P1/P5 changelog
coverage was checked in all ten languages; the existing entry-management record
now includes conditional installer availability. The existing renderer updated
22 Markdown artifacts without refreshing dates or online metadata.

Temurin host appDebug assembly and four wizard JVM suites pass: 18/18 in a
combined 2m 44s build. Runtime version remains 5296; no new host APK was installed.
The original host/index item stays unchecked because the separate official index
requires published APK releases. Its inventory and generated download entry will
be updated after final publication, without fabricated URLs or changing admission.

The next original item covers Documentation, TypeScript declarations, Offline
Docs and Ace versions/publication. Read-only inspection found Documentation and
Offline Docs clean; the existing TypeScript package.json change and Ace releases/
files were preserved. Public API signatures did not change in this turn.

No physical device, network proxy, SIM configuration, real model or shopping
flow was changed. XQ-AT72 / QV710AF65F remains an explicit offline follow-up.
The capture emulator is closed. Removal of its private ignored avd-home directory
was rejected by automatic execution review with only `blocked by policy`; the
images remain locally in build/p8-docs-private/avd-home and were not committed.
This cleanup failure does not invalidate the capture or source verification.

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

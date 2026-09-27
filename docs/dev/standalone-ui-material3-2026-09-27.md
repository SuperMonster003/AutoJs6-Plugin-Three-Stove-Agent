# Standalone app Material 3 redesign and shared model switcher (2026-09-27)

Maintainer request: the previous standalone redesign (build 93, [record](standalone-ui-redesign-2026-09-26.md)) still felt fragmented. Redesign every standalone screen for consistency, completeness and detail. Make switching online models fast without presets. Put a top-right overflow menu with Settings on the home screen. Give Settings the items inherited from AutoJs6 (language, dark mode, theme color, check for updates, automatic update checks, ignored updates, version history, about app and developer), modeled on AutoJs6-Plugin-Three-Stone-AI.

The maintainer chose four options on 2026-09-27:

1. Align the toolkit with Three-Stone AI.
2. Make the home screen a task feed with a docked composer and the model capsule in the top bar.
3. Keep models independent of presets.
4. Redesign all pages in phased commits.

The earlier constraint still holds: choosing Full access shows only an inline warning, never a dialog. The choices are recorded as roadmap decisions D45 (toolkit) and D46 (models). No roadmap phase was added, split or dropped. There were no public AIDL, permission, host or reference-plugin changes.

## Commits

| Build | Commit | Scope |
| --- | --- | --- |
| 95 | df48060 | AppCompat 1.7.1 + Material Components 1.13.0, Material 3 themes, `HostAppearanceActivity` on AppCompat, `ui/kit` |
| 96 | 0c4f7e9 | Immediate settings (no Save button), About screen, document viewer |
| 97 | 89cb6ca | Shared model switcher independent of presets, private model record in history |
| 98 | 9099d56 | Readable confirmations with parameter tables, Material floating panel |
| 99 | cc244af | Task feed workbench with docked composer, connection banner, keyed timeline |
| 100 | 0aca0d9 | Searchable history, run detail timeline, Activity Result document saves |
| 101 | db9b9c9 | Presets, memory, MCP servers and script directories on the kit; legacy helpers removed |
| 102 | this record | Final layout fixes, screenshots, roadmap and AGENTS updates, verification |

## Design

- **Toolkit (D45).**
  - Every screen is built in Kotlin through `ui/kit`, with no XML layouts or Compose. The kit provides tokens, the `AgentPalette` / `AgentColorPolicy` colors, scaffold and insets, rows, buttons, cards, dialogs, bottom sheets, chips and feedback.
  - The accent keeps at least 4.5:1 contrast against the background, the surface and its own tonal fill.
  - Host or custom theme colors tint controls at runtime.
  - `localeFilters` keeps only the app's 10 languages from library resources.
  - Kit builders take a `Kit(context, palette)`, so the `:agent` overlay and `PendingCard` use the same styling.
- **Home.**
  - The top bar has the title, the model capsule, History and an overflow menu (New task, Presets, Memory, Script directories, MCP servers, Settings).
  - One connection banner covers every non-attached host state and hides once attached.
  - The feed shows a welcome state (example goals and recent tasks) or the current task. The current task shows the goal, state, model and preset, the full access badge, progress, limits, keyed steps, the inline confirmation card, the accessibility fallback and the result.
  - Rendering is incremental, so the 500 ms poll never rebuilds the feed. Scrolling is sticky and follows only a task; a "Jump to latest" chip appears when new task content arrives out of view.
  - The composer is docked above the keyboard (`adjustResize` plus edge-to-edge insets). It has two rows: an options row with the preset chip and the full access badge, then an input row with the goal field, voice and a round Start button.
  - Run again and Retry with another model only fill the composer; they never start a task.
- **Models (D46).**
  - The workbench and the floating ball share `filesDir/model-selection.json`: the current choice, 8 recent and 16 pinned models. The format is closed; anything unreadable fails closed to Automatic.
  - Automatic means the first on-device target, otherwise the first target. The broker and the sheet preview share this rule.
  - The bottom sheet offers search, Automatic with its preview, pinned and recent models, locality groups, tool-calling and image-input badges, and an Unavailable row for a model that has disappeared.
  - Plugin UI tasks (decided by the private endpoint) never inherit a preset's legacy `targetId`. Host and script requests keep inheritance.
  - Private history records the requested target and the resolved model. The host projection strips both, plus the full access flag.
- **Settings.**
  - Sections, in order: Appearance, Tasks, Tools and data, Quick access, Data management, Updates, Information.
  - Every change is saved at once through serialized whole-object saves, with optimistic rendering and rollback.
  - Full access shows only a red inline note.
  - Tool groups and limits open bottom sheets; durations are in minutes.
  - About shows the version, developer, source, license and third-party notices. Version history and notices open in a scaffolded document viewer.
- **Confirmations and floating ball.**
  - Confirmation cards show a risk badge, the localized tool group and tool, the description and a parameter table instead of raw JSON. The actions are Allow once (filled), Always allow for this session (tonal) and Deny (outlined danger).
  - `ConfirmationActivity` keeps `FLAG_SECURE`.
  - The floating ball uses a Material 3 themed context and inline panels only (no dialogs or popups in the overlay). Its model row opens the shared switcher.
- **History and detail.**
  - History has search, status chips, a preset chip and a Material date range picker, and a destructive "clear finished" action.
  - The detail screen shows the model, duration and limits, a shared step timeline with parameter tables and expandable observations, and a menu to export diagnostics, delete the record or use the task's model.
- **Management screens.**
  - Presets: cards with a row menu and a full-page editor.
  - Memory: search, scope chips and the import review.
  - MCP servers: a risk choice, the write-only token field and a tool checklist.
  - Script directories: validation errors under the field.
  - Every editor asks before discarding unsaved changes.
  - `AgentUi`, `HistoryViews`, `AgentDialogs`, the spinner layout and the collapsible-section state were removed.

## Dependencies and package size

AppCompat 1.7.1 and Material Components for Android 1.13.0 (Apache-2.0) are added with their AndroidX runtime dependencies. Coordinates, SHA-256 hashes and the transitive list are in `THIRD_PARTY_NOTICES.md`, and the v1.2.0 changelog has a `dependency` entry. The app still has no native libraries and passes the native-page-alignment guard.

| Release APK (unsigned name, R8) | Bytes |
| --- | --- |
| build 93, previous redesign (signed release) | 826488 |
| build 94, before the dependency | 846324 |
| build 95, AppCompat + Material, `localeFilters` | 1952497 |
| build 96 | 2119353 |
| build 97 | 2130994 |
| build 100 | 2323016 |
| build 101 | 2327192 |
| build 102, final | 2327936 |

## Tests added or rewritten

- JVM:
  - `AgentColorPolicyTest` and `KitContrastTest`, for WCAG contrast of accents, tonal fills and custom seeds in light and dark;
  - `SettingsDraft`;
  - `ModelSelectionCodecTest`;
  - `AutomaticTargetTest`;
  - preset admission (UI tasks ignore preset models, scripts keep them);
  - history codec and host projection;
  - `ArgumentRows`;
  - `RunHistoryFilter` query.
- Instrumentation:
  - Settings, About, the model sheet, workbench feed, history and detail, presets, memory, MCP, script roots, confirmation and floating tests were rewritten for the new widgets. They keep the existing ids and tags where possible.
  - `UiAccessibilityAudit` checks labels, 48 dp targets, parent bounds and clipped text. Only labels with an explicit truncatable role and a full content description may ellipsize.

The new checks found and fixed these problems:

- dark default tonal text at 3.97:1;
- a 2x-font overflow of the confirmation countdown and the memory review bar;
- a preset chip below 48 dp with extreme text;
- a "Recent tasks" header shown with empty history;
- the "Jump to latest" chip appearing over the welcome state;
- an empty toolbar menu measured as a zero-width RTL control;
- a sheet handle not cleared synchronously on dismiss.

No layout threshold, identity check or confirmation policy was relaxed.

## Verification

The final source (build 102) passed `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease` in one Gradle run under the Temurin 21 emulation, with a single platform version banner:

- JVM: 617 tests, 0 failures, 0 errors, 1 existing performance opt-in skip.
- Lint: 0 errors and 8 warnings. The warnings are dependency and plugin versions, two duplicate launcher icon configurations, the `AgentRuntime` static context and the unused round launcher icon.
- R8 release: no missing classes, no native libraries.
- `py .python/generate_markdown.py --check` passed (10 languages, 36 artifacts). The string check found the same 353 keys in all 11 `values*` folders, with no unused keys. `git diff --check` was clean.

The signed digest archive (`appendDigestToReleasedFiles`) was not produced, because nothing is released.

Device checks used only the disposable `AI_Agent_Conformance_UI_20260926` (API 37.1, x86_64, 16 KiB) and `AI_Agent_Conformance_UI24_20260926` (API 24, x86) emulators. They were set up with the guarded fake host (`run_conformance.py --prepare-only`) and driven by direct `am instrument` per verified serial. The five attached phones were never targeted.

| Device | Full suite on the final source | Duration |
| --- | --- | --- |
| API 37.1, AI_Agent_Conformance_UI_20260926 | 128 tests: 124 passed, 0 failed, 4 skipped | 463.15 s |
| API 24, AI_Agent_Conformance_UI24_20260926 | 122 tests: 118 passed, 0 failed, 4 skipped | 334.537 s |

Each suite skips four explicit opt-in fixtures: three capture methods and one real-MCP interoperability fixture that needs an external server. Earlier full runs passed in Phase 5, Phase 7 and before the final layout fixes. The API 24 emulator crashed once when Gradle ran at the same time; it was restarted, and Gradle is never run during a device suite.

Large text: the five UI audit methods ran on API 37.1 at font scale 2.0, including the Arabic dark layout. They covered settings, dialogs, sheets and documents, management screens, confirmation questions and floating states: 32 named states, 0 issues. The font scale was restored to 1.0 afterwards.

IME check: the goal field was focused on both emulators. The docked composer and the Start button stayed above the keyboard, and `dumpsys input_method` reported the keyboard shown.

Screenshots: all 12 images in `docs/images/` were recaptured from production views with synthetic fixtures. Provenance and hashes are in [the capture record](../images/README.md). The model images now contain only the bottom sheet (1080 x 1278). A second capture after the last code change, on freshly cleared plugin data, matched 11 images byte for byte. `detail.png` differed only in the task time and elapsed values, so the committed images were kept.

## Boundaries

- Only disposable emulators and synthetic model replies were used. There were no real model calls, credentials, payments or physical-device installs.
- The keyboard and 2x-font behavior was checked on emulators only, not on vendor ROMs.
- 1.2.0 remains an unreleased development candidate. Nothing was pushed, tagged or published.

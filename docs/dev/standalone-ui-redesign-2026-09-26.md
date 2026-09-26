# Standalone UI redesign, 2026-09-26

## Scope and decisions

This is the user's explicit request to redesign the standalone app after the original roadmap completed. It does not introduce a new roadmap stage or change previous real-model acceptance results.

- A shared native Android design layer supplies the toolbar, cards, typography, fields, touch targets, colors, dialogs and disclosure controls. History, task detail, presets, memory, script roots, MCP settings, documents and confirmation controls use the same presentation rules. No runtime dependency was added.
- The workbench presents model selection, a goal and the start action first. Voice input shares the action row. Presets move into optional task settings; recent tasks and the current task stay on the workbench. New tasks and new questions scroll into view.
- The model picker uses the existing host model catalog, searches model/provider names, groups locality and displays declared tool/image capabilities. The selected target is saved for future tasks and survives recreation. Removing a selected model blocks admission until the user chooses again. Selecting automatic restores preset inheritance. Sharing text preserves the quick model; explicit preset shortcuts and historical reruns inherit their own preset.
- The public request contract already supports an options.target override. The change uses that field without mutating presets, running jobs, grants, tool groups, confirmation or budgets. Provider credentials and model transport remain in AutoJs6.
- The overflow menu opens settings, presets, memory and script roots. Settings cover language, dark mode, theme color, task configuration, data, updates, version history and app/developer information. Appearance defaults to the host, supports explicit overrides, and safely falls back to Android. Language/night mode can also follow Android directly. App preferences never alter host preferences.
- Appearance is a bounded atomic private snapshot. A file lock coordinates UI and agent-process readers on older Android versions. Section expansion and unfinished settings drafts survive recreation; back navigation confirms abandoning a changed settings draft. Invalid budgets identify the corresponding field.
- Automatic updates default off. Enabling them allows foreground checks no more than once per 12 hours, including failed attempts. Ignored versions and failures remain silent. Manual checks retain the existing 24-hour successful cache and can explicitly show an ignored release. Ignored versions can be restored individually. No APK is downloaded automatically.
- Ten languages plus the explicit English resource directory, README sources, generated instructions and current changelogs are synchronized. Reference: the 3-Stone AI plugin's existing appearance and update settings.

## Verification and boundaries

All device checks use disposable AI_Agent_Conformance_UI* emulators and the guarded fake-host setup. Synthetic model replies exercise actual activities, private Binder calls, persistence, request admission, history and confirmation. No real phone, credential, paid model or device action is needed for this UI change.

The first complete API 24 / API 37.1 runs passed 109 / 115 cases, skipped four explicit opt-in fixtures each, and found one outdated width assertion each. That assertion descended into unmeasured children of collapsed sections. The updated check expands the real sections, waits for layout and excludes hidden subtrees. The broader audit also checks toolbar image buttons, labels, 48 dp targets, parent bounds and clipped text.

Earlier local build attempts encountered JDK discovery delays, two emulator boot crashes and an initial Android back-callback lint failure. The emulator was recovered with its isolated configuration. Legacy Android back handling is retained for API 24-32; newer releases use OnBackInvokedDispatcher. No identity or permission check, layout threshold or confirmation policy was weakened.

## Final build and package

- Version 1.2.0 / build 93. No runtime dependency, public AIDL or host repository change.
- Debug, androidTest, full JVM, lint and signed R8 archive passed together on the final production source. JVM: 582 discovered, 581 passed, 0 failed, 0 errors, 1 existing performance opt-in skip.
- Lint: 0 errors, 6 existing warnings (dependency versions, a static context reference and launcher-icon resources). The final release has no native libraries and passes the native-page-alignment guard.
- APK: releases/autojs6-plugin-ai-agent-v1.2.0-45eddc66.apk, 826488 bytes, CRC32 45eddc66, SHA-256 639be87328bc82ac5b70f6251fc1009f754bf3df369ea72f5674d299d302dcf6. APK Signature Scheme v2 verification passed with one signer.
- Reproduce the build with the repository's JDK 21 toolchain and Gradle wrapper: :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug :app:appendDigestToReleasedFiles.

## Device rerun history

A complete intermediate revision passed 116 cases on API 37.1 and 110 on API 24, with four explicit skips per device. The last production edit only adjusted confirmation/floating button presentation. Its first full rerun lost the shared ADB connection during the real confirmation timeout case; the desktop streams ended without final results. Device logs then showed UiAutomation disconnect/DeadObject failures and dependent window assertions. These runs are retained as incomplete failures, not counted as passed. The same emulator processes were reconnected using a separate ADB server, with names and qemu identity checked before further actions.

## Final device results

| Device | Full suite result | Duration |
| --- | --- | --- |
| API 24, x86, AI_Agent_Conformance_UI24_20260926 | 110 passed, 0 failed, 4 skipped | 276.389 s |
| API 37.1, x86_64, 16 KiB, AI_Agent_Conformance_UI_20260926 | 116 passed, 0 failed, 4 skipped | 339.342 s |

These are complete reruns after the confirmation/floating spacing changes and before the final button background-state fix. Each suite skips three opt-in capture methods and one real-MCP interoperability fixture requiring an external server. Model selection, missing-model admission, request target precedence, share/shortcut behavior, overflow navigation, settings drafts, appearance persistence, update opt-in/throttling and ignored-version migration are covered together with the existing Binder, runner, history, memory, MCP and confirmation regressions.

The instrumentation is invoked directly for each guarded serial rather than with a Gradle task that could enumerate attached phones. Only the disposable emulator instances are installed or changed. Local raw receipts are retained under build/ui-redesign/isolated-final-24.log and isolated-final-37.log.

The final build also passed a separate API 37.1 run at font scale 2.0: five UI audit methods, 28 named states with zero reported issues, plus the Arabic dark MCP editor bounds/touch checks. Four explicitly enabled capture cases passed, producing 12 PNGs from the final views. The font scale was restored to 1.0. See [capture provenance and hashes](../images/README.md) for reproduction and synthetic-data boundaries. The corrected confirmation primary button was visually checked in the final capture.

The final signed APK was installed and its launcher started successfully on both API 24 and API 37.1, with versionCode 93 / versionName 1.2.0 verified. These release checks validate installation/startup, not a new full R8 Binder or real-model acceptance run. The release artifact was built from this final working tree before the build-93 source commit; it remains an unpublished local development candidate.



## Final visual correction

The final screenshot review found a primary confirmation action rendered with white text over the inactive surface after repeated styling. The API 37.1 pixel test reproduced 1.13:1 contrast. Explicitly initializing the child state did not resolve it. A state list alone also reproduced the failure. Local Android framework sources explain the ordering: View sets the state, then its tint application mutates LayerDrawable and clones children without copying their state; dispatching the unchanged parent state does not update those new children. The final background is mutated before assignment, so View dispatches state to the final children. Primary actions retain explicit enabled/disabled fill drawables. The regression checks actual rendered background/text contrast while enabled, disabled and enabled again, with repeated styling. Its first harness replaced the workbench content and consequently invalidated the workbench's view lookups; the harness now adds its test view without removing production content. No production null-check bypass was introduced for that fixture error.

After the final background correction, the pixel contrast regression and the five existing UI audit methods passed together on both API 24 and API 37.1: 6/6 each, no skips. These final targeted runs cover management screens, settings/dialogs/documents, confirmation/question states, floating states and the Arabic dark MCP editor. The complete 110/116-case suites above were not rerun after this presentation-only correction; the final targeted receipts are retained separately.

Both named disposable AVDs were closed through their authenticated local consoles after verification. Their local data/logs are retained. No physical-device package or setting was changed.

<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Runs natural-language tasks in AutoJs6 by choosing registered scripts and operating the screen step by step</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### Introduction

******

AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

The plugin is both an AutoJs6 plugin and a standalone app. Scripts reach it through the `ai.agent` API of AutoJs6; users reach it through its own task workbench, the AutoJs6 drawer, a floating ball, the system share sheet, app shortcuts, and voice input. Model calls and device actions always go through AutoJs6 over Binder: the host lends the plugin a model broker (the AI Provider plugins the host already knows, such as 3-Stone AI) and a capability broker with a bounded grant. The plugin never holds credentials, never binds a model provider itself, and never requests the accessibility permission.

******

### Status

******

Development preview: P6 task screens, settings, the floating ball, sharing, shortcuts and voice drafts are available. The ai.agent API requires AutoJs6 build 5293 or later. Reliability and release gates remain in P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### Features

******

Release 1.0.0 is planned to provide the following capabilities:

- Script selection: scripts registered through `project.json` or an `@agent` header comment are listed to the model with their descriptions and parameter schemas; the agent picks one, completes the parameters, asks for confirmation when required, runs it inside AutoJs6 and reads its structured result.
- Step-by-step screen operation: the agent observes the accessibility node tree in a compact text form (and screen text through an OCR plugin when one is installed), then clicks, types, scrolls and presses keys through the AutoJs6 capability broker until it can verify the goal.
- Safety by design: read-only tools run automatically, sensitive actions (payment, sending, deletion, file writes, shell, coordinate gestures, scripts registered as sensitive) require confirmation, and every run has step, model-call, duration and token budgets.
- Script API and user interface: `ai.agent.run(goal, options)` returns an `AgentRun` handle with events, responses and cancellation; the standalone app offers a task workbench with history, presets, preference memory, settings and release history.

### Tool catalog

Development preview: registered scripts, screen actions and the ai.agent task API are connected. Task APIs require AutoJs6 build 5293 or later; the complete workbench follows in P6 and reliability acceptance continues in P7.

| Tool | Group | Risk | Default | Description |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | Open an application by package name or display name. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | Read clipboard text. |
| `clipboard_set` | `act` | `NORMAL` | `on` | Replace clipboard text. |
| `ui_click` | `act` | `NORMAL` | `on` | Click one observed target. |
| `ui_long_click` | `act` | `NORMAL` | `on` | Long-click one observed target. |
| `ui_press_key` | `act` | `NORMAL` | `on` | Use an Android navigation or notification-panel action. |
| `ui_scroll` | `act` | `NORMAL` | `on` | Scroll one observed target a bounded number of times. |
| `ui_set_text` | `act` | `NORMAL` | `on` | Set or append text on one observed editable target. |
| `files_list` | `files` | `NORMAL` | `off` | List workspace files. |
| `files_read` | `files` | `NORMAL` | `off` | Read bounded workspace file text. |
| `files_stat` | `files` | `NORMAL` | `off` | Read workspace file metadata. |
| `files_write` | `files` | `SENSITIVE` | `off` | Write a workspace file after confirmation. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | Tap coordinates only with the gesture group enabled and confirmation. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | Follow a bounded coordinate path after confirmation. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | Swipe between coordinates after confirmation. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | Read available preference memory in the current scope. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | Propose a preference for user-approved storage; never store credentials. |
| `app_current` | `observe` | `READ_ONLY` | `on` | Read the current window and application. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5289 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent, connect to AutoJs6, enter a goal, select the default preset and start. Answer questions or confirm actions in the task card; open recent tasks to review their details.
4. Configure extra folders in the launcher's "Script directories", one absolute path per line. The host validates and applies saved paths; tasks can only narrow the approved folders.
5. Up to 200 tasks / 32 MiB. Older, least recently viewed finished tasks are removed first. Rerun fills the original goal and preset in the workbench. Review them and press Start task to execute again. Clearing history keeps running tasks. The export keeps diagnostic counters, tool names and confirmation outcomes. Goals, parameters, observations and script results are removed. Choose where to save the file.
6. Open Presets in the workbench to save a task configuration. Names are stable script and memory identifiers; copy a preset to use another name. The built-in default can be edited but not deleted. Choose a model from the host catalog, or keep automatic selection. A missing selected model fails without switching targets. Task options can further narrow preset limits. Fixed and task context share an 8 KiB limit. Memory scope can include global and current-preset entries, either one, or neither. Editing or deleting a preset does not change queued tasks. Up to 32 presets / 1 MiB are stored privately.
7. Open Memory to review, edit, delete or back up preferences. Up to 500 entries / 256 KiB; each retains its scope, source task and timestamps. Confirm each memory_propose and each imported entry separately. Unknown preset scopes require that preset to exist first. Automatic injection uses up to 4 KiB of the newest entries in the allowed scope; current-preset values override global values with the same key. memory: false disables automatic injection only; disable the memory tool group or select no memory scope to also block queries and proposals. Export includes actual values and provenance. Do not store credentials; recognized credential keys and token formats are rejected.
8. Answer in the workbench while it is open. In the background, open the high-priority notification to review the specific request. Confirmations show the tool, parameters, risk and time remaining. Allowing similar actions applies only to this tool at this risk level in this task; payments and memory proposals always require individual approval. Remember this answer creates a separate memory_propose for review, within the allowed memory scope. Confirmation normally waits 120 seconds, questions up to 10 minutes, both bounded by the task budget. Timeout returns USER_TIMEOUT; the model may ask again or report partial completion. Old requests cannot answer new ones. Notification permission and channel settings affect background delivery.
9. Open Settings from the workbench to choose tool groups, budgets, cautious mode, voice input and the default preset. Changes apply to new tasks. gesture/files/shell are initially off; OCR requires an available authorized host plugin. Budgets inherit stock defaults when blank and remain within protocol limits. Presets and task options can only narrow them. Data management shows counts and bytes; category clearing requires confirmation and no active task. Clearing presets restores the built-in default. Script folders, licenses and source links are also available.
10. Release history and legal notices are bundled for offline reading. Check updates manually through GitHub Releases, with a 24-hour success cache, cancellation and an ignored-version setting. The dialog opens release history inside the app or the release page in a browser. Checks never run automatically and APKs are not downloaded.
11. Enable the floating ball in Settings, allow display over other apps, then save. It is off by default, appears only while AutoJs6 is connected, hides on lock or disconnect, and has no idle foreground service. Drag to move; tap to enter a goal, choose a preset, review a question or confirmation, or stop a task. Collapsing the card restores background confirmation notifications. Share plain text to AI Agent, use the New task app shortcut, or pin a preset with an optional goal from Presets. All entries open editable drafts and require Start task. A deleted preset never falls back silently. Voice uses the system recognizer in the interface language, is hidden when unavailable and fills text without sending.

> Development preview: P6 task screens, settings, the floating ball, sharing, shortcuts and voice drafts are available. The ai.agent API requires AutoJs6 build 5293 or later. Reliability and release gates remain in P7/P8.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- Binder contract entries require the org.autojs.permission.PLUGIN signature permission. The launcher (also used by shortcuts) and the text/plain ACTION_SEND share target are public; they accept bounded goal/preset drafts only. External intents cannot execute tasks, provide confirmations or change grants. Private settings, voice results and task controls are not exported.
- The plugin holds no API keys, never binds a model provider and does not request the accessibility permission: model calls and device actions go through brokers that AutoJs6 lends for one attached link and revokes on detach, each bounded by a grant (allowed methods, rates, sizes, model quota).
- INTERNET is used only for manual GitHub release checks. FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE support active tasks; POST_NOTIFICATIONS provides progress and confirmations. SYSTEM_ALERT_WINDOW is requested only when the user enables the floating ball in Settings. No accessibility, storage or microphone permission is requested.
- Task history, presets and preference memory stay in the plugin's private storage; backups and device transfers are disabled.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5289 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: Verified host attachment with queued tasks, responses, cancellation, queries and private step history; host loss blocks tasks and process restart never resumes them automatically.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/09/25_

- `Hint` Development preview: P6 task screens, settings, the floating ball, sharing, shortcuts and voice drafts are available. The ai.agent API requires AutoJs6 build 5293 or later. Reliability and release gates remain in P7/P8.
- `Feature` Optional floating task input, progress, stop and confirmation cards; plain-text sharing, static and pinned preset shortcuts, and system speech recognition that fills a draft without sending
- `Feature` Global settings, per-category data management, offline release history and legal notices, and cancellable manual update checks with daily caching and ignored versions
- `Feature` Inline and notification confirmation with risk, countdown, task-scoped approval and separately confirmed answer memory
- `Feature` Preference memory with per-proposal confirmation, scoped queries, conflict protection, per-entry persistence, editing, deletion and JSON backup with individual import approval
- `Feature` Named presets with creation, editing, copying, deletion and default selection; host model catalog labels for locality and structured JSON, fixed context, narrower tool groups and budgets, confirmation policy, approved script folders and memory scope
- `Feature` Full task timelines and results, status/preset/date filters, rerun drafts, deletion, redacted JSON export, and versioned private history with migration and LRU cleanup at 200 tasks / 32 MiB
- `Feature` Task workbench with shared run admission, host appearance, inline interactions, budget progress and up to 20 recent tasks readable while disconnected
- `Feature` Completion requires evidence, partial results list unfinished work, and order or payment tasks must report an observed order state
- `Feature` Task verification tracks unchanged screens across context trimming and blocks the third equivalent action request before execution
- `Feature` Actions wait for a bounded stable screen sample and include changes since the last action in subsequent observations
- `Feature` Screen actions bind confirmation to inspected host nodes, support text append and bounded scrolling, and report action results and window changes
- `Feature` Screen OCR is offered only when the host reports an available authorized OCR plugin; text is merged into bounded lines with coordinates
- `Feature` Screen observations retain host snapshot references, bounded node and console output, and summaries of visible text and state changes
- `Feature` Single-script tasks retain the script ID, path, execution ID and reported result after model completion, including explicit null and marked truncation for large results
- `Feature` Registered script execution with confirmed manifest checks, structured observations, redacted console tails and owned-script stopping on timeout or task cancellation
- `Feature` Scoped preference memory injection for script parameters, with a 4 KiB limit, explicit truncation and task-level opt-out
- `Feature` Registered script parameter validation with defaults, missing-value questions, current-manifest risk checks and complete parameter tables for confirmation
- `Feature` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `Feature` README, plugin-center instructions, and changelog in 10 languages
- `Feature` Agent core catalog with 30 tools, group admission, parameter schemas, bridge-call preparation, bounded observations and sensitive-risk escalation
- `Feature` Agent decision core with protocol-specific schemas, strict and extracted JSON parsing, tool/branch validation, at most two repair retries and English/Chinese prompt templates
- `Feature` Agent task budgets for steps, model calls, elapsed time and tokens, with bounded tool/wait deadlines, usage estimation and output-token admission
- `Feature` Agent confirmation gate with default/cautious policies, task-local grants for the same tool and risk, mandatory per-action payment confirmation, and payment keywords in 10 languages
- `Feature` Private Agent step journal capped at 200 steps and 1 MiB, with password-text redaction and bounded terminal results that retain status and counters
- `Feature` Verified host attachment with queued tasks, responses, cancellation, queries and private step history; host loss blocks tasks and process restart never resumes them automatically
- `Feature` Deterministic Agent context packing with byte limits, complete recent step pairs, English/Chinese prompts and priority node selection; local models use a 3000-token input budget and compact tool signatures
- `Feature` Host model client core with validated event order, usage accounting, cancellation, deadlines and bounded format fallback; each fallback counts as a model call and preserves the decision repair allowance
- `Feature` Launcher connection requests with a 15-second timeout and guidance to enable and authorize AI Agent in AutoJs6
- `Feature` Task-only foreground notifications with progress, Stop and View actions; input and per-action confirmation can be answered from the launcher
- `Feature` Registered scripts refresh at task start, with a 60-second link cache, deterministic keyword ranking of up to 24 candidates, bounded parameter summaries and script_catalog queries
- `Fix` Notification confirmation returns to the target app before resuming actions, acknowledgements survive the screen stopping, and floating replies collapse the card before execution
- `Fix` Opening the app on Android 13 no longer crashes when the system bar controller is read before the window decor exists
- `Fix` Recent history uses task start times for ordering and retention so rewriting files during restart cannot evict newer tasks
- `Fix` Input and confirmation responses enforce interaction ownership so scripts cannot answer on behalf of the plugin interface
- `Fix` Cashier buttons labeled Confirm transaction require a separate payment confirmation and cannot reuse run-wide permissions
- `Fix` Offscreen matches with empty or inverted bounds retain their text and are marked as having unusable coordinates instead of reporting argument errors
- `Fix` Node relocation distinguishes container bounds and action capabilities to avoid confusing nested containers with the target
- `Fix` Actionable node target repair hints preserve the # reference prefix and omit snapshotId for selectors
- `Fix` Task admission preloads order intent rules and avoids expensive rule compilation
- `Fix` Verification distinguishes matching nodes in different windows, keeps screen observation requirements after clipboard reads, and avoids classifying file transfers as payments
- `Fix` Post-action screen reads that stop responding no longer exceed the stabilization deadline
- `Fix` Console redaction now handles multiline parameter values and parameter text matching credential labels before splitting or clipping lines
- `Fix` A retiring foreground service no longer rejects the next task while its replacement is starting
- `Improvement` Script confirmation descriptions account for JSON escaping so large parameter tables stay within the Binder event limit
- `Improvement` Minimum host is AutoJs6 6.8.0 / build 5289 for action node inspection and confirmation bound to execution
- `Dependency` Staged common-plugin-api, host-capability-api and ai-agent-api from one AutoJs6 6.8.0 / 5289 release build (MPL 2.0), with SHA-256 locks
- `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- AutoJs6 discussion #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md

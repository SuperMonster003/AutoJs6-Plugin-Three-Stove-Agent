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

Development preview for 1.0.0. Task APIs and interface entry points are implemented, and P7 audit evidence is recorded. P8 release checks and publication are still pending. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for passed cases and known limitations.

******

### Features

******

The current implementation provides these capabilities:

- Script selection: scripts registered through `project.json` or an `@agent` header comment are listed to the model with their descriptions and parameter schemas; the agent picks one, completes the parameters, asks for confirmation when required, runs it inside AutoJs6 and reads its structured result.
- Step-by-step screen operation: the agent observes the accessibility node tree in a compact text form (and screen text through an OCR plugin when one is installed), then clicks, types, scrolls and presses keys through the AutoJs6 capability broker until it can verify the goal.
- Safety by design: read-only tools run automatically, sensitive actions (payment, sending, deletion, file writes, shell, coordinate gestures, scripts registered as sensitive) require confirmation, and every run has step, model-call, duration and token budgets.
- Script API and user interface: `ai.agent.run(goal, options)` returns an `AgentRun` handle with events, responses and cancellation; the standalone app offers a task workbench with history, presets, preference memory, settings and release history.

### Screenshots

Actual English interface rendered on Android API 37.1 with synthetic tasks and a scripted demo model. These images illustrate the interface, not real-model task success. No private account data is included. [Capture procedure](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| Task workbench | Task details |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Task workbench" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="Task details" width="288" /> |
| Action confirmation | Floating task input |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Action confirmation" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="Floating task input" width="288" /> |

******

### Installation

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5293 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.

Install and enable [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), then configure an online model or import a supported local model there. The current host model broker selects 3-Stone AI; support for another Provider requires host integration. Select a target in AI Agent > Presets. Connected to AutoJs6 shows the host connection; it is not a model picker.

### Compatibility

Android 7.0+ (API 24). Host attachment requires AutoJs6 6.8.0 / build 5289+, while the complete task API and this quick start require build 5293+. Use a host build containing the Agent changes. Enable the host accessibility service for screen actions. OCR is optional and requires an installed, authorized OCR plugin reported as available by the host. The AI Agent plugin has no model credentials or accessibility service of its own.

### Quick start from the interface

Open AI Agent, connect to AutoJs6, enter a goal, select the default preset and start. Answer questions or confirm actions in the task card; open recent tasks to review their details.

### Quick start from a script

Run this JavaScript in AutoJs6 after connecting AI Agent and configuring a model. Questions and confirmations are handled by the plugin interface. To reuse a saved configuration, add `preset: "your-preset-name"` to the options.

```javascript
let run = ai.agent.run('Read the Android version and report the observed value.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

Read `result.status`: a resolved result may be completed, partial, failed, blocked or cancelled. `run.cancel()` stops the task. See the [ai.agent API](https://docs.autojs6.com/#ai) for target selection, events, budgets and script-owned responses.

### Register a script

Save the following as `text-counter.js` in the AutoJs6 working directory or a host-approved Script directory. The leading `@agent` JSDoc opts the file into the catalog. Ask the agent to count the characters in a specified text; missing required parameters are requested before execution.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

Alternatively, put this `project.json` beside `main.js`, whose body reads `ai.agent.context().parameters` and calls `ai.agent.result(...)` as above. A project registration belongs in the `agent` object.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

Parameter types are string, number, integer and boolean; nested objects and arrays are unsupported. Sensitive scripts always require confirmation before running. Register only scripts you have reviewed: a risk declaration does not sandbox JavaScript. [Complete manifest format](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Tool catalog

This table is generated from the packaged ToolCatalog. Risk can be raised by the actual screen target; cautious mode also confirms non-read-only actions. Settings, presets, task options and host grants all constrain the available groups.

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

### Presets and memory

Open Presets in the workbench to save a task configuration. Names are stable script and memory identifiers; copy a preset to use another name. The built-in default can be edited but not deleted. Choose a model from the host catalog, or keep automatic selection. A missing selected model fails without switching targets. Task options can further narrow preset limits. Fixed and task context share an 8 KiB limit. Memory scope can include global and current-preset entries, either one, or neither. Editing or deleting a preset does not change queued tasks. Up to 32 presets / 1 MiB are stored privately.

Open Memory to review, edit, delete or back up preferences. Up to 500 entries / 256 KiB; each retains its scope, source task and timestamps. Confirm each memory_propose and each imported entry separately. Unknown preset scopes require that preset to exist first. Automatic injection uses up to 4 KiB of the newest entries in the allowed scope; current-preset values override global values with the same key. memory: false disables automatic injection only; disable the memory tool group or select no memory scope to also block queries and proposals. Export includes actual values and provenance. Do not store credentials; recognized credential keys and token formats are rejected.

### Usage

- Configure extra folders in the launcher's "Script directories", one absolute path per line. The host validates and applies saved paths; tasks can only narrow the approved folders.
- Up to 200 tasks / 32 MiB. Older, least recently viewed finished tasks are removed first. Rerun fills the original goal and preset in the workbench. Review them and press Start task to execute again. Clearing history keeps running tasks. The export keeps diagnostic counters, tool names and confirmation outcomes. Goals, parameters, observations and script results are removed. Choose where to save the file.
- Answer in the workbench while it is open. In the background, open the high-priority notification to review the specific request. Confirmations show the tool, parameters, risk and time remaining. Allowing similar actions applies only to this tool at this risk level in this task; payments and memory proposals always require individual approval. Remember this answer creates a separate memory_propose for review, within the allowed memory scope. Confirmation normally waits 120 seconds, questions up to 10 minutes, both bounded by the task budget. Timeout returns USER_TIMEOUT; the model may ask again or report partial completion. Old requests cannot answer new ones. Notification permission and channel settings affect background delivery.
- Open Settings from the workbench to choose tool groups, budgets, cautious mode, voice input and the default preset. Changes apply to new tasks. gesture/files/shell are initially off; OCR requires an available authorized host plugin. Budgets inherit stock defaults when blank and remain within protocol limits. Presets and task options can only narrow them. Data management shows counts and bytes; category clearing requires confirmation and no active task. Clearing presets restores the built-in default. Script folders, licenses and source links are also available.
- Release history and legal notices are bundled for offline reading. Check updates manually through GitHub Releases, with a 24-hour success cache, cancellation and an ignored-version setting. The dialog opens release history inside the app or the release page in a browser. Checks never run automatically and APKs are not downloaded.
- Enable the floating ball in Settings, allow display over other apps, then save. It is off by default, appears only while AutoJs6 is connected, hides on lock or disconnect, and has no idle foreground service. Drag to move; tap to enter a goal, choose a preset, review a question or confirmation, or stop a task. Collapsing the card restores background confirmation notifications. Share plain text to AI Agent, use the New task app shortcut, or pin a preset with an optional goal from Presets. All entries open editable drafts and require Start task. A deleted preset never falls back silently. Voice uses the system recognizer in the interface language, is hidden when unavailable and fills text without sending.

### Frequently asked questions

**Why is AutoJs6 required?**

The plugin owns the task loop and interface. AutoJs6 owns model access, accessibility actions and registered-script execution. Without a connected compatible host, history can be read but new device tasks cannot run. Host loss blocks active tasks; reconnecting never automatically replays them.

**Why must every payment be confirmed?**

Payment is a separate sensitive action. Approval of an order, a script or similar actions does not approve payment. Each detected payment action requires its own confirmation, and a timeout is a refusal. Check the merchant, items, address and amount before approving.

**What are the limits of local models?**

Tasks depend on instruction following, valid decision JSON and the available context. Small models may fail even when loading succeeds; the Gemma 4 E2B IT Wi-Fi case did not pass the recorded decision-validation run. Start with small tasks and review partial/failed results. Version 1.0.0 uses text node/OCR observations and a JSON decision loop; visual input, native tool calling and generated scripts remain in the 1.1.0 roadmap.

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

- `Hint` Development preview for 1.0.0. Task APIs and interface entry points are implemented, and P7 audit evidence is recorded. P8 release checks and publication are still pending. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for passed cases and known limitations.
- `Hint` Requires Android 7+, AutoJs6 6.8.0 / build 5293+ for task APIs, and an enabled 3-Stone AI plugin with a configured model. OCR is optional. The attachment protocol alone requires host build 5289+.
- `Feature` Natural-language task workbench with inline questions, progress, stop and results; optional floating input, text sharing, preset shortcuts and voice drafts
- `Feature` ai.agent script API for task creation, events, queries, responses and cancellation, including detached tasks and registered-script result/context access
- `Feature` Registered project.json / @agent scripts with catalog search, parameter validation and defaults, missing-value questions, confirmation, bounded execution and structured results
- `Feature` Screen observation through text nodes and optional authorized OCR, reference-bound clicks, input, scrolling and keys, with screen-change checks and completion evidence
- `Feature` Online and local model targets through the AutoJs6 host broker, without storing model credentials; a missing selected target fails without silently switching models
- `Feature` Step, model-call, duration and token budgets, bounded tool deadlines, at most two decision-repair retries per step and protection against repeated ineffective actions
- `Feature` Named presets and global settings for model selection, context, tool groups, budgets, cautious mode, script folders and memory scope; gesture/files/shell are off by default
- `Feature` Scoped preference memory with individual proposal/import approval, editing, deletion and JSON backup, capped at 500 entries / 256 KiB; automatic injection is limited to 4 KiB
- `Feature` Task details and timelines, filters, rerun drafts and redacted JSON export, with private history capped at 200 tasks / 32 MiB
- `Feature` Risk-based confirmation in the workbench, notifications and floating card; payments and memory always require individual approval; host loss blocks tasks and process restart never resumes them automatically
- `Feature` Settings, offline release history and legal notices in ten languages; manual GitHub release checks with cancellation, daily caching and ignored versions, without automatic APK downloads
- `Fix` Form and filter touch targets, wrapped picker labels and script parameter columns, and floating control layout with large fonts and on Android 7
- `Fix` Credential validation bypasses in preference memory involving fullwidth characters, zero-width characters and additional credential names
- `Fix` The floating task ball could remain hidden after waking an unlocked device while screen state was still settling
- `Fix` Interrupted tasks are recorded as failed after plugin process death; locked-screen observations stop actions until the device is unlocked
- `Fix` File tools reject traversal, absolute paths and invalid workspace paths before confirmation or host dispatch; task history records bounded rejection categories without rejected model text
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
- `Improvement` Task context packing reuses unchanged prompt and observation fragments while trimming long histories, reducing per-step processing time
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

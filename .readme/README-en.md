<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-stove-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Runs natural-language tasks in AutoJs6 by choosing registered scripts and operating the screen step by step</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Stove Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

3-Stove Agent is a standalone task workbench and an AutoJs6 plugin reached through ai.agent. Built-in device actions and model calls use the host brokers. Optional MCP tools connect only to servers configured by the user. No direct model-provider binding or accessibility permission is used.

******

### Status

******

Version 1.2.0 ships optional MCP tools, native tool calling, screenshot observation and generated scripts, accepted on five real devices and on API 24 / 35 / 36.1 emulators. Known limitations: small on-device models (Gemma 4 E2B / E4B) decide poorly; failures after VPN network switching with default auto-connect remain unresolved; a complete cross-UID vision task has not been accepted, AiGoCode gpt-5.6-sol only passed the initial-image and tool-result-image probes. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md) for the evidence.

******

### Features

******

The current implementation provides these capabilities:

- Script selection: scripts registered through `project.json` or an `@agent` header comment are listed to the model with their descriptions and parameter schemas; the agent picks one, completes the parameters, asks for confirmation when required, runs it inside AutoJs6 and reads its structured result.
- Step-by-step screen operation: the agent observes the accessibility node tree in a compact text form (and screen text through an OCR plugin when one is installed), then clicks, types, scrolls and presses keys through the AutoJs6 capability broker until it can verify the goal.
- Safety by design: read-only tools run automatically, sensitive actions (payment, sending, deletion, file writes, shell, coordinate gestures, scripts registered as sensitive) require confirmation by default, and every run has step, model-call, duration and token budgets. A confirmation can be allowed once or for the rest of the task; Settings also offers cautious mode and full access, which skips confirmations and is clearly labeled. The payment app list and the sensitive keyword table can be extended on the Risk recognition settings screen; packaged entries cannot be removed.
- Script API and user interface: `ai.agent.run(goal, options)` returns an `AgentRun` handle with events, responses and cancellation; the standalone app offers a task workbench with history, presets, preference memory, settings and release history.
- Native tool calling through the host: catalog schemas, whole-batch validation, sequential execution, individual confirmations, tool-result continuation and step records share the existing task rules
- Screenshot observations through AutoJs6 on Android 11+: screen_capture scales to a longest edge of 1280 and JPEG quality 70, with visual prompts, image-token admission and native tool-result attachments
- Generated JavaScript through script_run_source: the script_dynamic group is off by default. Each call shows a source summary with expandable full text for approval once or for the current task; full access skips this review. Execution has a timeout, cancellation, structured results and private source records. Both source UTF-8 and its JSON string encoding are limited to 8 KiB.
- MCP tools from selected local or external servers, with per-server risk settings and the mcp group disabled by default
- Redesigned standalone app on Material 3: the home screen is a task feed with the composer docked above the keyboard, a top bar with the model capsule, history and a menu (New task, Presets, Memory, Script directories, MCP servers, Settings), a connection banner only while AutoJs6 is not connected, a keyed step timeline, and Run again, which fills the composer without starting. Settings are grouped into clear sections and light/dark appearance is consistent

### Screenshots

Actual English interface rendered on Android API 37.1 with synthetic tasks and a scripted demo model. These images illustrate the interface, not real-model task success. No private account data is included. [Capture procedure](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/README.md).

| Task workbench | Task details |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/workbench.png?raw=true" alt="Task workbench" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/detail.png?raw=true" alt="Task details" width="288" /> |
| Action confirmation | Floating task input |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="Action confirmation" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/docs/images/floating.png?raw=true" alt="Floating task input" width="288" /> |

******

### Installation

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) on a device with AutoJs6 build 5293 or later.
2. Open the AutoJs6 plugin center, confirm that `3-Stove Agent` is recognized, and enable it. Official release packages pass signature verification automatically.

Install and enable [3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI), then configure an online model or import a supported local model there. The current host model broker selects 3-Stone AI; support for another Provider requires host integration. Choose a model with the model capsule on the 3-Stove Agent home screen. A banner appears there only while AutoJs6 is not connected.

### Compatibility

Android 7.0+ (API 24). Requires an AutoJs6 6.8.0 / build 5298+ host, which already contains every host change needed by the task API (build 5293+) and by native tool calling and image input (build 5297+). Screen actions need the host accessibility service; the Agent first starts it through the unattended method configured in AutoJs6 (Root, secure settings or Shizuku) and asks you to enable it only when that fails. OCR is optional and requires an installed, authorized OCR plugin reported as available by the host. The 3-Stove Agent plugin has no model credentials or accessibility service of its own.

### Quick start from the interface

Open 3-Stove Agent, connect to AutoJs6, enter a goal and start. The model capsule on the home screen chooses an online or local model, or Automatic (an on-device model first, otherwise the first available one). Search models, pin favorites and reuse recent ones; badges show declared tool-calling and image-input support. The workbench and the floating ball share this choice for new tasks; it never edits a preset or a running task, and presets no longer carry a model. The preset chip in the composer selects an optional preset. Answer questions and review progress in the task card.

### Quick start from a script

Run this JavaScript in AutoJs6 after connecting 3-Stove Agent and configuring a model. Questions and confirmations are handled by the plugin interface. To reuse a saved configuration, add `preset: "your-preset-name"` to the options.

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
if (!context) throw Error('Start this registered script through 3-Stove Agent');
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

Parameter types are string, number, integer and boolean; nested objects and arrays are unsupported. Sensitive scripts require confirmation before running unless full access is selected. Register only scripts you have reviewed: a risk declaration does not sandbox JavaScript. [Complete manifest format](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### Tool catalog

This table is generated from the packaged ToolCatalog. Risk can be raised by the actual screen target; cautious mode also confirms non-read-only actions, while full access skips confirmation for enabled tools. Settings, presets, task options and host grants all constrain the available groups.

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
| `app_installed` | `observe` | `READ_ONLY` | `on` | Check whether an application package is installed. |
| `app_list` | `observe` | `READ_ONLY` | `on` | List installed applications, optionally filtered by a package name or label fragment; the result is bounded. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_capture` | `observe` | `READ_ONLY` | `auto (vision)` | Capture the unlocked screen for the selected vision model when text nodes are insufficient. Returns a scaled JPEG observation, not device coordinates. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_list` | `script` | `READ_ONLY` | `on` | List script executions currently running in AutoJs6 with their ids and states. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `script_run_source` | `script_dynamic` | `SENSITIVE` | `off` | Run generated Rhino JavaScript with host script privileges after individual source approval. No sandbox. Source including JSON escaping <=8192 UTF-8 bytes. Use ai.agent.result(value) for results. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

### Presets and memory

Open Presets in the workbench to save a task configuration. Names are stable script and memory identifiers; copy a preset to use another name. The built-in default can be edited but not deleted. Presets do not include a model; a model saved in a preset by an earlier version is kept for scripts only. Task options can further narrow preset limits. Fixed and task context share an 8 KiB limit. Memory scope can include global and current-preset entries, either one, or neither. Editing or deleting a preset does not change queued tasks. Up to 32 presets / 1 MiB are stored privately. Presets can be exported as JSON and imported after a per-preset review; the file carries no model, and tool groups or script directories missing on this device are dropped on import.

Open Memory to review, edit, delete or back up preferences. Up to 500 entries / 256 KiB; each retains its scope, source task and timestamps. Confirm each memory_propose and each imported entry separately. Unknown preset scopes require that preset to exist first. Automatic injection uses up to 4 KiB of the newest entries in the allowed scope; current-preset values override global values with the same key. memory: false disables automatic injection only; disable the memory tool group or select no memory scope to also block queries and proposals. Export includes actual values and provenance. Do not store credentials; recognized credential keys and token formats are rejected.

### Usage

- Configure extra folders in the launcher's "Script directories", one absolute path per line. The host validates and applies saved paths; tasks can only narrow the approved folders.
- Up to 200 tasks / 32 MiB. Older, least recently viewed finished tasks are removed first. Rerun fills the original goal and preset in the workbench. Review them and press Start task to execute again. Clearing history keeps running tasks. The export keeps diagnostic counters, tool names and confirmation outcomes. Goals, parameters, observations and script results are removed. Choose where to save the file.
- Answer in the workbench while it is open. In the background, open the high-priority notification to review the specific request. Confirmations show the tool, parameters, risk and time remaining. Always allow for this session approves the same tool at the same risk level until the task ends, including later memory proposals or generated sources; payments need their own approval. Remember this answer creates a separate memory_propose for review, within the allowed memory scope. Confirmation normally waits 120 seconds, questions up to 10 minutes, both bounded by the task budget. Timeout returns USER_TIMEOUT; the model may ask again or report partial completion. Old requests cannot answer new ones. Notification permission and channel settings affect background delivery.
- Open Settings from the workbench to choose tool groups, budgets, operation permissions (standard, cautious or full access), voice input and the default preset. Every change is saved immediately and applies to new tasks. Full access runs enabled tools, including payments, without approval; the workbench, floating ball and history show a warning label while it is active. gesture/files/shell/script_dynamic are initially off; OCR requires an available authorized host plugin. Limits set to Automatic use the stock defaults; duration is entered in minutes, and all values remain within protocol limits. Presets and task options can only narrow them. Data management shows counts and bytes; category clearing requires confirmation and no active task. Clearing presets restores the built-in default. Presets, memory, script directories and MCP servers also open from Settings. Failure alerts enable a notification, a floating message and a dialog independently for tasks that stop on an error, a budget limit or a lost host. On the workbench the access chip switches the operation permissions directly, the preset chip opens a sheet that picks or manages presets, and the overflow menu toggles the floating ball.
- Open Settings from the home screen's top-right menu. Language, dark mode and theme color can follow AutoJs6 or use independent preferences; language and dark mode can also follow Android. Version history and legal notices are bundled offline. Manual GitHub release checks reuse successful results for 24 hours. Automatic checks are off by default; when enabled, they run only while the app is in use, at most once every 12 hours, silently skip failures and ignored versions, and never download APKs. Manage ignored updates restores selected versions individually. About shows the version, developer, source code, license and third-party notices.
- Enable the floating ball in Settings and allow display over other apps. It is off by default, appears only while AutoJs6 is connected, hides on lock or disconnect, and has no idle foreground service. Drag to move; tap to enter a goal, choose a preset, review a question or confirmation, or stop a task. Collapsing the card restores background confirmation notifications. Share plain text to 3-Stove Agent, use the New task app shortcut, or pin a preset with an optional goal from Presets. All entries open editable drafts and require Start task. A deleted preset never falls back silently. Voice uses the system recognizer in the interface language, is hidden when unavailable and fills text without sending.

### Frequently asked questions

**Why is AutoJs6 required?**

The plugin owns the task loop and interface. AutoJs6 owns model access, accessibility actions and registered-script execution. Without a connected compatible host, history can be read but new device tasks cannot run. Host loss blocks active tasks; reconnecting never automatically replays them.

**When are payments confirmed?**

Payment is a separate sensitive action. Approving an order, a script or another action does not approve payment. By default each detected payment action needs its own confirmation, and a timeout is a refusal. Choosing Always allow for this session on a payment prompt covers only later payments by that tool in the same task. Full access skips payment confirmation, so enable it only for goals and models you trust. Check the merchant, items, address and amount before approving.

**What are the limits of local models?**

Model loading alone does not guarantee successful tasks. The recorded Gemma 4 E2B IT Wi-Fi decision-validation case did not pass; this target keeps the JSON path. Native tool calling also requires a compatible host and target and retains parameter, confirmation and budget checks. Start with small tasks and review partial/failed results. Image input requires a target that accepts images; AiGoCode gpt-5.6-sol passed initial-image and tool-result-image probes, while other targets require separate verification. Generated scripts are available only when explicitly enabled, and each source follows the confirmation policy.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- Permission list: org.autojs.permission.PLUGIN (host contract entry points), FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE (the foreground service while a task runs), POST_NOTIFICATIONS (background confirmation and progress notices), INTERNET (manual or automatic GitHub release checks and connections to user-configured MCP servers), ACCESS_LOCAL_NETWORK (requested only from MCP settings on Android 17+), SYSTEM_ALERT_WINDOW (requested only when the floating ball is enabled in Settings). No accessibility, storage or microphone permission is requested, and model traffic never passes through the plugin.
- Binder contract entries require the org.autojs.permission.PLUGIN signature permission. The launcher (also used by shortcuts) and the text/plain ACTION_SEND share target are public; they accept bounded goal/preset drafts only. External intents cannot execute tasks, provide confirmations or change grants. Private settings, voice results and task controls are not exported.
- 3-Stove Agent is a standalone task workbench and an AutoJs6 plugin reached through ai.agent. Built-in device actions and model calls use the host brokers. Optional MCP tools connect only to servers configured by the user. No direct model-provider binding or accessibility permission is used.
- Model credentials remain in the model provider and model calls go through AutoJs6. MCP Bearer tokens are encrypted in private storage with Android Keystore and are never included in prompts or history exports. INTERNET also connects to configured MCP servers; Android 17+ local network access is requested only from MCP settings. Remote tools have the selected server risk, initially SENSITIVE. Cancellation does not undo remote actions and failed calls are not replayed automatically.
- Task history, presets and preference memory stay in the plugin's private storage; backups and device transfers are disabled.
- Screenshots are sent through AutoJs6 to the selected model, which may be online. Capture requires an unlocked, interactive screen. Step history stores dimensions and byte counts, not picture contents. JSON decisions retain the current image until another observation or answer replaces it; native conversations retain earlier images within per-call and session limits and reserve their tokens again for each round.
- Generated scripts run with AutoJs6 permissions, without a JavaScript sandbox; they may perform actions outside the enabled tool groups. The full source is stored in private steps subject to existing password redaction and history retention. A source changed by later password redaction cannot be saved as the original script. Review exported .js contents before sharing.
- Full access can only be enabled in the plugin's private settings; model output, screen content, script requests and external intents cannot enable or widen it. It skips confirmations for enabled tools, including payments, but enables no extra tool groups and keeps budgets and host grants. The host starts accessibility through the method configured in AutoJs6; the plugin still requests no accessibility permission.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.three.stove.agent
plugin id: three-stove-agent
engine: three-stove-agent
variant: default
service action: org.autojs.plugin.THREE_STOVE_AGENT
service category: three-stove-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.three.stove.agent.api.IThreeStoveAgentPlugin
minimum host build: 5298 (6.8.0)
```

`ThreeStoveAgentPluginService` / `IThreeStoveAgentPlugin` / `IThreeStoveAgentLink`: Verified host attachment with queued tasks, responses, cancellation, queries and private step history; host loss blocks tasks and process restart never resumes them automatically.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.3.0

_2026/09/28_

- `Feature` Configurable risk recognition: a new Risk recognition settings screen adds your own package names and keywords on top of the packaged payment app list (Alipay, AlipayHK, UnionPay, PayPal, Google Wallet, Samsung Pay, Huawei Wallet, Mi Pay) and the ten-language sensitive keyword table; additions only widen the lists and apply at once, and matching screen actions become sensitive and go through confirmation
- `Feature` Floating ball redesign: the compact ball shows the task and its current step on two lines and tapping the text opens a step timeline card; the control card is condensed to the same two rows as the workbench (preset, model and access above the goal field, voice and start), with the model and access mode switched inside the overlay and shared with the app; the timeline follows new steps while you are at the end, pauses when you scroll up and resumes at the end, as does the task details screen
- `Feature` Preset import / export: the presets screen gains Import JSON and Export JSON; the exported file carries every preset's configuration but no model, each imported preset is reviewed one by one with a replace notice for an existing name, tool groups or script directories missing on this device are dropped, and a pending review survives leaving the screen
- `Feature` Observation tools completed: the read-only tools app_list (installed applications, optionally filtered by a package name or label fragment, at most 200 rows), app_installed (whether a package is installed) and script_list (script executions currently running in AutoJs6 with their ids and states, for use with script_stop) map to package_manager.listApps, app.isInstalled and engines.list, which the host grant already allowed; they join the observation and script groups and are enabled by default
- `Fix` The send button no longer stays on the first line of a multi-line goal; it sits at the bottom like the microphone button
- `Improvement` The Retry with another model buttons are gone (the model is switched at the top of the workbench or inside the floating ball); the floating ball keeps Task history and Open workbench in its More menu; the history icon uses the standard glyph

#### v1.2.0

_2026/09/28_

- `Hint` The app is renamed 3-Stove Agent: the application ID is now io.github.supermonster003.autojs6.plugin.three.stove.agent, the repository is AutoJs6-Plugin-Three-Stove-Agent, the plugin ID and engine are three-stove-agent, the service action is org.autojs.plugin.THREE_STOVE_AGENT and the contract version is 2. The old name is not supported: uninstall the old AI Agent before installing; history, presets and memories are not migrated. The minimum host is now AutoJs6 6.8.0 / build 5298; earlier hosts no longer recognize this plugin
- `Hint` Version 1.2.0 ships optional MCP tools, native tool calling, screenshot observation and generated scripts, accepted on five real devices and on API 24 / 35 / 36.1 emulators. Known limitations: small on-device models (Gemma 4 E2B / E4B) decide poorly; failures after VPN network switching with default auto-connect remain unresolved; a complete cross-UID vision task has not been accepted, AiGoCode gpt-5.6-sol only passed the initial-image and tool-result-image probes. See ROADMAP.md for the evidence.
- `Feature` The workbench composer shows the current access mode (Standard / Cautious / Full access; only Full access is red) and switches it on tap; the overflow menu gains a "Floating ball" checkbox synced with the setting; the expanded floating card minimizes on an outside tap, sizes itself to its content and has a More button with Minimize and Turn off; a one-line goal field centres its cursor with the voice and send buttons, and the buttons stay bottom-aligned as the field grows
- `Feature` Workbench preset sheet: tapping the preset chip opens a sheet that picks the preset and manages presets in place (new, edit, copy, set as default, delete with confirmation) or opens the full presets screen
- `Feature` Failure alerts: Settings gains a "Failure alerts" section with independent switches for a notification (on by default), a floating message and a dialog; when a task stops on an error, a budget limit or a lost host, the agent process raises the chosen alerts, the notification opens the task details, and on Android 10+ the dialog needs the overlay permission or falls back to a notification; completed and cancelled tasks never alert
- `Feature` MCP tools from selected local or external servers, with per-server risk settings and the mcp group disabled by default
- `Feature` Open 3-Stove Agent, connect to AutoJs6, enter a goal and start. The model capsule on the home screen chooses an online or local model, or Automatic (an on-device model first, otherwise the first available one). Search models, pin favorites and reuse recent ones; badges show declared tool-calling and image-input support. The workbench and the floating ball share this choice for new tasks; it never edits a preset or a running task, and presets no longer carry a model. The preset chip in the composer selects an optional preset. Answer questions and review progress in the task card.
- `Feature` Open Settings from the home screen's top-right menu. Every change applies immediately, without a Save button: appearance, operation permissions, tool groups, task limits (duration in minutes), voice input, the floating ball and data cleanup. Language, dark mode and theme color can follow AutoJs6 or use independent preferences; language and dark mode can also follow Android. Version history and legal notices are bundled offline. Manual GitHub release checks reuse successful results for 24 hours. Automatic checks are off by default; when enabled, they run only while the app is in use, at most once every 12 hours, silently skip failures and ignored versions, and never download APKs. Manage ignored updates restores selected versions individually. About shows the version, developer, source code, license and third-party notices.
- `Feature` Screen tasks first start accessibility through the unattended method configured in AutoJs6 (Root, secure settings or Shizuku). Only if that fails or none is configured does the task card ask you to enable it, with a shortcut to accessibility settings.
- `Feature` Operation permissions in Settings now include Full access: enabled tools, including payments, deletion, scripts and memory writes, run without approval. It enables no extra tool groups and does not relax budgets or host permissions. The workbench, floating ball, current task and history details show a clear label instead of an interrupting dialog. Tasks that explicitly request cautious confirmation keep it.
- `Feature` Confirmation cards add Always allow for this session: until the task ends, the same tool at the same risk level runs without asking again, even with different arguments. Payments need their own approval; generated scripts and memory proposals can also be allowed for the session.
- `Fix` Tasks stopping after several large screen observations or native tool rounds; completed steps are now retained while older context is compacted, with confirmation, budget and timeout limits preserved
- `Fix` On a 360 dp phone at twice the text size the host connection banner squeezed the "Connect to AutoJs6" button into one character per line (seen on a Redmi Note 12 and an Xperia XZ1 Compact); the banner's two actions now stack when they do not fit side by side
- `Fix` The budget note in the preset editor still quoted the defaults from before the relaxation (40 steps, 60 model calls, 600000 ms, 300000 tokens); it now matches the automatic budget (60 steps, 90 calls, 15 minutes, 30 minutes detached, 500000 tokens) and states the duration in minutes
- `Fix` The workbench access mode chip had no accessible name until the first status arrived, so a screen reader met an unnamed button (found by the CI layout audit at API 35); it is now named "Operation permissions" until the status supplies the mode
- `Fix` Task stop reasons are specific: the terminal summary carries a bracketed cause with the budget dimension and used/limit (steps 60/60, task time 900 s/900 s), which limit was exceeded (model response size, task context beyond the model input limit, tool result batch), or the error code with the host's fixed failure reason (MODEL_FAILED: ONLINE_NETWORK_UNAVAILABLE); host reasons other than REQUEST_REJECTED were dropped before
- `Fix` A long scope name in the memory list no longer pushes the entry key out of its row: the scope badge ellipsizes on one line and keeps the full name in its accessibility description (found by the CI layout audit at API 24 / 360 dp)
- `Fix` When an internal exception fails a task, the step record keeps the exception class (never its message) for diagnosis; a tool timeout now names the tool time limit dimension in the result; MCP tool discovery is capped at 8 seconds so it cannot consume the 15 second preparation window
- `Fix` Plugin capabilities now declare native-tools and vision, run limits bind directly to the host contract constants, the MCP client version comes from the installed package, and scattered timeout and size literals reference the contract
- `Fix` A stopped AutoJs6 accessibility service is reported to the model as A11Y_SERVICE_NOT_RUNNING instead of an argument error
- `Improvement` Management screens are aligned: the preset editor gives the fixed context its own "Context" section, the MCP server editor uses the same pinned action bar (Delete / Save) as the preset and memory editors, the task history retention note shares the other screens' caption style, and the floating card keeps a gap between its header and body
- `Improvement` Task details show the model, preset, elapsed time and task limits as two aligned key / value columns, with long values wrapping beside the label column, in the same style as the confirmation parameter table
- `Improvement` The home task card has a single status row: the state in its tone colour (running accent, completed green, failed red, partial amber) with the model and preset on the same line, and the budget caption is a compact "Step n/m · calls · minutes · tokens" line; on task details "Run again" is the full-width primary action and "Retry with another model" sits on its own line so it never wraps
- `Improvement` The automatic task budget is relaxed: steps 40 -> 60, model calls 60 -> 90, duration 10 -> 15 minutes, tokens 300k -> 500k; settings, presets and single tasks can still only narrow it
- `Improvement` The launcher icon is the maintainer-provided Three Stove artwork: a dark glyph on light grey in light mode, a light glyph on dark grey in dark mode, with the round and adaptive icons composed from the same source image
- `Improvement` The floating ball step label carries its full text for screen readers under the truncatable role, the pinned-models-full notice is an in-page snackbar, and the launcher declares a round icon; the UI kit drops unused members and shares its paragraph and note builders
- `Improvement` The tool catalog declares the forced confirmation of memory proposals and generated scripts with a confirmAlways attribute, and built-in tool names are referenced through ToolNames constants that the snapshot test keeps aligned with the catalog
- `Improvement` Model credentials remain in the model provider and model calls go through AutoJs6. MCP Bearer tokens are encrypted in private storage with Android Keystore and are never included in prompts or history exports. INTERNET also connects to configured MCP servers; Android 17+ local network access is requested only from MCP settings. Remote tools have the selected server risk, initially SENSITIVE. Cancellation does not undo remote actions and failed calls are not replayed automatically.
- `Improvement` Redesigned standalone app on Material 3: the home screen is a task feed with the composer docked above the keyboard, a top bar with the model capsule, history and a menu (New task, Presets, Memory, Script directories, MCP servers, Settings), a connection banner only while AutoJs6 is not connected, a keyed step timeline, and Run again or Retry with another model that fill the composer without starting. Settings are grouped into clear sections and light/dark appearance is consistent
- `Improvement` Confirmations show the risk level, the tool group and every parameter in a readable table instead of raw JSON, with Allow once, Always allow for this session and Deny as clear actions. The floating ball uses the same Material design and chooses presets inline; its model row opens the shared model switcher
- `Improvement` Task history adds search, status chips and preset and date range filters, and clears finished tasks from its menu. Task details show the model, a step timeline with parameter tables and expandable observations, Run again or Retry with another model, and a menu to export diagnostics, delete the record or use the task's model for new tasks
- `Improvement` Presets, memory, MCP servers and script directories share the same design: preset cards with a row menu and a full-page editor (duration in minutes, sticky Save), memory search with scope chips, an MCP tool checklist with an enable switch and risk choice, and a prompt before discarding unsaved changes
- `Dependency` Upgrade the three host API release artifacts to AutoJs6 86d9bfa26b / build 5298: ai-agent-api becomes three-stove-agent-api (AIDL package org.autojs.plugin.three.stove.agent.api, contract version 2), with common-plugin-api and host-capability-api relocked from the same build
- `Dependency` Upgrade the three host API release artifacts to AutoJs6 3cdf7de13c / build 5297 (P10 mcp group option and TOOL_FAILED constant); the base contract stays V1
- `Dependency` Add AndroidX AppCompat 1.7.1 and Material Components for Android 1.13.0 with their AndroidX runtime dependencies for the Material 3 interface

#### v1.1.0

_2026/09/26_

- `Hint` 1.1.0 was not published on its own; all of its changes ship with 1.2.0
- `Hint` Native calling requires AutoJs6 build 5297+ and a tools-capable target, such as an online target in the 3-Stone AI 1.2.0 development candidate. Older hosts and unsupported targets retain JSON decisions. Each native conversation retains its original timeout, context/output limits and at most 16 tool rounds; failures after a tool action never restart through JSON
- `Hint` Image input requires a compatible host, the observe group and an explicitly enabled vision-capable model. Implementation and deterministic tests are available; real online vision acceptance is still pending. Older systems and text-only targets keep text observations. See ROADMAP.md
- `Hint` Generated scripts run with AutoJs6 permissions, without a JavaScript sandbox; they may perform actions outside the enabled tool groups. The full source is stored in private steps subject to existing password redaction and history retention. A source changed by later password redaction cannot be saved as the original script. Review exported .js contents before sharing.
- `Feature` Native tool calling through the host: catalog schemas, whole-batch validation, sequential execution, individual confirmations, tool-result continuation and step records share the existing task rules
- `Feature` Screenshot observations through AutoJs6 on Android 11+: screen_capture scales to a longest edge of 1280 and JPEG quality 70, with visual prompts, image-token admission and native tool-result attachments
- `Feature` Generated JavaScript through script_run_source: the script_dynamic group is off by default. Every call requires review of a source summary with expandable full text and individual approval. Execution has a timeout, cancellation, structured results and private source records. Both source UTF-8 and its JSON string encoding are limited to 8 KiB.
- `Dependency` Upgrade the three host API release artifacts to AutoJs6 52ce694f92 / build 5297 for negotiated image input; preserve the base build 5289+ attachment contract

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

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

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- AutoJs6 discussion #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/THIRD_PARTY_NOTICES.md

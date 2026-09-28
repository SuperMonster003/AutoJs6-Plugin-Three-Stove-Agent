******

### Release History

******

# v1.3.0

###### 2026/09/28

* `Feature` Configurable risk recognition: a new Risk recognition settings screen adds your own package names and keywords on top of the packaged payment app list (Alipay, AlipayHK, UnionPay, PayPal, Google Wallet, Samsung Pay, Huawei Wallet, Mi Pay) and the ten-language sensitive keyword table; additions only widen the lists and apply at once, and matching screen actions become sensitive and go through confirmation
* `Feature` Floating ball redesign: the compact ball shows the task and its current step on two lines and tapping the text opens a step timeline card; the control card is condensed to the same two rows as the workbench (preset, model and access above the goal field, voice and start), with the model and access mode switched inside the overlay and shared with the app; the timeline follows new steps while you are at the end, pauses when you scroll up and resumes at the end, as does the task details screen
* `Feature` Preset import / export: the presets screen gains Import JSON and Export JSON; the exported file carries every preset's configuration but no model, each imported preset is reviewed one by one with a replace notice for an existing name, tool groups or script directories missing on this device are dropped, and a pending review survives leaving the screen
* `Fix` The send button no longer stays on the first line of a multi-line goal; it sits at the bottom like the microphone button
* `Improvement` The Retry with another model buttons are gone (the model is switched at the top of the workbench or inside the floating ball); the floating ball keeps Task history and Open workbench in its More menu; the history icon uses the standard glyph

# v1.2.0

###### 2026/09/28

* `Hint` The app is renamed 3-Stove Agent: the application ID is now io.github.supermonster003.autojs6.plugin.three.stove.agent, the repository is AutoJs6-Plugin-Three-Stove-Agent, the plugin ID and engine are three-stove-agent, the service action is org.autojs.plugin.THREE_STOVE_AGENT and the contract version is 2. The old name is not supported: uninstall the old AI Agent before installing; history, presets and memories are not migrated. The minimum host is now AutoJs6 6.8.0 / build 5298; earlier hosts no longer recognize this plugin
* `Hint` Version 1.2.0 ships optional MCP tools, native tool calling, screenshot observation and generated scripts, accepted on five real devices and on API 24 / 35 / 36.1 emulators. Known limitations: small on-device models (Gemma 4 E2B / E4B) decide poorly; failures after VPN network switching with default auto-connect remain unresolved; a complete cross-UID vision task has not been accepted, AiGoCode gpt-5.6-sol only passed the initial-image and tool-result-image probes. See ROADMAP.md for the evidence.
* `Feature` The workbench composer shows the current access mode (Standard / Cautious / Full access; only Full access is red) and switches it on tap; the overflow menu gains a "Floating ball" checkbox synced with the setting; the expanded floating card minimizes on an outside tap, sizes itself to its content and has a More button with Minimize and Turn off; a one-line goal field centres its cursor with the voice and send buttons, and the buttons stay bottom-aligned as the field grows
* `Feature` Workbench preset sheet: tapping the preset chip opens a sheet that picks the preset and manages presets in place (new, edit, copy, set as default, delete with confirmation) or opens the full presets screen
* `Feature` Failure alerts: Settings gains a "Failure alerts" section with independent switches for a notification (on by default), a floating message and a dialog; when a task stops on an error, a budget limit or a lost host, the agent process raises the chosen alerts, the notification opens the task details, and on Android 10+ the dialog needs the overlay permission or falls back to a notification; completed and cancelled tasks never alert
* `Feature` MCP tools from selected local or external servers, with per-server risk settings and the mcp group disabled by default
* `Feature` Open 3-Stove Agent, connect to AutoJs6, enter a goal and start. The model capsule on the home screen chooses an online or local model, or Automatic (an on-device model first, otherwise the first available one). Search models, pin favorites and reuse recent ones; badges show declared tool-calling and image-input support. The workbench and the floating ball share this choice for new tasks; it never edits a preset or a running task, and presets no longer carry a model. The preset chip in the composer selects an optional preset. Answer questions and review progress in the task card.
* `Feature` Open Settings from the home screen's top-right menu. Every change applies immediately, without a Save button: appearance, operation permissions, tool groups, task limits (duration in minutes), voice input, the floating ball and data cleanup. Language, dark mode and theme color can follow AutoJs6 or use independent preferences; language and dark mode can also follow Android. Version history and legal notices are bundled offline. Manual GitHub release checks reuse successful results for 24 hours. Automatic checks are off by default; when enabled, they run only while the app is in use, at most once every 12 hours, silently skip failures and ignored versions, and never download APKs. Manage ignored updates restores selected versions individually. About shows the version, developer, source code, license and third-party notices.
* `Feature` Screen tasks first start accessibility through the unattended method configured in AutoJs6 (Root, secure settings or Shizuku). Only if that fails or none is configured does the task card ask you to enable it, with a shortcut to accessibility settings.
* `Feature` Operation permissions in Settings now include Full access: enabled tools, including payments, deletion, scripts and memory writes, run without approval. It enables no extra tool groups and does not relax budgets or host permissions. The workbench, floating ball, current task and history details show a clear label instead of an interrupting dialog. Tasks that explicitly request cautious confirmation keep it.
* `Feature` Confirmation cards add Always allow for this session: until the task ends, the same tool at the same risk level runs without asking again, even with different arguments. Payments need their own approval; generated scripts and memory proposals can also be allowed for the session.
* `Fix` Tasks stopping after several large screen observations or native tool rounds; completed steps are now retained while older context is compacted, with confirmation, budget and timeout limits preserved
* `Fix` On a 360 dp phone at twice the text size the host connection banner squeezed the "Connect to AutoJs6" button into one character per line (seen on a Redmi Note 12 and an Xperia XZ1 Compact); the banner's two actions now stack when they do not fit side by side
* `Fix` The budget note in the preset editor still quoted the defaults from before the relaxation (40 steps, 60 model calls, 600000 ms, 300000 tokens); it now matches the automatic budget (60 steps, 90 calls, 15 minutes, 30 minutes detached, 500000 tokens) and states the duration in minutes
* `Fix` The workbench access mode chip had no accessible name until the first status arrived, so a screen reader met an unnamed button (found by the CI layout audit at API 35); it is now named "Operation permissions" until the status supplies the mode
* `Fix` Task stop reasons are specific: the terminal summary carries a bracketed cause with the budget dimension and used/limit (steps 60/60, task time 900 s/900 s), which limit was exceeded (model response size, task context beyond the model input limit, tool result batch), or the error code with the host's fixed failure reason (MODEL_FAILED: ONLINE_NETWORK_UNAVAILABLE); host reasons other than REQUEST_REJECTED were dropped before
* `Fix` A long scope name in the memory list no longer pushes the entry key out of its row: the scope badge ellipsizes on one line and keeps the full name in its accessibility description (found by the CI layout audit at API 24 / 360 dp)
* `Fix` When an internal exception fails a task, the step record keeps the exception class (never its message) for diagnosis; a tool timeout now names the tool time limit dimension in the result; MCP tool discovery is capped at 8 seconds so it cannot consume the 15 second preparation window
* `Fix` Plugin capabilities now declare native-tools and vision, run limits bind directly to the host contract constants, the MCP client version comes from the installed package, and scattered timeout and size literals reference the contract
* `Fix` A stopped AutoJs6 accessibility service is reported to the model as A11Y_SERVICE_NOT_RUNNING instead of an argument error
* `Improvement` Management screens are aligned: the preset editor gives the fixed context its own "Context" section, the MCP server editor uses the same pinned action bar (Delete / Save) as the preset and memory editors, the task history retention note shares the other screens' caption style, and the floating card keeps a gap between its header and body
* `Improvement` Task details show the model, preset, elapsed time and task limits as two aligned key / value columns, with long values wrapping beside the label column, in the same style as the confirmation parameter table
* `Improvement` The home task card has a single status row: the state in its tone colour (running accent, completed green, failed red, partial amber) with the model and preset on the same line, and the budget caption is a compact "Step n/m · calls · minutes · tokens" line; on task details "Run again" is the full-width primary action and "Retry with another model" sits on its own line so it never wraps
* `Improvement` The automatic task budget is relaxed: steps 40 -> 60, model calls 60 -> 90, duration 10 -> 15 minutes, tokens 300k -> 500k; settings, presets and single tasks can still only narrow it
* `Improvement` The launcher icon is the maintainer-provided Three Stove artwork: a dark glyph on light grey in light mode, a light glyph on dark grey in dark mode, with the round and adaptive icons composed from the same source image
* `Improvement` The floating ball step label carries its full text for screen readers under the truncatable role, the pinned-models-full notice is an in-page snackbar, and the launcher declares a round icon; the UI kit drops unused members and shares its paragraph and note builders
* `Improvement` The tool catalog declares the forced confirmation of memory proposals and generated scripts with a confirmAlways attribute, and built-in tool names are referenced through ToolNames constants that the snapshot test keeps aligned with the catalog
* `Improvement` Model credentials remain in the model provider and model calls go through AutoJs6. MCP Bearer tokens are encrypted in private storage with Android Keystore and are never included in prompts or history exports. INTERNET also connects to configured MCP servers; Android 17+ local network access is requested only from MCP settings. Remote tools have the selected server risk, initially SENSITIVE. Cancellation does not undo remote actions and failed calls are not replayed automatically.
* `Improvement` Redesigned standalone app on Material 3: the home screen is a task feed with the composer docked above the keyboard, a top bar with the model capsule, history and a menu (New task, Presets, Memory, Script directories, MCP servers, Settings), a connection banner only while AutoJs6 is not connected, a keyed step timeline, and Run again or Retry with another model that fill the composer without starting. Settings are grouped into clear sections and light/dark appearance is consistent
* `Improvement` Confirmations show the risk level, the tool group and every parameter in a readable table instead of raw JSON, with Allow once, Always allow for this session and Deny as clear actions. The floating ball uses the same Material design and chooses presets inline; its model row opens the shared model switcher
* `Improvement` Task history adds search, status chips and preset and date range filters, and clears finished tasks from its menu. Task details show the model, a step timeline with parameter tables and expandable observations, Run again or Retry with another model, and a menu to export diagnostics, delete the record or use the task's model for new tasks
* `Improvement` Presets, memory, MCP servers and script directories share the same design: preset cards with a row menu and a full-page editor (duration in minutes, sticky Save), memory search with scope chips, an MCP tool checklist with an enable switch and risk choice, and a prompt before discarding unsaved changes
* `Dependency` Upgrade the three host API release artifacts to AutoJs6 86d9bfa26b / build 5298: ai-agent-api becomes three-stove-agent-api (AIDL package org.autojs.plugin.three.stove.agent.api, contract version 2), with common-plugin-api and host-capability-api relocked from the same build
* `Dependency` Upgrade the three host API release artifacts to AutoJs6 3cdf7de13c / build 5297 (P10 mcp group option and TOOL_FAILED constant); the base contract stays V1
* `Dependency` Add AndroidX AppCompat 1.7.1 and Material Components for Android 1.13.0 with their AndroidX runtime dependencies for the Material 3 interface

# v1.1.0

###### 2026/09/26

* `Hint` 1.1.0 was not published on its own; all of its changes ship with 1.2.0
* `Hint` Native calling requires AutoJs6 build 5297+ and a tools-capable target, such as an online target in the 3-Stone AI 1.2.0 development candidate. Older hosts and unsupported targets retain JSON decisions. Each native conversation retains its original timeout, context/output limits and at most 16 tool rounds; failures after a tool action never restart through JSON
* `Hint` Image input requires a compatible host, the observe group and an explicitly enabled vision-capable model. Implementation and deterministic tests are available; real online vision acceptance is still pending. Older systems and text-only targets keep text observations. See ROADMAP.md
* `Hint` Generated scripts run with AutoJs6 permissions, without a JavaScript sandbox; they may perform actions outside the enabled tool groups. The full source is stored in private steps subject to existing password redaction and history retention. A source changed by later password redaction cannot be saved as the original script. Review exported .js contents before sharing.
* `Feature` Native tool calling through the host: catalog schemas, whole-batch validation, sequential execution, individual confirmations, tool-result continuation and step records share the existing task rules
* `Feature` Screenshot observations through AutoJs6 on Android 11+: screen_capture scales to a longest edge of 1280 and JPEG quality 70, with visual prompts, image-token admission and native tool-result attachments
* `Feature` Generated JavaScript through script_run_source: the script_dynamic group is off by default. Every call requires review of a source summary with expandable full text and individual approval. Execution has a timeout, cancellation, structured results and private source records. Both source UTF-8 and its JSON string encoding are limited to 8 KiB.
* `Dependency` Upgrade the three host API release artifacts to AutoJs6 52ce694f92 / build 5297 for negotiated image input; preserve the base build 5289+ attachment contract

# v1.0.0

###### 2026/09/25

* `Hint` Version 1.0.0 provides natural-language tasks, registered-script invocation and device actions with risk-based confirmation. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md) for verified cases, model limitations and outstanding device checks. Native tool calling, visual input and dynamic script generation are planned for 1.1.0.
* `Hint` Requires Android 7+, AutoJs6 6.8.0 / build 5293+ for task APIs, and an enabled 3-Stone AI plugin with a configured model. OCR is optional. The attachment protocol alone requires host build 5289+.
* `Hint` Compatibility note: the native-tool host extension in AutoJs6 build 5297 is compatible with this version. The 3-Stone AI 1.2.0 development candidate implements online tool continuation for three protocols. This Agent version still uses structured JSON decisions; native-loop adoption and task comparisons remain in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stove-Agent/blob/master/ROADMAP.md).
* `Feature` Natural-language task workbench with inline questions, progress, stop and results; optional floating input, text sharing, preset shortcuts and voice drafts
* `Feature` ai.agent script API for task creation, events, queries, responses and cancellation, including detached tasks and registered-script result/context access
* `Feature` Registered project.json / @agent scripts with catalog search, parameter validation and defaults, missing-value questions, confirmation, bounded execution and structured results
* `Feature` Screen observation through text nodes and optional authorized OCR, reference-bound clicks, input, scrolling and keys, with screen-change checks and completion evidence
* `Feature` Online and local model targets through the AutoJs6 host broker, without storing model credentials; a missing selected target fails without silently switching models
* `Feature` Step, model-call, duration and token budgets, bounded tool deadlines, at most two decision-repair retries per step and protection against repeated ineffective actions
* `Feature` Named presets and global settings for model selection, context, tool groups, budgets, cautious mode, script folders and memory scope; gesture/files/shell are off by default
* `Feature` Scoped preference memory with individual proposal/import approval, editing, deletion and JSON backup, capped at 500 entries / 256 KiB; automatic injection is limited to 4 KiB
* `Feature` Task details and timelines, filters, rerun drafts and redacted JSON export, with private history capped at 200 tasks / 32 MiB
* `Feature` Risk-based confirmation in the workbench, notifications and floating card; payments and memory always require individual approval; host loss blocks tasks and process restart never resumes them automatically
* `Feature` Settings, offline release history and legal notices in ten languages; manual GitHub release checks with cancellation, daily caching and ignored versions, without automatic APK downloads
* `Fix` Premature task completion when remaining budgets were interpreted as consumed budgets
* `Fix` Form and filter touch targets, wrapped picker labels and script parameter columns, and floating control layout with large fonts and on Android 7
* `Fix` Credential validation bypasses in preference memory involving fullwidth characters, zero-width characters and additional credential names
* `Fix` The floating task ball could remain hidden after waking an unlocked device while screen state was still settling
* `Fix` Interrupted tasks are recorded as failed after plugin process death; locked-screen observations stop actions until the device is unlocked
* `Fix` File tools reject traversal, absolute paths and invalid workspace paths before confirmation or host dispatch; task history records bounded rejection categories without rejected model text
* `Fix` Notification confirmation returns to the target app before resuming actions, acknowledgements survive the screen stopping, and floating replies collapse the card before execution
* `Fix` Opening the app on Android 13 no longer crashes when the system bar controller is read before the window decor exists
* `Fix` Recent history uses task start times for ordering and retention so rewriting files during restart cannot evict newer tasks
* `Fix` Input and confirmation responses enforce interaction ownership so scripts cannot answer on behalf of the plugin interface
* `Fix` Cashier buttons labeled Confirm transaction require a separate payment confirmation and cannot reuse run-wide permissions
* `Fix` Offscreen matches with empty or inverted bounds retain their text and are marked as having unusable coordinates instead of reporting argument errors
* `Fix` Node relocation distinguishes container bounds and action capabilities to avoid confusing nested containers with the target
* `Fix` Actionable node target repair hints preserve the # reference prefix and omit snapshotId for selectors
* `Fix` Task admission preloads order intent rules and avoids expensive rule compilation
* `Fix` Verification distinguishes matching nodes in different windows, keeps screen observation requirements after clipboard reads, and avoids classifying file transfers as payments
* `Fix` Post-action screen reads that stop responding no longer exceed the stabilization deadline
* `Fix` Console redaction now handles multiline parameter values and parameter text matching credential labels before splitting or clipping lines
* `Fix` A retiring foreground service no longer rejects the next task while its replacement is starting
* `Improvement` Task context packing reuses unchanged prompt and observation fragments while trimming long histories, reducing per-step processing time
* `Improvement` Script confirmation descriptions account for JSON escaping so large parameter tables stay within the Binder event limit
* `Improvement` Minimum host is AutoJs6 6.8.0 / build 5289 for action node inspection and confirmation bound to execution
* `Dependency` Staged common-plugin-api, host-capability-api and ai-agent-api from one AutoJs6 6.8.0 / 5289 release build (MPL 2.0), with SHA-256 locks
* `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

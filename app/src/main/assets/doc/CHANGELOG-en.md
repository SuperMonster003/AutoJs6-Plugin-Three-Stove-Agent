******

### Release History

******

# v1.0.0

###### 2026/09/25

* `Hint` Version 1.0.0 provides natural-language tasks, registered-script invocation and device actions with risk-based confirmation. See [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for verified cases, model limitations and outstanding device checks. Native tool calling, visual input and dynamic script generation are planned for 1.1.0.
* `Hint` Requires Android 7+, AutoJs6 6.8.0 / build 5293+ for task APIs, and an enabled 3-Stone AI plugin with a configured model. OCR is optional. The attachment protocol alone requires host build 5289+.
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

******

### Release History

******

# v1.0.0

###### 2026/09/24

* `Hint` Development preview: P6.1-P6.5 provide the workbench, history, presets and preference memory. The ai.agent API requires AutoJs6 build 5293 or later. Remaining interfaces continue in P6.6-P6.7; reliability and release gates remain in P7/P8.
* `Feature` Inline and notification confirmation with risk, countdown, task-scoped approval and separately confirmed answer memory
* `Feature` Preference memory with per-proposal confirmation, scoped queries, conflict protection, per-entry persistence, editing, deletion and JSON backup with individual import approval
* `Feature` Named presets with creation, editing, copying, deletion and default selection; host model catalog labels for locality and structured JSON, fixed context, narrower tool groups and budgets, confirmation policy, approved script folders and memory scope
* `Feature` Full task timelines and results, status/preset/date filters, rerun drafts, deletion, redacted JSON export, and versioned private history with migration and LRU cleanup at 200 tasks / 32 MiB
* `Feature` Task workbench with shared run admission, host appearance, inline interactions, budget progress and up to 20 recent tasks readable while disconnected
* `Feature` Completion requires evidence, partial results list unfinished work, and order or payment tasks must report an observed order state
* `Feature` Task verification tracks unchanged screens across context trimming and blocks the third equivalent action request before execution
* `Feature` Actions wait for a bounded stable screen sample and include changes since the last action in subsequent observations
* `Feature` Screen actions bind confirmation to inspected host nodes, support text append and bounded scrolling, and report action results and window changes
* `Feature` Screen OCR is offered only when the host reports an available authorized OCR plugin; text is merged into bounded lines with coordinates
* `Feature` Screen observations retain host snapshot references, bounded node and console output, and summaries of visible text and state changes
* `Feature` Single-script tasks retain the script ID, path, execution ID and reported result after model completion, including explicit null and marked truncation for large results
* `Feature` Registered script execution with confirmed manifest checks, structured observations, redacted console tails and owned-script stopping on timeout or task cancellation
* `Feature` Scoped preference memory injection for script parameters, with a 4 KiB limit, explicit truncation and task-level opt-out
* `Feature` Registered script parameter validation with defaults, missing-value questions, current-manifest risk checks and complete parameter tables for confirmation
* `Feature` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Feature` Agent core catalog with 30 tools, group admission, parameter schemas, bridge-call preparation, bounded observations and sensitive-risk escalation
* `Feature` Agent decision core with protocol-specific schemas, strict and extracted JSON parsing, tool/branch validation, at most two repair retries and English/Chinese prompt templates
* `Feature` Agent task budgets for steps, model calls, elapsed time and tokens, with bounded tool/wait deadlines, usage estimation and output-token admission
* `Feature` Agent confirmation gate with default/cautious policies, task-local grants for the same tool and risk, mandatory per-action payment confirmation, and payment keywords in 10 languages
* `Feature` Private Agent step journal capped at 200 steps and 1 MiB, with password-text redaction and bounded terminal results that retain status and counters
* `Feature` Verified host attachment with queued tasks, responses, cancellation, queries and private step history; host loss blocks tasks and process restart never resumes them automatically
* `Feature` Deterministic Agent context packing with byte limits, complete recent step pairs, English/Chinese prompts and priority node selection; local models use a 3000-token input budget and compact tool signatures
* `Feature` Host model client core with validated event order, usage accounting, cancellation, deadlines and bounded format fallback; each fallback counts as a model call and preserves the decision repair allowance
* `Feature` Launcher connection requests with a 15-second timeout and guidance to enable and authorize AI Agent in AutoJs6
* `Feature` Task-only foreground notifications with progress, Stop and View actions; input and per-action confirmation can be answered from the launcher
* `Feature` Registered scripts refresh at task start, with a 60-second link cache, deterministic keyword ranking of up to 24 candidates, bounded parameter summaries and script_catalog queries
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
* `Improvement` Script confirmation descriptions account for JSON escaping so large parameter tables stay within the Binder event limit
* `Improvement` Minimum host is AutoJs6 6.8.0 / build 5289 for action node inspection and confirmation bound to execution
* `Dependency` Staged common-plugin-api, host-capability-api and ai-agent-api from one AutoJs6 6.8.0 / 5289 release build (MPL 2.0), with SHA-256 locks
* `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

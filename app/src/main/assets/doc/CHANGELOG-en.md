******

### Release History

******

# v1.0.0

###### 2026/09/23

* `Hint` Development preview: the host link and task controls are connected. Script execution adapters and screen recovery continue in P3/P4; the script API and workbench follow in P5/P6.
* `Hint` Tasks are submitted by the host. The standalone workbench and ai.agent script API remain scheduled for P5/P6.
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
* `Improvement` Minimum host requirement finalized at AutoJs6 6.8.0 / build 5285, matching delivery of the P1 host interfaces and entry points
* `Dependency` Staged common-plugin-api, host-capability-api and ai-agent-api from one AutoJs6 6.8.0 / 5285 release build (MPL 2.0), with SHA-256 locks
* `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

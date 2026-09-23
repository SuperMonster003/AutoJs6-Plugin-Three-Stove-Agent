******

### Release History

******

# v1.0.0

###### 2026/09/23

* `Hint` The installed preview displays the host status. The P1 host interfaces and the P2.1-P2.3 tool, decision and runner cores are implemented and tested. Real task execution still needs the P2.4/P2.5 model and host integration and the P3/P4 execution adapters. The script API and workbench follow in P5/P6.
* `Hint` The host AI Agent contracts, capability and model brokers, screen observations, registered-script execution, drawer and plugin-center entries are implemented; plugin task execution remains under development
* `Feature` Plugin identity `ai-agent` with the INFO service, the Wake Activity, the `org.autojs.plugin.AI_AGENT` service placeholder in the `:agent` process, and a launcher screen that reports whether a compatible AutoJs6 host is installed
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Feature` Agent core catalog with 30 tools, group admission, parameter schemas, bridge-call preparation, bounded observations and sensitive-risk escalation; runtime integration follows in later phases
* `Feature` Agent decision core with protocol-specific schemas, strict and extracted JSON parsing, tool/branch validation, at most two repair retries and English/Chinese prompt templates; task execution is not connected yet
* `Feature` Agent task budgets for steps, model calls, elapsed time and tokens, with bounded tool/wait deadlines, usage estimation and output-token admission
* `Feature` Agent confirmation gate with default/cautious policies, task-local grants for the same tool and risk, mandatory per-action payment confirmation, and payment keywords in 10 languages
* `Feature` Private Agent step journal capped at 200 steps and 1 MiB, with password-text redaction and bounded terminal results that retain status and counters
* `Feature` Serial Agent runner and task queue (1 active + 8 waiting), cancellable model/tool ports, user interaction deadlines, unique terminal events and blocked state after host loss; real host integration remains in later phases
* `Feature` Deterministic Agent context packing with byte limits, complete recent step pairs, English/Chinese prompts and priority node selection; local models use a 3000-token input budget and compact tool signatures
* `Improvement` Minimum host requirement finalized at AutoJs6 6.8.0 / build 5285, matching delivery of the P1 host interfaces and entry points
* `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

******

### Release History

******

# v1.0.0

###### 2026/09/23

* `Hint` P0 development preview: the plugin identity, the AutoJs6 discovery contract and a launcher screen that reports the host status. The agent loop, the script catalog, the ai.agent API and the task workbench are not implemented yet. See ROADMAP.md.
* `Hint` The host AI Agent contracts, capability and model brokers, screen observations, registered-script execution, drawer and plugin-center entries are implemented; plugin task execution remains under development
* `Feature` Plugin identity `ai-agent` with the INFO service, the Wake Activity, the `org.autojs.plugin.AI_AGENT` service placeholder in the `:agent` process, and a launcher screen that reports whether a compatible AutoJs6 host is installed
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Feature` Agent core catalog with 30 tools, group admission, parameter schemas, bridge-call preparation, bounded observations and sensitive-risk escalation; runtime integration follows in later phases
* `Improvement` Minimum host requirement finalized at AutoJs6 6.8.0 / build 5285, matching delivery of the P1 host interfaces and entry points
* `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added Gson 2.13.2 for bounded strict JSON parsing and schema trees

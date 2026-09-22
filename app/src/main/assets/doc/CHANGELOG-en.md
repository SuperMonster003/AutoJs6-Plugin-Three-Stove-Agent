******

### Release History

******

# v1.0.0

###### 2026/09/23

* `Hint` P0 development preview: the plugin identity, the AutoJs6 discovery contract and a launcher screen that reports the host status. The agent loop, the script catalog, the ai.agent API and the task workbench are not implemented yet. See ROADMAP.md.
* `Hint` The host AI Agent contract, capability and model brokers, screen observations and registered-script execution are implemented. Plugin task execution remains under development
* `Feature` Plugin identity `ai-agent` with the INFO service, the Wake Activity, the `org.autojs.plugin.AI_AGENT` service placeholder in the `:agent` process, and a launcher screen that reports whether a compatible AutoJs6 host is installed
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`

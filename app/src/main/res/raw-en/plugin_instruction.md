AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Version 1.0.0 is the P0 development preview of the roadmap: the plugin identity, the AutoJs6 discovery contract (INFO service, Wake Activity, and the `org.autojs.plugin.AI_AGENT` service placeholder) and a launcher screen that reports the host status. The agent loop, the script catalog, the `ai.agent` API and the task workbench are not implemented yet; progress and evidence are tracked in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). The plugin will require AutoJs6 build 5283 or later.

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5283 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent from the launcher: in this preview the screen only reports whether a compatible AutoJs6 host is installed. The task workbench, the drawer entry and the `ai.agent` API arrive with the later roadmap phases.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for the connection guide and the current progress.

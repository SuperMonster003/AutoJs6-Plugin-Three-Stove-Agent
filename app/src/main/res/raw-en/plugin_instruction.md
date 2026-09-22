AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

The plugin runtime in version 1.0.0 remains a P0 development preview: INFO, Wake Activity, the `org.autojs.plugin.AI_AGENT` placeholder service and a host-status launcher. The host implements the P1 contracts, brokers, screen observations, registered-script execution, drawer and plugin-center entries. The plugin agent loop and script selection, the `ai.agent` API and the task workbench remain in later phases. AutoJs6 build 5285 is required; see [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for progress and evidence.

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5285 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent from the launcher or the management action in the AutoJs6 drawer. This preview only displays the host status; the task workbench and `ai.agent` API arrive in later phases.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for the connection guide and the current progress.

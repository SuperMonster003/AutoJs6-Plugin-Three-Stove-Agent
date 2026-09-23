AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

The installed preview displays the host status. The P1 host interfaces and the P2.1-P2.3 tool, decision and runner cores are implemented and tested. Real task execution still needs the P2.4/P2.5 model and host integration and the P3/P4 execution adapters. The script API and workbench follow in P5/P6. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5285 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent from the launcher or the management action in the AutoJs6 drawer. This preview only displays the host status; the task workbench and `ai.agent` API arrive in later phases.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for the connection guide and the current progress.

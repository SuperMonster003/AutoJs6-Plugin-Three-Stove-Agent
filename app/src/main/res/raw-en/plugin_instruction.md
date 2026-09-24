AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Development preview: the task workbench supports goal input, progress, inline replies and recent task details. The ai.agent task API requires AutoJs6 build 5293 or later. History management, custom presets and other entries continue in P6; reliability acceptance remains in P7. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5289 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent, connect to AutoJs6, enter a goal, select the default preset and start. Answer questions or confirm actions in the task card; open recent tasks to review their details.
4. Configure extra folders in the launcher's "Script directories", one absolute path per line. The host validates and applies saved paths; tasks can only narrow the approved folders.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for the connection guide and the current progress.

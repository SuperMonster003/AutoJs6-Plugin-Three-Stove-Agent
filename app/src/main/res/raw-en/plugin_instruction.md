AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

Development preview: P6.1-P6.3 provide the workbench, task history and named presets. The ai.agent API requires AutoJs6 build 5293 or later. Memory management and the remaining interfaces continue in P6.4-P6.7; reliability and release gates remain in P7/P8. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5289 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent, connect to AutoJs6, enter a goal, select the default preset and start. Answer questions or confirm actions in the task card; open recent tasks to review their details.
4. Configure extra folders in the launcher's "Script directories", one absolute path per line. The host validates and applies saved paths; tasks can only narrow the approved folders.
5. Up to 200 tasks / 32 MiB. Older, least recently viewed finished tasks are removed first. Rerun fills the original goal and preset in the workbench. Review them and press Start task to execute again. Clearing history keeps running tasks. The export keeps diagnostic counters, tool names and confirmation outcomes. Goals, parameters, observations and script results are removed. Choose where to save the file.
6. Open Presets in the workbench to save a task configuration. Names are stable script and memory identifiers; copy a preset to use another name. The built-in default can be edited but not deleted. Choose a model from the host catalog, or keep automatic selection. A missing selected model fails without switching targets. Task options can further narrow preset limits. Fixed and task context share an 8 KiB limit. Memory scope can include global and current-preset entries, either one, or neither. Editing or deleting a preset does not change queued tasks. Up to 32 presets / 1 MiB are stored privately.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) for the connection guide and the current progress.

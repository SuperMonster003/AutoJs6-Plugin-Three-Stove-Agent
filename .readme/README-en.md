<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Runs natural-language tasks in AutoJs6 by choosing registered scripts and operating the screen step by step</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### Introduction

******

AI Agent turns a natural-language goal into actions on an Android device running AutoJs6. It either picks a script that the user has registered for agent use, fills in its parameters and runs it, or observes the screen through the accessibility node tree and acts on it step by step (observe, decide, act, verify) until the goal is reached, a confirmation is needed, or a budget runs out. It answers [AutoJs6 discussion #577](https://github.com/SuperMonster003/AutoJs6/discussions/577).

The plugin is both an AutoJs6 plugin and a standalone app. Scripts reach it through the `ai.agent` API of AutoJs6; users reach it through its own task workbench, the AutoJs6 drawer, a floating ball, the system share sheet, app shortcuts, and voice input. Model calls and device actions always go through AutoJs6 over Binder: the host lends the plugin a model broker (the AI Provider plugins the host already knows, such as 3-Stone AI) and a capability broker with a bounded grant. The plugin never holds credentials, never binds a model provider itself, and never requests the accessibility permission.

******

### Status

******

Version 1.0.0 is the P0 development preview of the roadmap: the plugin identity, the AutoJs6 discovery contract (INFO service, Wake Activity, and the `org.autojs.plugin.AI_AGENT` service placeholder) and a launcher screen that reports the host status. The agent loop, the script catalog, the `ai.agent` API and the task workbench are not implemented yet; progress and evidence are tracked in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md). The plugin will require AutoJs6 build 5283 or later.

******

### Features

******

Release 1.0.0 is planned to provide the following capabilities:

- Script selection: scripts registered through `project.json` or an `@agent` header comment are listed to the model with their descriptions and parameter schemas; the agent picks one, completes the parameters, asks for confirmation when required, runs it inside AutoJs6 and reads its structured result.
- Step-by-step screen operation: the agent observes the accessibility node tree in a compact text form (and screen text through an OCR plugin when one is installed), then clicks, types, scrolls and presses keys through the AutoJs6 capability broker until it can verify the goal.
- Safety by design: read-only tools run automatically, sensitive actions (payment, sending, deletion, file writes, shell, coordinate gestures, scripts registered as sensitive) require confirmation, and every run has step, model-call, duration and token budgets.
- Script API and user interface: `ai.agent.run(goal, options)` returns an `AgentRun` handle with events, responses and cancellation; the standalone app offers a task workbench with history, presets, preference memory, settings and release history.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) on a device with AutoJs6 build 5283 or later.
2. Open the AutoJs6 plugin center, confirm that `AI Agent` is recognized, and enable it. Official release packages pass signature verification automatically.
3. Open AI Agent from the launcher: in this preview the screen only reports whether a compatible AutoJs6 host is installed. The task workbench, the drawer entry and the `ai.agent` API arrive with the later roadmap phases.

> In this preview the launcher screen only reports the host status; the AutoJs6 drawer entry, the `ai.agent` API and the task workbench arrive with roadmap phases P1, P5 and P6.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the launcher screen is the only other exported component.
- The plugin holds no API keys, never binds a model provider and does not request the accessibility permission: model calls and device actions go through brokers that AutoJs6 lends for one attached link and revokes on detach, each bounded by a grant (allowed methods, rates, sizes, model quota).
- The plugin does not use the network. This preview declares no permission besides the plugin permission; foreground service, notification and overlay permissions will be added with the features that need them and documented here.
- Task history, presets and preference memory stay in the plugin's private storage; backups and device transfers are disabled.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.ai.agent
plugin id: ai-agent
engine: ai-agent
variant: default
service action: org.autojs.plugin.AI_AGENT
service category: ai-agent
service process: :agent
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.ai.agent.api.IAiAgentPlugin
minimum host build: 5283 (6.8.0)
```

`AiAgentPluginService` answers `org.autojs.plugin.AI_AGENT` (category `ai-agent`) in the `:agent` process; in this preview it exposes a placeholder Binder carrying the descriptor `org.autojs.plugin.ai.agent.api.IAiAgentPlugin` until the host contract module is staged. `AiAgentPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/09/23_

- `Hint` P0 development preview: the plugin identity, the AutoJs6 discovery contract and a launcher screen that reports the host status. The agent loop, the script catalog, the ai.agent API and the task workbench are not implemented yet. See ROADMAP.md.
- `Hint` The host AI Agent contract, capability and model brokers, screen observations and registered-script execution are implemented. Plugin task execution remains under development
- `Feature` Plugin identity `ai-agent` with the INFO service, the Wake Activity, the `org.autojs.plugin.AI_AGENT` service placeholder in the `:agent` process, and a launcher screen that reports whether a compatible AutoJs6 host is installed
- `Feature` README, plugin-center instructions, and changelog in 10 languages
- `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- AutoJs6 discussion #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md

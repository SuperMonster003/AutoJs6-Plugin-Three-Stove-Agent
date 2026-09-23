<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-ai-agent-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>등록된 스크립트를 선택하고 화면을 단계별로 조작하여 AutoJs6에서 자연어 작업을 실행</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-AI-Agent?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-AI-Agent?color=534BAE&label=License"/></a>
  </p>
</div>

******

### 언어

******

현재 README.md는 다음 언어를 지원합니다:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ja.md)
- 한국어 [ko] # 현재
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/.readme/README-ar.md)

******

### 소개

******

AI Agent는 자연어 목표를 AutoJs6가 실행되는 Android 기기의 실제 동작으로 바꿉니다. 사용자가 에이전트용으로 등록한 스크립트를 골라 매개변수를 채우고 실행하거나, 접근성 노드 트리로 화면을 관찰하고 관찰, 결정, 실행, 검증의 순환으로 단계별로 조작합니다. 목표를 달성하거나 확인이 필요하거나 예산이 소진될 때까지 계속됩니다. [AutoJs6 토론 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577)에 대한 답입니다.

이 플러그인은 AutoJs6 플러그인이자 독립 실행형 앱입니다. 스크립트는 AutoJs6의 `ai.agent` API로, 사용자는 자체 작업 화면, AutoJs6 드로어, 플로팅 버튼, 시스템 공유 시트, 앱 바로가기, 음성 입력으로 이용합니다. 모델 호출과 기기 동작은 항상 Binder를 거쳐 AutoJs6가 수행합니다. 호스트는 플러그인에 모델 브로커 (3-Stone AI 등 호스트가 이미 아는 AI Provider 플러그인)와 범위가 제한된 grant가 붙은 기능 브로커를 빌려줍니다. 플러그인은 자격 증명을 보관하지 않고, 모델 제공자에 직접 바인딩하지 않으며, 접근성 권한도 요청하지 않습니다.

******

### 현재 상태

******

개발 미리보기: 호스트 연결과 작업 제어가 연결되었습니다. 스크립트 실행과 화면 복구는 P3/P4, 스크립트 API와 작업 화면은 P5/P6에서 구현합니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

******

### 기능

******

1.0.0에서는 다음 기능을 제공할 예정입니다:

- 스크립트 선택: `project.json` 또는 `@agent` 헤더 주석으로 등록한 스크립트를 설명과 매개변수 스키마와 함께 모델에 제시합니다. 에이전트는 스크립트를 고르고 매개변수를 채우며 필요하면 확인을 요청한 뒤 AutoJs6 안에서 실행하고 구조화된 결과를 읽습니다.
- 화면 단계별 조작: 에이전트는 접근성 노드 트리를 간결한 텍스트로 관찰하고 (OCR 플러그인이 설치되어 있으면 화면 텍스트도 읽음), AutoJs6 기능 브로커를 통해 클릭, 입력, 스크롤, 키 입력을 수행하며 목표를 검증할 수 있을 때까지 계속합니다.
- 설계 단계의 안전성: 읽기 전용 도구는 자동으로 실행되고, 민감한 동작 (결제, 전송, 삭제, 파일 쓰기, shell, 좌표 제스처, 민감으로 등록된 스크립트)은 확인이 필요하며, 모든 실행에는 단계 수, 모델 호출 수, 시간, 토큰 예산이 있습니다.
- 스크립트 API와 사용자 인터페이스: `ai.agent.run(goal, options)`은 이벤트, 응답, 취소를 갖춘 `AgentRun` 핸들을 반환합니다. 독립 실행형 앱은 기록, 프리셋, 선호 메모리, 설정, 릴리스 기록이 있는 작업 화면을 제공합니다.

### 도구 목록

호스트 신원 확인, 작업 대기열, 응답, 취소, 조회와 비공개 단계 기록; 호스트 연결이 끊어지면 작업을 차단하고 프로세스 재시작 후 자동으로 재개하지 않음. 작업은 호스트가 제출합니다. 작업 화면과 ai.agent 스크립트 API는 P5/P6에 예정되어 있습니다.

| 도구 | 그룹 | 위험 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `app_launch` | `act` | `NORMAL` | `on` | Open an application by package name or display name. |
| `clipboard_get` | `act` | `READ_ONLY` | `on` | Read clipboard text. |
| `clipboard_set` | `act` | `NORMAL` | `on` | Replace clipboard text. |
| `ui_click` | `act` | `NORMAL` | `on` | Click one observed target. |
| `ui_long_click` | `act` | `NORMAL` | `on` | Long-click one observed target. |
| `ui_press_key` | `act` | `NORMAL` | `on` | Use an Android navigation or notification-panel action. |
| `ui_scroll` | `act` | `NORMAL` | `on` | Scroll one observed target a bounded number of times. |
| `ui_set_text` | `act` | `NORMAL` | `on` | Set or append text on one observed editable target. |
| `files_list` | `files` | `NORMAL` | `off` | List workspace files. |
| `files_read` | `files` | `NORMAL` | `off` | Read bounded workspace file text. |
| `files_stat` | `files` | `NORMAL` | `off` | Read workspace file metadata. |
| `files_write` | `files` | `SENSITIVE` | `off` | Write a workspace file after confirmation. |
| `ui_click_xy` | `gesture` | `SENSITIVE` | `off` | Tap coordinates only with the gesture group enabled and confirmation. |
| `ui_gesture` | `gesture` | `SENSITIVE` | `off` | Follow a bounded coordinate path after confirmation. |
| `ui_swipe` | `gesture` | `SENSITIVE` | `off` | Swipe between coordinates after confirmation. |
| `memory_get` | `memory` | `READ_ONLY` | `on` | Read available preference memory in the current scope. |
| `memory_propose` | `memory` | `SENSITIVE` | `on` | Propose a preference for user-approved storage; never store credentials. |
| `app_current` | `observe` | `READ_ONLY` | `on` | Read the current window and application. |
| `console_tail` | `observe` | `READ_ONLY` | `on` | Read bounded recent console lines; they may include unrelated scripts. |
| `device_info` | `observe` | `READ_ONLY` | `on` | Read device information. |
| `screen_state` | `observe` | `READ_ONLY` | `on` | Read whether the screen is on. |
| `ui_dump` | `observe` | `READ_ONLY` | `on` | Observe the current accessibility tree before choosing an action. |
| `ui_find` | `observe` | `READ_ONLY` | `on` | Find nodes matching all selector conditions. |
| `ui_wait_for` | `observe` | `READ_ONLY` | `on` | Wait for a selector to appear or disappear within a deadline. |
| `ocr_screen` | `ocr` | `READ_ONLY` | `auto (OCR)` | Read screen text through the host OCR plugin. |
| `script_catalog` | `script` | `READ_ONLY` | `on` | Find scripts explicitly registered for Agent use. |
| `script_run` | `script` | `NORMAL` | `on` | Run a registered script by id with validated parameters and its registered risk. |
| `script_stop` | `script` | `NORMAL` | `on` | Stop an owned script execution. |
| `shell_exec` | `shell` | `SENSITIVE` | `off` | Execute a bounded non-root shell command after confirmation. |
| `report_progress` | `user` | `READ_ONLY` | `on` | Report bounded progress without declaring task completion. |

******

### 사용 방법

******

1. AutoJs6 빌드 5285 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `AI Agent`가 인식되는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. 런처에서 호스트 연결을 요청하고 15초가 지나면 AutoJs6의 AI Agent 활성화와 연결 허용 방법 안내.

> 작업은 호스트가 제출합니다. 작업 화면과 ai.agent 스크립트 API는 P5/P6에 예정되어 있습니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 진입점은 서명 권한 `org.autojs.permission.PLUGIN`으로 보호되어 AutoJs6만 접근할 수 있습니다. 시작 화면이 유일한 그 외의 내보낸 구성 요소입니다.
- 플러그인은 API 키를 보관하지 않고, 모델 제공자에 바인딩하지 않으며, 접근성 권한을 요청하지 않습니다. 모델 호출과 기기 동작은 AutoJs6가 연결된 링크 하나를 위해 빌려주고 분리 시 회수하는 브로커를 통해 이루어지며, 각 브로커는 grant (허용 메서드, 속도, 크기, 모델 할당량)로 제한됩니다.
- 플러그인은 네트워크를 사용하지 않습니다. 이 미리보기는 플러그인 권한 외에 어떤 권한도 선언하지 않습니다. 포그라운드 서비스, 알림, 오버레이 권한은 이를 필요로 하는 기능과 함께 추가되고 여기에 문서화됩니다.
- 작업 기록, 프리셋, 선호 메모리는 플러그인의 비공개 저장소에만 보관됩니다. 백업과 기기 간 이전은 비활성화되어 있습니다.

플러그인은 공식 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases) 페이지 또는 AutoJs6 플러그인 센터에서만 받으세요. 출처를 알 수 없는 패키지는 버전 번호가 같아 보여도 호스트 검증에 실패하거나 위험을 동반할 수 있습니다.

******

### 플러그인 인터페이스

******

다음 정보는 AutoJs6 호스트와 플러그인 개발자를 위한 것입니다. 호스트는 이 식별자로 플러그인을 발견하고 호환성을 협상합니다:

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
minimum host build: 5285 (6.8.0)
```

`AiAgentPluginService` / `IAiAgentPlugin` / `IAiAgentLink`: 호스트 신원 확인, 작업 대기열, 응답, 취소, 조회와 비공개 단계 기록; 호스트 연결이 끊어지면 작업을 차단하고 프로세스 재시작 후 자동으로 재개하지 않음.

******

### 로드맵

******

플러그인의 계획과 진행 상황은 ROADMAP.md에 체크 가능한 목록으로 관리되며, 단계별로 수락 기준과 증거 수준이 함께 기록됩니다. 체크되지 않은 항목은 현재 기능이 아니라 의도를 나타냅니다. Issues를 통한 논의를 환영합니다.

- [ROADMAP.md 보기](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)

******

### 릴리스 기록

******

#### v1.0.0

_2026/09/23_

- `힌트` 개발 미리보기: 호스트 연결과 작업 제어가 연결되었습니다. 스크립트 실행과 화면 복구는 P3/P4, 스크립트 API와 작업 화면은 P5/P6에서 구현합니다.
- `힌트` 작업은 호스트가 제출합니다. 작업 화면과 ai.agent 스크립트 API는 P5/P6에 예정되어 있습니다.
- `기능` `ai-agent`: `AiAgentPluginInfoService`, `WakeActivity`, `AiAgentPluginService`, `ui.LauncherActivity`
- `기능` 10개 언어의 README, 플러그인 센터 안내, 변경 기록
- `기능` 30 개 도구, 그룹 제어, 매개변수 Schema, bridge 호출 준비, 제한된 관찰 및 민감 위험 상향을 갖춘 Agent 코어 목록
- `기능` 프로토콜별 Schema, 엄격/추출 JSON 파싱, 도구/분기 검증, 최대 두 번의 수정 재시도 및 영어/중국어 프롬프트를 갖춘 Agent 결정 코어
- `기능` Agent 단계, 모델 호출, 시간, token 예산과 도구/대화 제한 시간, usage 추정 및 출력 token 제한
- `기능` Agent 확인 게이트에 기본/신중 정책, 작업 내 동일 도구와 위험에 한정된 허용, 결제마다 확인 및 10개 언어 결제 키워드 구현
- `기능` Agent 비공개 단계 로그를 200단계와 1 MiB로 제한, 비밀번호 마스킹 및 상태와 카운터를 유지하는 최종 결과 축약
- `기능` 호스트 신원 확인, 작업 대기열, 응답, 취소, 조회와 비공개 단계 기록; 호스트 연결이 끊어지면 작업을 차단하고 프로세스 재시작 후 자동으로 재개하지 않음
- `기능` Agent 컨텍스트 구성에 바이트 제한, 최근 전체 단계 쌍, 영어/중국어 프롬프트와 노드 우선 선택 구현; 로컬 모델은 입력 3000 token 예산과 간결한 도구 정의 사용
- `기능` 호스트 모델 클라이언트에 이벤트 순서 검증, usage 집계, 취소, 제한 시간 및 제한된 형식 전환 구현; 각 재시도는 모델 호출로 계산하고 결정 수정 한도를 유지
- `기능` 런처에서 호스트 연결을 요청하고 15초가 지나면 AutoJs6의 AI Agent 활성화와 연결 허용 방법 안내
- `개선` 최소 호스트 요구 사항을 AutoJs6 6.8.0 / 빌드 5285 로 확정하여 P1 호스트 인터페이스 및 진입점 제공 버전과 일치시켰습니다
- `의존성` 동일한 AutoJs6 6.8.0 / 5285 release 빌드의 common-plugin-api, host-capability-api 및 ai-agent-api (MPL 2.0) 추가, SHA-256으로 고정
- `의존성` 제한된 엄격 JSON 파싱과 Schema 트리를 위해 Gson 2.13.2 추가

##### 더 많은 릴리스 기록

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/app/src/main/assets/doc/CHANGELOG-ko.md)

******

### 빌드와 검증

******

이 섹션은 소스에서 플러그인을 빌드하려는 개발자를 위한 것입니다. 일반 사용자는 Releases 페이지의 미리 빌드된 APK를 설치하면 됩니다.

디버그 APK 빌드:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 단위 테스트 실행 및 계측 테스트 APK 빌드:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

릴리스 APK 빌드:

```powershell
.\gradlew.bat :app:assembleRelease
```

릴리스 산출물을 수집하고 파일 이름에 버전과 CRC32 다이제스트를 추가:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

다국어 문서 소스와 생성된 산출물이 동기화되어 있는지 검증 (CI에서도 적용):

```powershell
py .python\generate_markdown.py --check
```

빌드에는 JDK 21 이상과 Android SDK 37이 필요합니다. Gradle과 플러그인 버전은 `version.properties`와 `io.github.supermonster003.autojs6-platform-versions`로 중앙에서 관리됩니다.

******

### 현지화와 문서 생성

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

`.readme/`와 `.changelog/`의 언어 JSON 파일이 README, 플러그인 센터 안내, 변경 기록의 유일한 소스입니다. 항상 이 JSON 소스를 편집하고 `py .python/generate_markdown.py`를 다시 실행하세요. 생성된 README, `plugin_instruction.md`, 변경 기록 산출물은 절대 손으로 편집하지 않습니다. `py .python/generate_markdown.py --check`를 실행하면 모든 생성 산출물을 검증할 수 있습니다.

******

### 라이선스

******

프로젝트 코드는 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/LICENSE)에 따라 제공됩니다. 서드파티 구성 요소와 라이선스는 [서드파티 고지](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md)에 나열되어 있습니다.

******

### 링크

******

- AutoJs6 프로젝트: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 문서: https://docs.autojs6.com
- AutoJs6 토론 #577: https://github.com/SuperMonster003/AutoJs6/discussions/577
- 서드파티 고지: https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/THIRD_PARTY_NOTICES.md

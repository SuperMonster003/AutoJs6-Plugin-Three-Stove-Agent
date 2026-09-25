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

1.0.0 개발 미리 보기입니다. 작업 API와 화면 진입점이 구현되었고 P7 감사 증거가 기록되었습니다. P8 출시 검사와 공개는 아직 완료되지 않았습니다. 통과 사례와 알려진 제한은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)를 참고하세요.

******

### 기능

******

현재 구현은 다음 기능을 제공합니다:

- 스크립트 선택: `project.json` 또는 `@agent` 헤더 주석으로 등록한 스크립트를 설명과 매개변수 스키마와 함께 모델에 제시합니다. 에이전트는 스크립트를 고르고 매개변수를 채우며 필요하면 확인을 요청한 뒤 AutoJs6 안에서 실행하고 구조화된 결과를 읽습니다.
- 화면 단계별 조작: 에이전트는 접근성 노드 트리를 간결한 텍스트로 관찰하고 (OCR 플러그인이 설치되어 있으면 화면 텍스트도 읽음), AutoJs6 기능 브로커를 통해 클릭, 입력, 스크롤, 키 입력을 수행하며 목표를 검증할 수 있을 때까지 계속합니다.
- 설계 단계의 안전성: 읽기 전용 도구는 자동으로 실행되고, 민감한 동작 (결제, 전송, 삭제, 파일 쓰기, shell, 좌표 제스처, 민감으로 등록된 스크립트)은 확인이 필요하며, 모든 실행에는 단계 수, 모델 호출 수, 시간, 토큰 예산이 있습니다.
- 스크립트 API와 사용자 인터페이스: `ai.agent.run(goal, options)`은 이벤트, 응답, 취소를 갖춘 `AgentRun` 핸들을 반환합니다. 독립 실행형 앱은 기록, 프리셋, 선호 메모리, 설정, 릴리스 기록이 있는 작업 화면을 제공합니다.

### 화면 예시

Android API 37.1의 실제 영어 화면이며 예제 작업과 응답이 정해진 데모 모델을 사용합니다. 화면 설명용 이미지로 실제 모델의 작업 성공을 입증하지 않습니다. 개인 계정 정보는 포함하지 않습니다. [캡처 절차](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/README.md).

| 작업 화면 | 작업 상세 |
| --- | --- |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/workbench.png?raw=true" alt="작업 화면" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/detail.png?raw=true" alt="작업 상세" width="288" /> |
| 작업 승인 | 플로팅 작업 입력 |
| <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/confirmation.png?raw=true" alt="작업 승인" width="288" /> | <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/docs/images/floating.png?raw=true" alt="플로팅 작업 입력" width="288" /> |

******

### 설치

******

1. AutoJs6 빌드 5293 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `AI Agent`가 인식되는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.

[3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI)를 설치하고 활성화한 뒤 온라인 모델을 설정하거나 지원되는 로컬 모델을 가져오세요. 현재 호스트 모델 중개자는 3-Stone AI를 선택합니다. 다른 Provider는 호스트 통합이 필요합니다. AI Agent > 프리셋에서 모델을 선택하세요. Connected to AutoJs6는 호스트 연결 상태이며 모델 선택은 프리셋에 있습니다.

### 호환성

Android 7.0+ (API 24). 연결에는 AutoJs6 6.8.0 / build 5289+가 필요하고 전체 작업 API 및 이 예제에는 build 5293+가 필요합니다. Agent 변경이 포함된 호스트 빌드를 사용하세요. 화면 조작에는 호스트 접근성 서비스를 켜야 합니다. OCR은 선택 사항이며 설치 및 승인되었고 호스트가 사용 가능하다고 보고한 OCR 플러그인이 필요합니다. AI Agent는 모델 자격 증명을 저장하거나 자체 접근성 서비스를 제공하지 않습니다.

### 화면에서 시작

AI Agent를 열고 AutoJs6에 연결한 다음 목표를 입력하고 기본 프리셋으로 시작하세요. 작업 카드에서 응답하거나 작업을 확인하고 최근 작업에서 상세 정보를 확인하세요.

### 스크립트에서 시작

AI Agent 연결 및 모델 설정 후 AutoJs6에서 다음 JavaScript를 실행하세요. 질문과 승인은 플러그인 화면에서 처리됩니다. 저장된 설정을 쓰려면 옵션에 `preset: "your-preset-name"`을 추가하세요.

```javascript
let run = ai.agent.run('Android 버전을 읽고 실제로 관찰한 값을 보고하세요.', {
    tools: ['observe', 'user'],
    interaction: 'plugin',
    budget: { maxSteps: 8 },
});
run.on('progress', (event) => console.log(event.message));
run.result.then(
    (result) => console.log(result.status, result.summary),
    (error) => console.error(error.code, error.message),
);
```

`result.status`를 확인하세요. Promise가 이행되어도 결과는 completed, partial, failed, blocked 또는 cancelled일 수 있습니다. `run.cancel()`로 중지합니다. 모델, 이벤트, 예산 및 스크립트 응답은 [ai.agent API](https://docs.autojs6.com/#ai)를 참고하세요.

### 스크립트 등록

다음 예제를 AutoJs6 작업 폴더 또는 호스트가 승인한 스크립트 폴더의 `text-counter.js`로 저장하세요. 선두 `@agent` JSDoc이 파일을 목록에 등록합니다. 특정 텍스트의 문자 수를 세도록 요청하면 누락된 필수 매개변수를 실행 전에 묻습니다.

```javascript
/**
 * @agent
 * @description Count Unicode characters in the supplied text
 * @param {string} text Text to count
 * @risk readonly
 * @confirm never
 * @timeout 10000
 */
let context = ai.agent.context();
if (!context) throw Error('Start this registered script through AI Agent');
let text = new java.lang.String(context.parameters.text);
ai.agent.result({ characters: text.codePointCount(0, text.length()) });
```

프로젝트에서는 다음 `project.json`을 `main.js` 옆에 둘 수 있습니다. main.js는 위와 같이 `ai.agent.context().parameters`를 읽고 `ai.agent.result(...)`를 호출합니다. 등록 정보는 `agent` 객체에 둡니다.

```json
{
  "name": "Text counter",
  "main": "main.js",
  "agent": {
    "id": "text-counter",
    "description": "Count Unicode characters in the supplied text",
    "parameters": {
      "type": "object",
      "properties": { "text": { "type": "string" } },
      "required": ["text"],
      "additionalProperties": false
    },
    "risk": "readonly",
    "confirm": "never",
    "timeoutMs": 10000
  }
}
```

매개변수는 string, number, integer, boolean을 지원하며 중첩 객체와 배열은 지원하지 않습니다. sensitive 스크립트는 실행 전에 항상 승인받습니다. 검토한 스크립트만 등록하세요. 위험 선언이 JavaScript를 샌드박스로 격리하지는 않습니다. [전체 등록 형식](https://github.com/SuperMonster003/AutoJs6/blob/master/docs/dev/agent-script-manifest-v1.md).

### 도구 목록

이 표는 패키지의 ToolCatalog에서 생성됩니다. 실제 화면 대상에 따라 위험이 높아질 수 있으며 신중 모드는 읽기 전용 이외의 작업도 확인합니다. 설정, 프리셋, 작업 옵션과 호스트 권한이 사용 가능한 그룹을 제한합니다.

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

### 프리셋과 기억

작업 화면에서 프리셋을 열어 구성을 저장하세요. 이름은 스크립트와 메모리의 고정 식별자이며 다른 이름이 필요하면 복사합니다. 내장 default는 편집할 수 있지만 삭제할 수 없습니다. 호스트 목록에서 모델을 고르거나 자동 선택을 유지하세요. 지정 모델을 사용할 수 없으면 다른 모델로 바꾸지 않고 실패합니다. 작업 옵션은 프리셋 제한을 더 줄일 수만 있습니다. 고정 문맥과 작업 문맥의 합계는 8 KiB 이하입니다. 메모리는 전역과 현재 프리셋, 둘 중 하나 또는 사용 안 함을 선택합니다. 편집이나 삭제는 이미 대기 중인 작업을 바꾸지 않습니다. 비공개 저장소에 최대 32개 / 1 MiB를 저장합니다.

메모리에서 환경설정을 확인, 편집, 삭제하거나 백업합니다. 최대 500개 / 256 KiB이며 범위, 출처 작업과 시간을 보존합니다. memory_propose와 가져오기는 항목마다 확인해야 합니다. 없는 프리셋은 먼저 만드세요. 자동 주입은 허용 범위의 최신 완전한 항목부터 최대 4 KiB이며 같은 key는 현재 프리셋이 우선합니다. memory: false는 자동 주입만 끕니다. 조회와 제안도 차단하려면 memory 도구 그룹 또는 메모리 범위를 끄세요. 내보내기는 실제 값과 출처를 포함합니다. 자격 증명을 저장하지 마세요. 식별 가능한 자격 증명 키와 토큰 형식은 거부됩니다.

### 사용 방법

- 런처의 "스크립트 디렉터리"에서 추가 폴더를 설정하고 줄마다 절대 경로를 하나씩 입력하세요. 저장한 경로는 호스트가 검증하여 적용하며 작업은 승인된 폴더 범위만 좁힐 수 있습니다.
- 최대 200개 작업 / 32 MiB. 종료된 작업 중 가장 오래 조회하지 않은 항목부터 제거합니다. 재실행은 원래 목표와 프리셋을 작업 화면에 채웁니다. 확인 후 시작 버튼을 눌러 실행하세요. 기록을 비워도 실행 중인 작업은 유지됩니다. 진단 횟수, 도구 이름과 확인 결과를 보존합니다. 목표, 매개변수, 관찰 내용과 스크립트 결과는 제거됩니다. 저장 위치를 선택하세요.
- 앱이 열려 있으면 작업 화면에서 답변하고, 백그라운드에서는 높은 우선순위 알림으로 해당 요청을 엽니다. 확인 화면에 도구, 인수, 위험과 남은 시간이 표시됩니다. 작업 내 허용은 같은 도구의 같은 위험 수준에만 적용되며 결제와 기억 제안은 매번 확인합니다. 답변 기억은 허용된 범위에서 별도 memory_propose를 생성합니다. 확인은 보통 120초, 질문은 최대 10분이며 작업 예산도 적용됩니다. 시간이 지나면 USER_TIMEOUT을 반환하고 모델이 재질문 또는 부분 완료를 결정합니다. 이전 요청으로 새 요청에 답할 수 없습니다. 백그라운드 알림은 권한과 채널 설정의 영향을 받습니다.
- 작업 화면의 설정에서 도구 그룹, 예산, 신중 모드, 음성 입력과 기본 프리셋을 선택합니다. 변경 사항은 새 작업에 적용됩니다. gesture/files/shell은 기본으로 꺼져 있으며 OCR에는 호스트가 허용한 사용 가능한 플러그인이 필요합니다. 비어 있는 예산은 초기 기본값을 따르고 설정값은 프로토콜 상한 이내여야 합니다. 프리셋과 개별 옵션은 범위를 줄일 수만 있습니다. 데이터 관리에서 항목 수와 바이트를 확인하고 실행 중인 작업이 없을 때 확인 후 범주별로 지웁니다. 프리셋 삭제는 내장 default로 복원합니다. 스크립트 폴더, 라이선스와 소스 링크도 제공합니다.
- 릴리스 기록과 법적 고지는 오프라인으로 읽습니다. GitHub Releases 업데이트 확인은 수동으로 실행하며 성공 결과를 24시간 저장합니다. 취소와 버전 무시가 가능하고 앱 내 기록이나 브라우저 릴리스 페이지를 열 수 있습니다. 자동 확인이나 APK 다운로드는 하지 않습니다.
- 설정에서 플로팅 볼을 켜고 다른 앱 위에 표시를 허용한 후 저장하세요. 기본적으로 꺼져 있으며 AutoJs6 연결 중에만 표시되고 잠금이나 연결 해제 시 숨겨집니다. 대기 중 포그라운드 서비스는 없습니다. 드래그로 이동하고 눌러 목표 및 프리셋 입력, 질문 및 확인 응답, 작업 중지를 할 수 있습니다. 카드를 접으면 백그라운드 확인 알림이 복원됩니다. 일반 텍스트 공유, 새 작업 바로가기, 프리셋 화면의 고정 목표 바로가기를 사용할 수 있습니다. 모든 진입점은 편집 가능한 초안을 열며 시작 버튼을 눌러야 실행됩니다. 삭제된 프리셋을 자동 대체하지 않습니다. 음성 인식은 화면 언어를 사용하고 지원되지 않으면 숨겨집니다. 결과는 입력란에만 채우고 전송하지 않습니다.

### 자주 묻는 질문

**왜 AutoJs6가 필요한가요?**

플러그인은 작업 루프와 화면을 담당합니다. AutoJs6는 모델 접근, 접근성 동작과 등록 스크립트 실행을 담당합니다. 호환 호스트가 연결되지 않으면 기록은 볼 수 있지만 새 기기 작업은 시작할 수 없습니다. 호스트가 끊기면 실행 중인 작업은 blocked가 되며 재연결해도 자동 재실행하지 않습니다.

**왜 결제마다 승인해야 하나요?**

결제는 별도의 민감한 동작입니다. 주문이나 스크립트, 유사 동작을 승인해도 결제가 승인되지는 않습니다. 감지된 결제 동작마다 개별 승인이 필요하며 시간 초과는 거절입니다. 승인 전에 상점, 상품, 주소와 금액을 확인하세요.

**로컬 모델의 한계는 무엇인가요?**

작업은 지시 준수, 유효한 결정 JSON과 사용 가능한 문맥에 의존합니다. 작은 모델은 로드 후에도 작업에 실패할 수 있습니다. 기록된 Gemma 4 E2B IT Wi-Fi 사례는 결정 검증을 통과하지 못했습니다. 작은 작업부터 시작하고 partial/failed 결과를 확인하세요. 1.0.0은 노드/OCR 텍스트와 JSON 결정 루프를 사용합니다. 시각 입력, 네이티브 도구 호출 및 스크립트 생성은 1.1.0 계획에 남아 있습니다.

******

### 권한과 보안

******

플러그인은 명확한 경계를 따릅니다:

- Binder 계약 진입점은 org.autojs.permission.PLUGIN 서명 권한으로 보호됩니다. 런처 (바로가기 포함)와 text/plain ACTION_SEND 공유 대상은 공개되며 크기가 제한된 목표/프리셋 초안만 받습니다. 외부 Intent는 작업 실행, 확인 응답 또는 권한 변경을 할 수 없습니다. 설정, 음성 결과 및 작업 제어는 공개되지 않습니다.
- 플러그인은 API 키를 보관하지 않고, 모델 제공자에 바인딩하지 않으며, 접근성 권한을 요청하지 않습니다. 모델 호출과 기기 동작은 AutoJs6가 연결된 링크 하나를 위해 빌려주고 분리 시 회수하는 브로커를 통해 이루어지며, 각 브로커는 grant (허용 메서드, 속도, 크기, 모델 할당량)로 제한됩니다.
- INTERNET은 GitHub 릴리스 수동 확인에만 사용합니다. FOREGROUND_SERVICE와 FOREGROUND_SERVICE_SPECIAL_USE는 실행 중인 작업을, POST_NOTIFICATIONS는 진행 및 확인을 지원합니다. SYSTEM_ALERT_WINDOW는 설정에서 플로팅 볼을 켤 때만 요청합니다. 접근성, 저장소 또는 마이크 권한을 요청하지 않습니다.
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
minimum host build: 5289 (6.8.0)
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

_2026/09/25_

- `힌트` 1.0.0 개발 미리 보기입니다. 작업 API와 화면 진입점이 구현되었고 P7 감사 증거가 기록되었습니다. P8 출시 검사와 공개는 아직 완료되지 않았습니다. 통과 사례와 알려진 제한은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)를 참고하세요.
- `힌트` Android 7+, 작업 API용 AutoJs6 6.8.0 / build 5293+, 모델을 설정하고 활성화한 3-Stone AI가 필요합니다. OCR은 선택 사항입니다. 연결 프로토콜만의 최소 호스트는 build 5289+입니다.
- `기능` 자연어 작업 화면의 질문, 진행, 중지 및 결과와 선택적 플로팅 입력, 텍스트 공유, 프리셋 바로가기 및 음성 초안
- `기능` ai.agent API의 작업 생성, 이벤트, 조회, 응답 및 취소, detached 작업과 등록 스크립트 결과/문맥 접근
- `기능` project.json / @agent 등록 스크립트 검색, 매개변수 검증 및 기본값, 누락 값 질문, 승인, 제한된 실행 및 구조화된 결과
- `기능` 텍스트 노드와 선택적 승인 OCR을 통한 화면 관찰, 참조 기반 클릭, 입력, 스크롤 및 키 동작과 화면 변화/완료 증거 확인
- `기능` 모델 자격 증명 저장 없이 AutoJs6를 통한 온라인 및 로컬 모델 접근; 선택한 대상이 없으면 다른 모델로 자동 전환하지 않고 실패 보고
- `기능` 단계, 모델 호출, 시간 및 token 예산, 도구 기한, 단계당 최대 두 번의 결정 수정 재시도와 반복되는 무효 동작 방지
- `기능` 모델, 문맥, 도구 그룹, 예산, 신중 모드, 스크립트 폴더 및 기억 범위를 위한 명명된 프리셋과 전역 설정; gesture/files/shell 기본 비활성화
- `기능` 범위별 선호 기억의 제안/가져오기 개별 승인, 편집, 삭제 및 JSON 백업, 최대 500개 / 256 KiB; 자동 주입 최대 4 KiB
- `기능` 작업 상세 및 타임라인, 필터, 재실행 초안 및 민감 정보 제거 JSON 내보내기, 개인 기록 최대 200개 작업 / 32 MiB
- `기능` 작업 화면, 알림 및 플로팅 카드의 위험별 승인; 결제와 기억은 항상 개별 승인, 호스트 단절 시 작업 차단 및 프로세스 재시작 후 자동 재개 금지
- `기능` 10개 언어 설정, 오프라인 출시 기록 및 법적 고지; 취소, 일일 캐시 및 버전 무시를 지원하는 수동 GitHub 업데이트 확인, APK 자동 다운로드 없음
- `수정` 양식과 필터의 터치 영역, 선택 항목과 스크립트 매개변수 열의 줄바꿈, 큰 글꼴 및 Android 7 에서의 플로팅 컨트롤 배치
- `수정` 기본 설정 메모리의 전각 문자, 너비가 0인 문자 및 일부 인증 정보 이름으로 인증 정보 검사를 우회할 수 있는 문제
- `수정` 보안 잠금이 없는 기기를 깨울 때 화면 상태가 안정되기 전에 판단하여 작업 플로팅 볼이 다시 표시되지 않던 문제
- `수정` 플러그인 프로세스 종료로 중단된 작업을 재시작 후 실패로 기록하고, 잠금 화면 감지 후 화면 작업을 중지
- `수정` 파일 도구는 확인이나 호스트 호출 전에 경로 탐색, 절대 경로, 잘못된 작업 경로를 거부; 작업 기록에는 거부된 모델 본문 없이 제한된 거부 분류만 저장
- `수정` 알림 확인 후 대상 앱으로 돌아간 다음 작업을 재개하고, 화면이 중지되어도 응답을 처리하며, 플로팅 응답을 제출하면 카드를 접어 포커스를 해제
- `수정` Android 13에서 창 뷰 생성 전에 시스템 표시줄 컨트롤러를 읽어 앱 시작 시 충돌하던 문제
- `수정` 최근 기록을 작업 시작 시간으로 정렬하고 보존하여 재시작 시 파일 갱신으로 새 작업이 제거되는 문제 방지
- `수정` 질문 및 확인 응답의 interaction 소유권을 검사하여 스크립트가 플러그인 화면을 대신해 응답하지 않도록 처리
- `수정` 거래 확인 버튼이 결제 동작으로 인식되지 않는 문제, 이제 매번 확인이 필요하며 작업 전체 권한을 재사용할 수 없음
- `수정` 화면 밖 항목의 비어 있거나 뒤집힌 경계를 인수 오류로 처리하지 않고 텍스트를 유지하며 좌표 사용 불가로 표시
- `수정` 노드 재탐색 시 경계 또는 동작 기능이 다른 중첩 컨테이너를 같은 대상으로 잘못 인식하는 문제
- `수정` 노드 대상 수정 안내에서 # 참조 접두사 유지와 selector 사용 시 snapshotId 생략을 명시
- `수정` 작업 접수 시 주문 의도 규칙을 미리 로드하고 규칙 초기화 비용을 줄임
- `수정` 다른 창의 동일 노드를 구별하고 클립보드 읽기 후에도 화면 관찰 요구를 유지하며 파일 전송을 결제로 오인하지 않음
- `수정` 작업 후 화면 읽기가 응답하지 않을 때 안정화 대기 기한을 초과하는 문제
- `수정` 콘솔 줄 분리와 자르기 전에 여러 줄 매개변수를 숨기고 매개변수 텍스트가 자격 증명 레이블과 같을 때의 누락 방지
- `수정` 종료 중인 포그라운드 서비스가 다음 작업의 시작 요청을 잘못 거부하는 문제
- `개선` 긴 작업 기록을 줄일 때 변경되지 않은 프롬프트와 관찰 조각을 재사용하여 단계별 처리 시간 단축
- `개선` 확인 설명에 JSON 이스케이프 후 크기를 반영하여 큰 매개변수 표도 Binder 이벤트 제한 내에 유지
- `개선` 작업 노드 검사와 확인을 실행에 연결하기 위한 최소 호스트는 AutoJs6 6.8.0 / 빌드 5289
- `의존성` 동일한 AutoJs6 6.8.0 / 5289 release 빌드의 common-plugin-api, host-capability-api 및 ai-agent-api (MPL 2.0) 추가, SHA-256으로 고정
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

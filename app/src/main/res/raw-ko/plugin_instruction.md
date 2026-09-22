AI Agent는 자연어 목표를 AutoJs6가 실행되는 Android 기기의 실제 동작으로 바꿉니다. 사용자가 에이전트용으로 등록한 스크립트를 골라 매개변수를 채우고 실행하거나, 접근성 노드 트리로 화면을 관찰하고 관찰, 결정, 실행, 검증의 순환으로 단계별로 조작합니다. 목표를 달성하거나 확인이 필요하거나 예산이 소진될 때까지 계속됩니다. [AutoJs6 토론 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577)에 대한 답입니다.

버전 1.0.0 의 플러그인 실행 부분은 P0 개발 미리보기입니다: INFO, Wake Activity, `org.autojs.plugin.AI_AGENT` 임시 서비스와 호스트 상태 시작 화면을 제공합니다. 호스트에는 P1 계약, 브로커, 화면 관찰, 등록 스크립트 실행, 서랍 및 플러그인 센터 진입점을 구현했습니다. 플러그인의 에이전트 루프와 스크립트 선택, `ai.agent` API 및 작업 화면은 후속 단계에 남아 있습니다. AutoJs6 빌드 5285 이상이 필요합니다. 진행 상황과 검증 기록은 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md) 를 참조하세요.

### 사용 방법

1. AutoJs6 빌드 5285 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `AI Agent`가 인식되는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. 런처 또는 AutoJs6 서랍의 관리 기능에서 AI Agent 를 여세요. 이 미리보기는 호스트 상태만 표시합니다. 작업 화면과 `ai.agent` API 는 후속 단계에서 제공합니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)를 참고하세요.

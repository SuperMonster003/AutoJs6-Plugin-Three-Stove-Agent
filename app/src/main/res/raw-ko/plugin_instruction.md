AI Agent는 자연어 목표를 AutoJs6가 실행되는 Android 기기의 실제 동작으로 바꿉니다. 사용자가 에이전트용으로 등록한 스크립트를 골라 매개변수를 채우고 실행하거나, 접근성 노드 트리로 화면을 관찰하고 관찰, 결정, 실행, 검증의 순환으로 단계별로 조작합니다. 목표를 달성하거나 확인이 필요하거나 예산이 소진될 때까지 계속됩니다. [AutoJs6 토론 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577)에 대한 답입니다.

개발 미리보기: 작업 화면에서 목표 입력, 진행 상황, 인라인 응답 및 최근 작업 상세 정보를 지원합니다. ai.agent 작업 API에는 AutoJs6 빌드 5293 이상이 필요합니다. 기록 관리, 사용자 프리셋 및 기타 진입점은 P6에서, 안정성 검증은 P7에서 계속됩니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 사용 방법

1. AutoJs6 빌드 5289 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `AI Agent`가 인식되는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. AI Agent를 열고 AutoJs6에 연결한 다음 목표를 입력하고 기본 프리셋으로 시작하세요. 작업 카드에서 응답하거나 작업을 확인하고 최근 작업에서 상세 정보를 확인하세요.
4. 런처의 "스크립트 디렉터리"에서 추가 폴더를 설정하고 줄마다 절대 경로를 하나씩 입력하세요. 저장한 경로는 호스트가 검증하여 적용하며 작업은 승인된 폴더 범위만 좁힐 수 있습니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)를 참고하세요.

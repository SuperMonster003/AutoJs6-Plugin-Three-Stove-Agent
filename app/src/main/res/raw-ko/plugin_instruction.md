AI Agent는 자연어 목표를 AutoJs6가 실행되는 Android 기기의 실제 동작으로 바꿉니다. 사용자가 에이전트용으로 등록한 스크립트를 골라 매개변수를 채우고 실행하거나, 접근성 노드 트리로 화면을 관찰하고 관찰, 결정, 실행, 검증의 순환으로 단계별로 조작합니다. 목표를 달성하거나 확인이 필요하거나 예산이 소진될 때까지 계속됩니다. [AutoJs6 토론 #577](https://github.com/SuperMonster003/AutoJs6/discussions/577)에 대한 답입니다.

설치된 미리보기는 호스트 상태만 표시합니다. P1 호스트 인터페이스와 P2.1-P2.4 도구, 결정, 실행기, 컨텍스트, 모델 클라이언트 코어의 구현과 테스트가 완료되었습니다. 실제 실행에는 P2.5 Binder/포그라운드 서비스 연동과 P3/P4 실행 어댑터가 필요합니다. 스크립트 API와 작업 화면은 P5/P6에서 구현합니다. [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md).

### 사용 방법

1. AutoJs6 빌드 5285 이상이 설치된 기기에 [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/releases)에서 플러그인 APK를 설치합니다.
2. AutoJs6 플러그인 센터를 열어 `AI Agent`가 인식되는지 확인하고 활성화합니다. 공식 릴리스 패키지는 서명 검증을 자동으로 통과합니다.
3. 런처 또는 AutoJs6 서랍의 관리 기능에서 AI Agent 를 여세요. 이 미리보기는 호스트 상태만 표시합니다. 작업 화면과 `ai.agent` API 는 후속 단계에서 제공합니다.

연결 안내와 현재 진행 상황은 [프로젝트 README](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent)와 [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-AI-Agent/blob/master/ROADMAP.md)를 참고하세요.

# Task C-5: Cucumber 인수테스트 + Testcontainers (포트와 어댑터가 인수테스트를 만나는 지점)

(Grit's Why): C-3에서 쓴 Gherkin이 여기서 실제로 돌아갑니다. 그리고 그 인수테스트가 'AI가 짠 코드가 맞는지'를 판정하는 결정론 게이트(1-1)와 같은 역할을 환경 위에서 합니다.

### 수행 내용

1. C-3의 Feature에 Cucumber-JVM Step Definition을 붙이고, 헥사고날 Core를 통과시켜 인수테스트를 초록불로 만드세요. Happy Path뿐 아니라 Unhappy Path(경계·실패·거절)도 포함하세요.
2. Testcontainers로 실제 DB(+ 외부 의존) 컨테이너를 띄워 통합 검증하세요. CI Runner에서도 동일하게 돌도록 GitHub Actions 워크플로를 구성하세요.
3. 포트/어댑터가 인수테스트와 만나는 지점을 설명하세요. 인수테스트는 Inbound Port를 호출하고, Outbound Adapter는 Testcontainers의 실제 인프라로 대체된다는 흐름을 그리세요.
4. 이제 어댑터를 실제로 갈아끼워 Core의 독립성을 증명해 보세요. Outbound Adapter(예: Testcontainers로 띄운 DB)를 다른 구현(인메모리 Fake나 다른 저장소)으로 교체하고, Core 코드는 한 줄도 바꾸지 않은 채 C-3에서 쓴 인수테스트가 그대로 GREEN인지 확인하세요. 어댑터를 바꾸려고 Core를 건드려야 했다면, 의존 방향이 안쪽을 향하지 않고 새고 있는 것입니다. 어디를 고쳐야 Core를 안 건드리고 교체되는지 적어 주세요.

### 제출물

- [ ]  Step Definition + 통과하는 도메인/어댑터 코드 + Testcontainers 설정을 GitHub에.
- [ ]  CI에서 인수테스트가 초록불인 실행 결과(GitHub Actions 로그).
- [ ]  'Unhappy Path를 먼저 떠올린 과정' + 포트/어댑터-인수테스트 접점 설명. (최소 400자)
- [ ]  어댑터 교체 전후(무엇을 무엇으로 바꿨는지)와 Core 무수정 증빙(Core diff 없음 + 인수테스트 GREEN 로그). Core를 건드려야 했다면 그 원인과 수정.

# Task C-6: Walking Skeleton + AI 파이프라인

(Grit's Why): 이것이 이번 Station의 진짜 산출물입니다. C-3에서 설계하고 C-5에서 Port 레벨 인수테스트를 초록불로 만들었다면, C-6은 마지막 간극을 닫습니다. 실제 Inbound Adapter(컨트롤러)까지 붙여 HTTP부터 DB까지 진짜 한 줄기로 통하는, 끝에서 끝까지 가장 얇게 도는 walking skeleton을 만들고, 그 골격 생성을 AI 파이프라인으로 돌립니다.

### 수행 내용

1. 인수테스트 1개가 통과하는 최소 end-to-end 골격(walking skeleton)을 세우세요. Inbound Adapter(예: 컨트롤러) → Inbound Port → Core → Outbound Port → Outbound Adapter(Testcontainers DB)까지 한 줄기가 실제로 도는 상태.
2. 이 골격 생성을 AI에게 맡기되, 1-1/1-2의 하네스를 확장한 파이프라인으로 운영하세요. agent.md/CLAUDE.md에 헥사고날 컨벤션(포트/어댑터 규약, 의존성 방향)을 컨텍스트로 주고, Layer 단위로 지시·검수(Iterative Prompting)하세요. 매 단계 Cucumber 인수테스트로 결과를 판정합니다.
3. AI가 만든 골격을 본인이 검수한 기록(수용/기각 + 이유)을 남기세요. 헥사고날 경계를 어긴 제안(예: Core가 Adapter를 직접 의존)을 잡아낸 사례가 있으면 적으세요.

### 제출물

- [ ]  walking skeleton 코드(1 인수테스트 초록불, 1-command 기동)를 GitHub에.
- [ ]  AI 파이프라인(프롬프트/컨텍스트/agent.md)과 Layer 단위 검수 로그.
- [ ]  AI 제안 중 헥사고날 경계 위반을 잡아낸 사례 + 본인 판단. (위반이 없었다면, 검수 시 적용한 경계 점검과 AI 출력이 경계를 지킨 이유를 적으세요.) (최소 300자)

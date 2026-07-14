# Task B-5: 리팩토링 실전 (테스트 안전망 위에서)

(Grit's Why): 설계를 머리로 아는 것과 살아있는 코드를 안전하게 옮기는 것은 다릅니다. 여기서 1-1의 테스트가 안전망으로 일합니다. (역사 그릿 표현: '돌아가는 쓰레기를 예쁜 코드로 바꿀 시간. 단, 테스트가 통과된 상태에서만.')

### 수행 내용

1. (안전망 먼저) 리팩토링 전 원본 코드 상태에서 동작을 고정하는 특성화 테스트를 1-1 방식으로 깔고, 그 테스트가 GREEN인 커밋을 가장 먼저 남깁니다(커밋 해시 명시). 안전망 없이는 구조를 건드리지 않습니다.
2. (대상) B-2의 kata(TicketService) 또는 조건을 충족하는 본인 코드. 본인 코드는 특성화 테스트로 기존 동작을 고정할 수 있는(외부 I/O가 경계지어진) 코드여야 하고, 안 되면 kata를 씁니다.
3. (리팩토링) 안전망 위에서 B-3(Rich Domain) + B-4(SOLID) 설계 방향으로 옮깁니다. 최소 4~5개 커밋으로 쪼개고, 각 커밋은 한 번에 하나의 기법(메서드 추출 / 의존성 주입 / 인터페이스 도출 / 도메인으로 행위 이동 등)만 담고 파울러 카탈로그 기법명을 커밋 메시지에 적습니다. 매 커밋 테스트 GREEN 유지. 행위는 보존되고 구조만 바뀝니다.
4. '리팩토링이 필요한 경우 vs 불필요한 경우' 기준을 세웁니다. 판단 도구는 파울러 코드 스멜 카탈로그 + 비용/효용 저울입니다. (멘토 프레임 '재즈 3단계': 모방=카탈로그 기법 그대로 따라하기, 응용=상황에 맞게 변형, 자기화=언제 리팩토링을 '안 할지'까지 판단.)
5. 새 요구사항 하나를 가정해 설계가 변화에 강한지 시험해 보세요. (예: 할인 정책을 한 종류 더 추가하거나, 알림 채널을 하나 더 붙이기.) 먼저 그 변경이 어느 파일이나 클래스를 건드릴지 예측해 적어두고, 그다음 실제로 적용해 보세요. 변경이 예측한 한두 곳에만 갇히면 설계가 변화에 강한 것이고, 여기저기 번지면 아직 결합이 덜 끊긴 것입니다. 번졌다면 어디서 새는지와 어떻게 끊을지 한 줄 적어 주세요.

### 제출물

- [x]  리팩토링 시작 전 특성화 테스트 GREEN 증빙(커밋 해시 + 실행 로그).
    - 안전망 커밋(리팩토링 전, quirk 포함 현재 동작 고정): [`f97c1a7`](https://github.com/woong7361/architecture-thinking/commit/f97c1a7) — 특성화 인수테스트 6 시나리오 GREEN.
    - 실행 로그 + C0~C6 매 커밋 GREEN 재검증: [task5-history/refactoring-report.md](https://github.com/woong7361/architecture-thinking/blob/main/task2/task5-history/refactoring-report.md) (JDK17 실행법 + 각 커밋 `6 Scenarios (6 passed)`)
- [x]  리팩토링 전/후 코드 + 단계별 커밋 히스토리(기법명 커밋 메시지 + 매 단계 테스트 그린 유지)를 GitHub에.
    - 코드 디렉터리: [task2/task5-history](https://github.com/woong7361/architecture-thinking/tree/main/task2/task5-history)
    - 단계별 커밋 히스토리(C0→C6, 파울러 기법명 커밋 메시지): [compare `f97c1a7…b69d858`](https://github.com/woong7361/architecture-thinking/compare/f97c1a7...b69d858)
    - 커밋 로그 표(기법·스멜·해시·GREEN): [refactoring-log.md](https://github.com/woong7361/architecture-thinking/blob/main/task2/task5-history/refactoring-log.md) · 부분별 before/after 상세: [refactoring-report.md](https://github.com/woong7361/architecture-thinking/blob/main/task2/task5-history/refactoring-report.md) · 실행 전략: [refactoring-strategy.md](https://github.com/woong7361/architecture-thinking/blob/main/task2/task5-history/refactoring-strategy.md)
- [x]  '리팩토링이 필요한 경우 vs 불필요한 경우' 본인 기준. (최소 300자)
    - 답안: [task5-history/refactoring-criteria.md](../task5-history/refactoring-criteria.md) — 1부 스멜 탐지 카탈로그 + 2부 결정 게이트(GO=필요 / DEFER·LEAVE=불필요·보류 / REMOVE=과구조)
- [ ]  가정한 새 요구사항 1개, 변경 전 예측(어느 파일을 고칠지), 실제로 바뀐 파일 목록. 예측과 어긋났거나 변경이 번졌다면 그 원인.
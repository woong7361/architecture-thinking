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
- [x]  가정한 새 요구사항 1개, 변경 전 예측(어느 파일을 고칠지), 실제로 바뀐 파일 목록. 예측과 어긋났거나 변경이 번졌다면 그 원인.
    - 답안: [task5-history/change-resilience-test.md](https://github.com/woong7361/architecture-thinking/blob/main/task2/task5-history/change-resilience-test.md) — 예측 잠금(`88301ab`) 후 R1(할인/가격 축, 대조군)·R2(판매중지/예약 규칙 축, 실험군) 적용. 예측 정확히 일치. **저항력은 끊은 축에서만**: R2는 `TicketService` 0곳 수정(리팩토링 전이면 서비스 열림), R1은 baseline과 동일(가격 축 미개입).
    - **결과**: 두 요구를 넣고 예측과 대조하니, 둘 다 예측이 정확히 맞았고 변경이 번지지 않았다(각 production 2곳). 그러나 성격은 갈렸다 — R2(판매중지, 예약 규칙 축)는 새 규칙이 `Ticket.ensureReservable()` **한 곳**에 떨어져 중심 코드인 `TicketService`를 **0곳** 고쳤고 `new Ticket(...).suspend()`로 **mock 0개** 단위 검증이 됐다. 반면 R1(할인, 가격 축)은 `TicketService`의 금액 계산 라인을 고쳐야 했고, 이건 **리팩토링 전이었어도 똑같은 위치·개수**였다.
    - **이유**: 저항력은 **내가 실제로 끊은 축에서만** 생기기 때문이다. 리팩토링으로 예약 규칙을 `Ticket`으로 모았으니 그 축의 변경(R2)만 한 곳에 갇혔다. 가격 축은 애초에 끊은 적이 없어(결제=I/O=서비스 몫) R1은 baseline과 같았다. 그래서 "고친 파일 몇 곳"만 세면 둘 다 2곳이라 같아 보이지만, **서비스가 열리는가 / mock 없이 검증되는가**를 함께 봐야 진짜 차이가 드러난다.
    - **결론**: 정리한 축(예약 규칙)은 변경이 한 곳에 갇혀 리팩토링이 값을 했다. 안 정리한 축(가격)은 리팩토링의 실패가 아니라 **의도한 트레이드오프**다 — "모든 결합을 끊어라"와 "미리 추상화하지 마라(YAGNI)"는 반대로 당기고, 할인이 1종류인 지금 그 축을 미리 끊는 건 과설계(YAGNI 위반)이므로 일부러 남겨뒀다. 게다가 지금은 새지도 않으며(2곳에 갇힘), "샌다"는 할인이 2종류로 늘 때의 미래 얘기다. 그때 `DiscountPolicy` 인터페이스를 뽑아 그 축도 끊는다는 트리거만 예약해뒀다.
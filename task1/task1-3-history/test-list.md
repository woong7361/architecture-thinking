# 환불 비용 계산 (REFUND) — 테스트 목록 & 전략

> **범위 정정(사이클13 이후)**: 이 문서의 산출물은 **환불 비용 계산**이다. "일할계산(proration)"은
> 그 안의 한 갈래(무료환불 기간이 아닐 때의 계산 규칙)일 뿐이라, 애초 제목이 하위 규칙을 전체로 오인하게
> 했다. 진입점은 `RefundCalculator.calculate`(최종 환불액 반환), 일할계산은 그것이 위임하는 내부 순수계산
> `Proration.calculate`다. 코드는 이미 이 구조라 이번 정정은 문서(제목·시그니처)만 손댄다.

> **이 문서의 목적**: 세션이 끊겨도 이어갈 수 있는 캐리 문서.
> Kent Beck의 *테스트 목록(test list)* 처럼 **살아있는 to-do**로 굴린다 —
> 케이스를 끝내면 `[x]` 체크, 새 케이스가 떠오르면 목록 맨 아래에 추가한다.
> 규율(Red→Green→Refactor, Mock 기준, 기록법)은 상위 [CLAUDE.md](./CLAUDE.md)를 따르고,
> 사이클별 실행 서사는 [tdd-log.md](./tdd-log.md)에 쌓인다. 이 문서는 **앞으로 할 일 + 고정된 결정**만 담는다.

---

## 확정된 도메인 규칙 (locked)

한 번 정한 계약. 바꾸면 이 절부터 고치고 그 아래 목록을 재정렬한다.

- **시그니처(공개 진입점)**: `int RefundCalculator.calculate(int price, int totalDays, int elapsedDays)`
  → 최종 **환불액**을 반환한다. 내부의 일할계산은 `int Proration.calculate(int price, int totalDays, int elapsedDays)`가 담당(정책이 전액환불이 아닐 때 위임받는 순수계산). 두 클래스 시그니처는 동일하고, 계약의 무게중심이 `Proration`(하위 규칙)에서 `RefundCalculator`(진입점)로 옮겨온 것이다.
- **남은 일수는 파라미터가 아니다** → 내부에서 `remaining = totalDays - elapsedDays`
- **환불 정책 분기 (7일 무료환불)**: `elapsedDays ≤ 7` → **무료(전액) 환불 = price**. `elapsedDays ≥ 8` → **일할계산**.
  - `7일 이하`는 7 **포함**(≤ 7). 전자상거래 청약철회처럼 초기엔 사용량과 무관하게 전액 환불.
  - **정책보다 입력 검증(예외)이 먼저다**: elapsed>total, 음수 등은 7일 룰 판정 *전에* 예외로 던진다.
  - (설계 노트, 사이클7~8 Refactor에서 실행됨) 이 분기는 일할계산 *위*의 정책 레이어다. `Proration`은
    정책을 모르는 순수 계산 클래스, `RefundCalculator`(사이클7 최초 이름 `RefundPolicy`, 사이클8 리네임)가
    별도 클래스로 "elapsed≤7 전액 여부"를 판정하고 아니면 `Proration.calculate`에 위임한다.
    사이클6의 private 메서드 분리는 가독성 정리였을 뿐 책임 분리가 아니었다고 판단해(SRP) 클래스 경계로
    다시 갈랐고, `RefundPolicy`라는 이름이 "정책 판정만 함"으로 오해될 수 있어 최종 환불액을 반환하는
    진입점임을 드러내도록 `RefundCalculator`로 리네임했다.
- **버림 우선**: 일단가를 원 단위로 먼저 버림한 뒤 곱한다
  `일단가 = floor(price / totalDays)`, `결과 = 일단가 × remaining`
  (전체 계산 후 버림이 아니다. 예: 10000/30/20 → floor(10000/30)=333 → 333×10 = **3330**)
- **Mock 없음**: 순수 로직(seam 안쪽)이라 진짜 실행한다. Mock이 끼면 seam을 잘못 그은 신호.

---

## 테스트 목록 (Kent Beck 스타일 백로그)

> **한 사이클에 목록 하나만** Red로 밀어넣는다. 미리 다 구현하지 않는다.
> `[x]` = Green까지 통과 완료. 옆에 사이클 번호를 적어 [tdd-log.md](./tdd-log.md)와 대조한다.

- [x] **1. 절반 사용** — 30000 / 30 / 15 → 15000  *(happy)* — 사이클1 **Green 통과**(`return 15000;` 하드코딩, 삼각측량 대기)
- [x] **2. 다른 비율** — 30000 / 30 / 20 → 10000  *(삼각측량 성공: 1번 하드코딩을 깨서 `dailyRate×remaining` 일반식 창발, 사이클2 Green)*
- [x] **3. 전액 소진 (경계)** — 30000 / 30 / 30 → 0  *(remaining = 0, 사이클3: Red 없이 즉시 Green — 일반식이 자연히 처리, 회귀 안전망으로 등록)*
- [x] **4. 미사용 (경계)** — 30000 / 30 / 0 → 30000  *(⚠️ 무료환불 구간이라 값이 30000으로 같음 — 정책을 **못 가른다**. 정책 판별은 9번이 담당. 사이클4: Red 없이 즉시 Green)*
- [x] **5. 안 나눠떨어짐 (버림 규칙 고정)** — 10000 / 30 / 20 → 3330  *(일단가 먼저 버림, elapsed 20 > 7 → 일할계산 구간. 사이클5: Red 없이 즉시 Green, 결함주입 후보5의 핵심 회귀테스트로 등록)*
- [x] **6. 총일수 0 (예외)** — price / **0** / elapsed → 예외 throw  *(사이클12: Red(`0/0`이 예외 없이 price 반환 — 정책 지름길 탓에 ArithmeticException조차 안 남) → Green(`validateInputs()`에 `totalDays==0` 가드를 범위 가드 앞단). elapsed=0이 total=0을 통과시키는 유일한 값)*
- [x] **7. 경과일수 범위 위반 (예외)** — elapsed > total, 또는 elapsed < 0 → 예외 throw  *(사이클11: Red 2행(elapsed=40, -1 둘 다 예외 없이 샘) → Green(`elapsed<0 || elapsed>total` 복합 가드) → Refactor(검증 가드 2개를 private `validateInputs()`로 그룹핑). 2점이 복합 조건 강제)*
- [x] **8. 음수 금액 (예외)** — price < 0 → 예외 throw  *(사이클10: Red(예외 부재로 단언 실패) → Green(`RefundCalculator`에 `price<0` 가드를 정책 분기 앞단 추가). 예외 타입=`IllegalArgumentException`, 가드 위치=정책보다 앞단 확정)*
- [x] **9. 무료환불 상한 (정책 경계·핵심)** — 30000 / 30 / **7** → 30000  *(≤7 전액. 일할계산이면 23000이라 이 케이스가 7일 규칙을 **강제**한다. 사이클6: Red(23000) 확인 → Green(정책 분기 추가))*
- [x] **10. 일할계산 전환 (정책 경계)** — 30000 / 30 / **8** → 22000  *(≥8 첫 일할 값. 9와 straddle pair. 사이클9: Red 없이 즉시 Green — 기존 `elapsed<=7` 분기+`Proration` 위임이 자연히 처리)*

*(새 경계·예외가 떠오르면 여기에 11, 12 … 으로 추가)*

---

## 진행 전략 (seam & 삼각측량)

- **순서**: happy(1~2)로 일할 일반식을 세운 뒤 → 경계(3~5) → **정책 분기(9~10)** → 예외(6~8)로 확장.
- **삼각측량**: 예제가 하나뿐일 땐 하드코딩으로 Green, **다음 예제가 그걸 깨뜨릴 때** 일반식으로 넘어간다.
  - 1번 Green = `return 15000;` (하드코딩)
  - 2번 Red 가 하드코딩을 깨뜨림 → `일단가 × remaining` 일반식 창발
  - **9번 Red(elapsed=7→30000)가 일할 일반식을 깨뜨림** → `if (elapsed ≤ 7) return price;` 정책 분기 창발
- **Refactor 방향**: 테스트와 코드에 박힌 중복(하드코딩 기대값)을 지우며 `dailyRate` 추출.
  정책 분기가 자리잡으면 `일할계산`과 `정책 판정`을 별 메서드/클래스로 분리(seam)할지 검토.
  `Money` 값객체는 이 흐름에서 필요가 드러날 때만 도입.
- **예외 처리 위치**: 6~8은 가드 절(guard clause)로 **정책 분기보다 앞단**에서 던진다.
  어떤 예외 타입을 쓸지는 6번 Red를 쓸 때 단언으로 먼저 못박는다 (예: `IllegalArgumentException`).

---

## 고의 결함 후보 (수행내용 4번 — 나중에)

통과 후 일부러 틀려서 테스트가 Red가 되는지 확인할 지점:

- **5번**: 버림을 반올림으로 바꾸거나, `floor` 제거 → 3330 이 3333/3334로 → 잡히는가?
- **3번**: 경계 부등호 `elapsed >= total` 를 `>` 로 뒤집기 → remaining=0 케이스가 새는가?
- **9번(정책 경계)**: `elapsed <= 7` 을 `< 7` 로 바꾸기 → elapsed=7이 일할계산(23000)으로 새는가? (9번이 30000을 단언하므로 잡혀야 정상)
- **일반식**: `일단가 × remaining` 의 `×`를 `+`로, `remaining` 을 `elapsed`로 바꾸기.

→ 결과는 [tdd-log.md](./tdd-log.md) 하단 "고의 결함 주입" 표에 기록.

---

## 세션 재개 방법 (환경)

- **JDK**: `~/.jdks/corretto-17.0.7` (JAVA_HOME). 전역 `mvn`·Java 24 없음 — 17로 진행.
- **실행**: `cd task1/task1-3-history && ./mvnw test`
  - 전역 Maven 설치 없음. wrapper(`mvnw`)가 Maven 3.9.6을 자체 부트스트랩.
  - `JAVA_HOME` 이 corretto-17로 잡혀 있어야 함.
- **현재 상태 대조**: [tdd-log.md](./tdd-log.md) 표(사이클별 R/G/R) + 커밋 접두어 `[Red]`/`[Green]`/`[Refactor]`.

### 지금 상태 (스냅샷)

- 빌드 골격·wrapper·의존성 다운로드 정상.
- **대상 A(환불 비용 계산) 사이클 13까지 완료 — 체크리스트 1~10번 전부 Green**(사이클13은 코드 리뷰 발견 Refactor: 스테일 Javadoc 링크 수정). `Proration`(순수 계산)/`RefundCalculator`(정책 판정+조합+검증) 클래스 분리 완료(사이클7~8 SRP 리팩터). `RefundCalculator.validateInputs()`에 입력 가드 3종 완비: 음수 금액(사이클10)·경과일수 범위(사이클11)·총일수 0(사이클12).
- **예외 타입 관례 확정(사이클10~12)**: 입력검증 예외 = `IllegalArgumentException`, 가드 위치 = `RefundCalculator.validateInputs()`(정책 분기 **앞단**). 총일수 0은 `ArithmeticException`이 아니라 명시적 `IllegalArgumentException`으로 선제 차단(정책 지름길 때문에 나눗셈에 도달조차 안 하므로 명시 가드가 필수임이 Red에서 드러남).
- **다음 할 일**: 대상 A 예외까지 완료 → (a) **수행내용 4번 고의 결함 주입**(아래 "고의 결함 후보" 절 실행, 회귀 안전망이 진짜 결함을 잡는지 확인), (b) **대상 B(Order 상태 전이)**, (c) **대상 C(Facade+Mockito)**. `Validator` 클래스 승격은 검증 불변식의 독립 변화 압력이 없어 계속 미룸(사이클11~12 판단).

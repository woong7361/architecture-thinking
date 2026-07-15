# 공유 참조 — 코드 스멜 · SOLID · 파울러 기법 매핑

> **이 스킬은 공용이다. 이 참조는 특정 코드베이스·과제에 묶이지 않는다** — 어떤 SOLID 리팩토링 대상에도 그대로 쓴다.
> Gen(찾기)·Critique(누락·과설계 검사)·Eval(채점)이 **같은 표**를 봐야 진단이 결정적이 된다(rubric 공유와 같은 논리).
>
> 1·2부 = 파울러 『리팩토링 2판』 3장 스멜 카탈로그 + 비용/효율 결정 게이트. 3부 = SOLID 매핑 레이어.
> **특정 target의 골든 진단·비교 같은 과제별 데이터는 이 참조에 넣지 않는다 — run 입력으로 공급한다**(스킬 자립·공용성 유지).

---

## 1부. 코드 스멜 탐지 표 (Type 분류 + 신호)

### Type 범례 (게이트가 종류마다 다르다)

- **A = 재배치.** 기존 개념 사이에서 행위를 옮기거나(Move) 추출·개명·캡슐화. **새 추상화 축을 안 만든다 → 간접비 ≈ 0.**
  게이트가 싸다: *지금 만지는 코드에 있고 조금이라도 이득이면 GO.*
- **B = 추상화 도입.** 변형 축을 따라 다형성/인터페이스/포트를 **새로 만든다 → 간접비 있음.**
  게이트가 비싸다: *변형이 ≥2개 있거나 확정 요구가 있을 때만 GO (YAGNI 카운트).*
- **C = 과(過)구조 신호.** 근거 없는 추상화·불필요한 계층이 **이미 있음** → 만들지 말거나 되돌린다(inline·collapse).

### 표 — 파울러 『리팩토링 2판』 3장 코드 스멜 전체(24)

| # | 스멜 | Type | 탐지 신호 (코드에서 보는 것) | 대응 기법(파울러) |
|---|------|:---:|------------------------------|------------------|
| 1 | **Mysterious Name** | A | 이름만으로 역할·반환을 모름; 이름과 실제 동작 불일치 | Change Function Declaration, Rename Variable/Field |
| 2 | **Duplicated Code** | A | 구조가 같은 코드 조각이 ≥2곳(복붙 흔적) | Extract Function, Pull Up Method, Slide Statements |
| 3 | **Long Function** | A | 한 메서드가 여러 단계(조회→검증→외부호출→저장)를 순차로; 단계 주석(`// 1.`), 20줄↑, 추상수준 혼재 | Extract Function, Decompose Conditional |
| 4 | **Long Parameter List** | A | 파라미터 3~4개↑, 늘 같이 몰려다니는 값들 | Introduce Parameter Object, Preserve Whole Object |
| 5 | **Global Data** | A | 어디서나 접근·수정되는 전역/정적 가변 상태 | Encapsulate Variable |
| 6 | **Mutable Data** | A | public setter로 외부가 아무 때나 상태 변경 → 불변식 못 지킴 | Encapsulate Variable, Remove Setting Method |
| 7 | **Divergent Change** | A | 한 클래스가 **서로 다른 이유**로 바뀜(책임 축 여럿) | Extract Class, Split Phase |
| 8 | **Shotgun Surgery** | A | 하나의 변경이 **여러 파일·메서드**를 동시에 건드림(책임 흩어짐) | Move Function/Field, Combine Functions into Class |
| 9 | **Feature Envy** | A | 메서드가 자기 필드보다 **다른 객체의 getter/setter**를 더 만짐; 남 데이터로 결정·변경 | Move Function, Extract Function |
| 10 | **Data Clumps** | A | 늘 같이 다니는 필드/파라미터 뭉치 | Extract Class, Introduce Parameter Object, Preserve Whole Object |
| 11 | **Primitive Obsession** | A (→B) | 도메인 개념을 원시타입으로(돈=int, 식별자=String), 의미·검증 흩어짐 | Replace Primitive with Object, Replace Type Code with Subclasses *(후자 B)* |
| 12 | **Repeated Switches** | **B** | **같은 타입코드 switch/if-else가 여러 곳 반복.** 새 케이스마다 모든 분기 수정 | Replace Conditional with Polymorphism (Strategy) |
| 13 | **Loops** | A | 명령형 루프로 필터·변환·집계를 직접 | Replace Loop with Pipeline |
| 14 | **Lazy Element** | **C** | 하는 일 없는 클래스/함수(위임만·한 줄 래퍼), 불필요한 계층 | Inline Function, Inline Class, Collapse Hierarchy |
| 15 | **Speculative Generality** | **C** | 사용처 1개뿐 인터페이스/추상, "언젠가"용 미사용 파라미터·훅, 구현 1개 전략 | Collapse Hierarchy, Inline, Remove Dead Code — **또는 안 만듦(YAGNI)** |
| 16 | **Temporary Field** | A | 특정 상황에서만 값이 차는 필드(평소 null/미사용) | Extract Class, Introduce Special Case |
| 17 | **Message Chains** | A | `a.getB().getC().getD()` 연쇄 호출 | Hide Delegate, Extract Function |
| 18 | **Middle Man** | **C** | 클래스 메서드 대부분이 다른 객체로 **위임만** | Remove Middle Man, Inline Function |
| 19 | **Insider Trading** | A | 모듈끼리 내부를 과하게 주고받음(강결합), 서로 private에 손댐 | Move Function/Field, Hide Delegate |
| 20 | **Large Class** | A | 필드·메서드 많고 책임 여럿; 접두사로 묶인 필드 그룹 | Extract Class, Extract Superclass |
| 21 | **Alternative Classes w/ Different Interfaces** | **B** | 같은 일을 하는 클래스들이 이름·시그니처만 다름 | Change Function Declaration + Extract Superclass |
| 22 | **Data Class (Anemic)** | A | 필드 + getter/setter만, 도메인 메서드 0개. 규칙이 클래스 **밖**에서 벌어짐 | Move Function, Encapsulate Record, Remove Setting Method |
| 23 | **Refused Bequest** | A | 서브클래스가 상속받은 것 다수를 안 씀/거부(오버라이드해 예외) | Push Down Method/Field, Replace Subclass with Delegate |
| 24 | **Comments** | A | 코드가 *무엇을 하는지* 설명하는 주석(냄새 탈취제) | Extract Function(이름으로 설명), Introduce Assertion |

> 사용법: target 코드를 이 표에 대조해 **매칭되는 행만** 후보로 올린다(매칭 안 되면 후보 아님).
> 스코프는 **지금 변경이 만지는 코드**로 제한(전체 스캔·폭주 금지).

---

## 2부. 비용/효율 결정 게이트

후보 스멜(1부)을 **할지·미룰지·둘지·없앨지**로 바꾸는 결정적 절차. 입력 = `(변경 목표 1개, target 코드)`.
목표 없이 코드만 스캔해 리팩토링하지 않는다(Fowler: 리팩토링은 변경에 묶인다).

```
스코프 필터: 변경 목표가 만지는 코드인가? ── 아니오 → LEAVE (범위 밖)
   │ 예
   ▼
1부 표로 A/B/C 분류
   ├─ A → GO      (탐지=이득, 망 있음, 간접비≈0. 한 커밋 한 기법으로 시퀀싱)
   ├─ B → v = (기존 구체 변형 수) + (확정 예정 변형 수)
   │        v ≥ 2 → GO   /   v < 2 → DEFER (여는 트리거 기록)
   └─ C → REMOVE (inline·collapse). '새로 만들 추상화'가 C면 = B의 v<2 → 안 만듦
```

- **확정 = 과제·요구에 명시된 것만.** "언젠가·혹시·아마"는 v 카운트에 **불인정**.
- 출력: **GO**(지금) / **DEFER**(미룸+여는 트리거 기록) / **LEAVE**(범위 밖) / **REMOVE**(과구조 제거).
- 시퀀싱(의존 순서): ①행위 이동·추출 → ②재배선 → ③제거·캡슐화 → ④개명·정리. 한 커밋 한 기법, 매 커밋 GREEN.

---

## 3부. SOLID 매핑 레이어 (B-6 신규)

기존 표엔 스멜↔기법은 있으나 스멜↔SOLID가 없다. 이 레이어가 각 위반에 **원칙명**을 붙인다.
task2 규칙 준수: **DIP=의존의 *방향* ≠ ISP=추상의 *너비*** 를 한 예로 뭉개지 않는다.
**OCP 추상화는 그 축이 실제로 늘어남을 아는 지금에만 정당** — 2부 B-게이트(v≥2)에 묶어 YAGNI를 병기한다.

| SOLID | 정의(task2 규칙) | 신호 스멜(#) | 추상화 축? | YAGNI 게이트 |
|---|---|---|:---:|---|
| **SRP** | 변경 이유 하나 | #7 Divergent, #20 Large, #3 Long(결정+I/O 혼재) | A | 탐지=GO |
| **OCP** | 확장 열림/수정 닫힘 | #12 Repeated Switches, #21 Alt Classes | **B** | **v≥2일 때만 GO** |
| **DIP** | 의존 *방향*(벤더 구체 아니라 내가 통제하는 추상에) | 벤더 구체 직접의존, #1 | A/B | 포트 1개=A(개명) / 벤더축=B |
| **ISP** | 추상의 *너비*(역할 단위로 좁힘) | 뚱뚱한 인터페이스(안 쓰는 메서드 강제 구현) | B | 역할 분리 요구 있을 때 |
| **LSP** | 치환 가능성(자식이 부모 계약 안 깨기) | #23 Refused Bequest | A | 탐지=GO |

- **과설계 탐지가 결정적이 된다:** 과설계 = **Type B인데 v<2인데 GO로 낸 제안의 개수.** 감이 아니라 술어.
- Critique는 이 술어로 검사하고, Eval은 `change_minimality`/과설계 신호로 채점한다.

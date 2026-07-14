# 리팩토링 판단 기준 — task2 B-5 (#4)

판단 절차는 두 단계다: **① 코드 스멜 카탈로그로 후보를 추린다 → ② 비용/효율 게이트로 할지 말지 결정한다.**
이 문서의 1부는 **①의 탐지 표**(AI가 target에서 스멜을 잡게)이고, 2부는 **②의 결정 게이트**다.
AI에게 판단을 맡기므로, 모든 칸은 "예뻐 보이나"가 아니라 **관찰 가능한 신호·셀 수 있는 수**로만 쓴다.

---

## 1부. 코드 스멜 탐지 표 (Type 분류 + 신호 + 예시)

> **이 표는 target 무관 재사용 카탈로그다.** 특정 코드에 묶지 않으므로 다른 프로젝트·파이프라인에서 그대로 입력으로 쓸 수 있다.

### Type 범례 (왜 나누나 — 게이트가 종류마다 다르기 때문)

- **A = 재배치.** 기존 개념 사이에서 행위를 옮기거나(Move) 추출·개명·캡슐화한다. **새 추상화 축을 안 만든다 → 간접비용 ≈ 0.**
  게이트가 싸다: *지금 만지는 코드에 있고 조금이라도 이득이면 GO.*
- **B = 추상화 도입.** 변형 축을 따라 다형성/인터페이스/포트를 **새로 만든다 → 간접비용 있음.**
  게이트가 비싸다: *변형이 ≥2개 있거나 확정 요구가 있을 때만 GO (YAGNI 카운트).*
- **C = 과(過)구조 신호(추상화를 새로 만드는 B의 반대).** 근거 없는 추상화·불필요한 계층이 **이미 있음** → 만들지 말거나 되돌린다(inline·collapse).

> 즉 A는 거의 "만질 때 같이 치운다", 비싼 판단은 B의 YAGNI 카운트 한 곳에 몰려 있다.

### 표 — 파울러 『리팩토링 2판』 3장 코드 스멜 전체(24)

| # | 스멜 | Type | 탐지 신호 (코드에서 보는 것) | 예시(일반형) | 대응 기법(파울러) |
|---|------|:---:|------------------------------|------|------------------|
| 1 | **Mysterious Name** | A | 이름만으로 역할·반환을 모름; 이름과 실제 동작 불일치 | `getX()`가 사실 값을 계산·저장까지 함; `tmp`·`data2` 같은 무의미 이름 | Change Function Declaration, Rename Variable/Field |
| 2 | **Duplicated Code** | A | 구조가 같은 코드 조각이 ≥2곳(복붙 흔적) | 두 메서드에 같은 검증·계산 블록이 복붙 | Extract Function, Pull Up Method, Slide Statements |
| 3 | **Long Function** | A | 한 메서드가 여러 단계(조회→검증→외부호출→저장)를 순차로 담음. 단계 주석(`// 1.`), 20줄↑, 추상수준 혼재 | 한 메서드에 조회+검증+외부호출+저장이 한 몸에 | Extract Function, Decompose Conditional |
| 4 | **Long Parameter List** | A | 파라미터 3~4개↑, 늘 같이 몰려다니는 값들 | `f(a, b, c, d)` — 특히 함께 다니는 값들 | Introduce Parameter Object, Preserve Whole Object |
| 5 | **Global Data** | A | 어디서나 접근·수정되는 전역/정적 가변 상태 | `public static` 가변 설정값, 가변 싱글턴 상태 | Encapsulate Variable |
| 6 | **Mutable Data** | A | public setter로 외부가 아무 때나 상태 변경 → 불변식 못 지킴 | 외부에서 `obj.setStatus(...)`를 아무 때나 호출 | Encapsulate Variable, Remove Setting Method |
| 7 | **Divergent Change** | A | 한 클래스가 **서로 다른 이유**로 바뀜(책임 축 여럿). `git log`가 여러 주제 | 한 Service가 조회방식·외부연동·업무규칙·저장방식 여러 이유로 바뀜 | Extract Class, Split Phase |
| 8 | **Shotgun Surgery** | A | 하나의 변경이 **여러 파일·메서드**를 동시에 건드림(책임 흩어짐). Divergent Change의 반대 | 필드 하나 추가에 DTO·매퍼·검증·화면 여러 곳 동시 수정 | Move Function/Field, Combine Functions into Class |
| 9 | **Feature Envy** | A | 메서드가 자기 필드보다 **다른 객체의 getter/setter**를 더 만짐; 남 데이터로 결정·변경 | 메서드가 `other.getA()+other.getB()`로 계산하고 `other.setX()`로 변경 | Move Function, Extract Function |
| 10 | **Data Clumps** | A | 늘 같이 다니는 필드/파라미터 뭉치 | `startDate, endDate`(또는 `x, y`)가 여러 시그니처에 늘 함께 | Extract Class, Introduce Parameter Object, Preserve Whole Object |
| 11 | **Primitive Obsession** | A (→B) | 도메인 개념을 원시타입으로(돈=int, 식별자·정보=String), 의미·검증 흩어짐 | 전화번호=String, 금액=int(음수 가능), 상태=int 코드 | Replace Primitive with Object, Replace Type Code with Subclasses *(후자는 B)* |
| 12 | **Repeated Switches** | **B** | **같은 타입코드 switch/if-else가 여러 곳 반복.** 새 케이스마다 모든 분기 수정 | `switch(type){A.. B..}`가 여러 메서드·파일에 반복 | Replace Conditional with Polymorphism (Strategy) |
| 13 | **Loops** | A | 명령형 루프로 필터·변환·집계를 직접 | `for`로 리스트를 걸러 변환·합산 | Replace Loop with Pipeline |
| 14 | **Lazy Element** | **C** | 하는 일 없는 클래스/함수(위임만·한 줄 래퍼), 불필요한 계층 | 필드 하나 감싼 클래스, 본문 한 줄 함수 | Inline Function, Inline Class, Collapse Hierarchy |
| 15 | **Speculative Generality** | **C** | 사용처 1개뿐 인터페이스/추상, "언젠가"용 미사용 파라미터·훅, 구현 1개 전략 | 구현 1개뿐인 인터페이스, 안 쓰는 확장 파라미터·훅 | Collapse Hierarchy, Inline Function, Inline Class, Change Function Declaration(미사용 파라미터 제거), Remove Dead Code — **또는 안 만듦(YAGNI)** |
| 16 | **Temporary Field** | A | 특정 상황에서만 값이 차는 필드(평소 null/미사용) | 특정 메서드 실행 중에만 채워지는 필드 | Extract Class, Introduce Special Case |
| 17 | **Message Chains** | A | `a.getB().getC().getD()` 연쇄 호출 | `order.getCustomer().getAddress().getCity()` | Hide Delegate, Extract Function |
| 18 | **Middle Man** | **C** | 클래스 메서드 대부분이 다른 객체로 **위임만** | 실질 로직 없이 `delegate.doX()` 나열 | Remove Middle Man, Inline Function |
| 19 | **Insider Trading** | A | 모듈끼리 내부를 과하게 주고받음(강결합), 서로 private에 손댐 | 두 클래스가 서로의 내부 상태를 계속 참조 | Move Function/Field, Hide Delegate |
| 20 | **Large Class** | A | 필드·메서드 많고 책임 여럿; 접두사로 묶인 필드 그룹 | 필드·메서드 과다, 접두사(`orderXxx`, `paymentXxx`)로 묶인 그룹 | Extract Class, Extract Superclass |
| 21 | **Alternative Classes w/ Different Interfaces** | **B** | 같은 일을 하는 클래스들이 이름·시그니처만 다름 | 같은 역할인데 `charge()` vs `pay()`처럼 시그니처만 다른 클래스들 | Change Function Declaration + Extract Superclass *(변형 이미 ≥2 → YAGNI 자동 통과)* |
| 22 | **Data Class (Anemic)** | A | 필드 + getter/setter만, 도메인 메서드 0개. 규칙이 클래스 **밖**에서 벌어짐 | 필드+get/set만 있고 행위 메서드 없는 클래스 | Move Function, Encapsulate Record, Remove Setting Method |
| 23 | **Refused Bequest** | A | 서브클래스가 상속받은 것 다수를 안 씀/거부(오버라이드해 예외) | 자식이 부모 메서드 대부분을 오버라이드해 무력화 | Push Down Method/Field, Replace Subclass with Delegate |
| 24 | **Comments** | A | 코드가 *무엇을 하는지* 설명하는 주석(냄새 탈취제) | `// 1. 조회` `// 2. 검증` 같은 단계 설명 주석 | Extract Function(이름으로 설명), Introduce Assertion |

> 사용법: target 코드를 이 표에 대조해 **매칭되는 행만** 후보로 올린다(매칭 안 되면 후보 아님). 스코프는 **지금 변경이 만지는 코드**로 제한한다(전체 스캔·폭주 금지).

---

## 2부. 비용/효율 결정 게이트

후보 스멜(1부)을 **할지·미룰지·둘지·없앨지**로 바꾸는 결정적 절차. "저울질"을 셀 수 있는 술어로 옮겨, AI가 돌려도 같은 입력이면 같은 판정이 나오게 한다.

### 원칙 3

1. **게이트는 "주어진 변경"에 대해 돈다.** 입력 = `(변경 목표 1개, target 코드)`. 목표가 있어야 "경로 위인가"·"확정 요구인가"가 판정된다(Fowler: 리팩토링은 변경에 묶인다). 목표 없이 코드만 스캔해 리팩토링하지 않는다.
2. **"효율(benefit)"은 별도 술어가 아니라 1부의 탐지 신호 그 자체다.** 코드가 탐지 신호에 걸림 = 이미 이득의 증거. 그래서 A는 별도 이득 계산이 없다.
3. **주관은 B의 카운트 한 곳에만.** 테스트 망은 상수(항상 깔림)로 계산에서 뺀다.

### 결정 트리 (후보 스멜마다)

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

### 결정적 술어 표

| 게이트 | 술어 (셀 수 있음) | 임계/정의 | 출력 |
|---|---|---|---|
| 스코프 | 스멜 위치 ∈ 변경이 만지는 파일/메서드? | — | 밖이면 **LEAVE** |
| A | 1부 탐지 신호에 매칭? | — | **GO** |
| B | v = 기존 구체 변형 + 확정 예정 변형 | **v ≥ 2 → GO / v < 2 → DEFER**. **확정 = 과제·요구에 명시된 것만**("언젠가·혹시·아마"는 불인정) | GO / DEFER |
| C | C행에 매칭? | — | **REMOVE** (없거나 안 만듦) |

### 출력 상태

- **GO** — 지금 리팩토링. 한 커밋 한 기법, 매 커밋 GREEN.
- **DEFER** — 미룸 + **여는 트리거를 기록**한다(예: "v가 2 되는 요구가 확정될 때").
- **LEAVE** — 이번 변경 범위 밖. 건드리지 않는다.
- **REMOVE** — 과구조(C)를 없앤다(inline·collapse). 아직 안 만든 추상화면 = "안 만듦".

### 시퀀싱 — 의존 순서 (규칙)

게이트는 "무엇을(GO/DEFER/LEAVE/REMOVE)"을, 시퀀싱은 "순서를" 정한다. 순서 기준은 **의존성**이다: 리팩토링 X가 Y의 산출(새 메서드·클래스, 사라진 호출자 등)을 전제로 하면 **Y가 먼저**다. 이는 취향이 아니라 안전 제약이다 — 전제가 안 선 채로 하면 매 커밋 GREEN이 깨진다.

의존성이 실제로 만드는 **결정적 기본 순서(4단계)**. GO를 기법별로 이 단계에 배치하고 위→아래로 실행한다:

1. **행위 이동·추출** (Move Function, Extract Function/Class) — 새 집을 만든다. 뒤 단계가 이걸 딛는다.
2. **재배선** (호출부를 새 메서드로 교체) — 1의 산출에 의존.
3. **제거·캡슐화** (Remove Setting Method, Inline Function/Class, 죽은 코드·중복 주석 삭제) — 2로 옛 호출자가 사라진 **뒤에야** 안전.
4. **개명·정리** (Rename, tidy) — 최종 구조에 의존, 맨 뒤.

- 각 단계는 **한 커밋 한 기법, 커밋마다 GREEN**. 4단계는 의존성의 결정적 프록시이며, **구체 의존 간선이 기본 순서와 어긋나면 의존성이 이긴다**(간선이 마스터, 4단계는 기본형).
- **A/B/C(비용 분류)는 순서가 아니다.** B는 순서상 뒤가 아니라 **트리거가 열릴 때**(대개 이 배치엔 DEFER), C는 있으면 **이른 제거**(1단계 전에 길 치우기)이거나 "안 만듦" 가드다.

---

> **요약**: 필요 여부 = ① 스코프(만지는 코드만) → ② A/B/C 분류(1부) → ③ 게이트(A: 탐지=GO / B: v≥2 카운트 / C: 제거). 테스트 망은 상수로 빠지고, 주관은 B의 "확정 예정 변형" 인정 한 곳에만 남아 결정적으로 돈다.

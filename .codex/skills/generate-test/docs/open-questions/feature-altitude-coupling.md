# 열린 질문 — feature(계약)가 구현 세부에 종속되는가

> 상태: **열림(토론 중).** 잠정 결론(합격 규칙)까지 도달했으나, **하네스를 실제로 돌려 실증**한 뒤
> closed로 옮긴다. 지금 닫지 않는 이유: 아래 "5 합격 규칙"이 실제 산출물에서 성립하는지 미검증.
> 자매 문서: [unit-test-lock-in.md](unit-test-lock-in.md) (안쪽 단위 lock-in).
> 관련 실측: [../phase4-split-vs-bundled.md](../phase4-split-vs-bundled.md),
> 레퍼런스 정답: `task2/task5-history/`의 `ticket_reservation.feature` + `TicketReservationSteps.java`.

## 질문 (다듬어진 최종형)

feature가 **요구사항**에 종속되는 건 당연하고 사람이 검토한다. 진짜 질문은:
**시스템(하네스)이 뽑는 feature 레벨 인수테스트 "코드"가 "구현 세부"에 종속돼도 되는가?**
리팩터링의 **결정적 최종 게이트**로 쓰려면 게이트가 구현에 종속되면 안 된다(리팩터링=게이트 파괴가 되니까).
인터페이스 변경은 불가피하다 치고, **그 외에는 구체에 종속되지 않는 인수테스트가 가능한가?**

---

## 배경 신호 (하네스에서 나온 근거)

### 신호 A — ticket 계약은 고도 축에서 3회 반려 → FAILED
`runs/contract-standalone/ticket-char/2026-07-13_57796ebc/57796ebc_failed.json`:
iter1 behavioral_altitude 3, iter2 2.5, iter3 3 (< min_axis 3.5) → max_iteration FAILED.
rubric이 "구현 세부 한 곳만 노출돼도 최대 3" 캡인데 게이트가 3.5 → **단일 누출도 하드 실패.**

### 신호 B — refund 계약은 고도 유지하며 PASS
`refund.feature`는 `환불금액`·`환불유형`·`주문상태`·`도메인오류` 등 정책 어휘로만 서술 → contract 4.3 PASS.

### 신호 A의 진짜 원인 — 입력이 계약이 아니라 "특성화 요청"이었다
`pipeline/inputs/57796ebc_input.json`을 열어보니 ticket run의 입력은 **정책이 아니었다**:
- `requirement`: "리팩토링 전 TicketService.reserveTicket의 **현재 동작을 있는 그대로 고정하는 특성화 테스트**"
- `source_material` = **Java 코드 전문**, `policy_rules` = 구현 사실("charge는 save보다 먼저"),
  `constraints.must_include` = **"클래스명 TicketService, 메서드 reserveTicket(...) 그대로 사용"** (구현 세부 노출을 *요구*)
- → 입력이 구현 세부 노출을 **요구**하는데 contract rubric은 그걸 **금지**. **구조적으로 보장된 FAILED.**
- 결정적 대비: **같은 ticket 입력**에 contract는 FAILED, unit은 PASS.

---

## 토론 전개 (빠짐없이)

### 1. 1차 재프레임 — "종속=결함"은 job에 따라 뒤집힌다
"feature가 구현에 종속되는가"의 표면 프레임이 틀렸다. **종속이 결함인지 아닌지는 입력 job에 달렸다:**

| 입력 job | 예 | 구현 종속은 | altitude 축은 |
|---|---|---|---|
| **정책 고정**(policy) | refund | 결함 ✅ | 옳게 작동 |
| **동작 특성화**(characterization) | ticket | **요구사항 그 자체** | **틀린 걸 잡는다** |

특성화 테스트는 리팩터링 안전망이라 구현을 붙잡는 게 존재 이유. 하네스엔 "job" 개념이 없어 contract 모드/altitude 게이트를 무차별 적용 → 특성화 입력엔 보장된 FAILED.
- **폐기된 가설**: "정책형 도메인 vs 절차형 도메인" 이분법(초기 추측)은 **틀렸다.** 축은 도메인 형태가 아니라 **입력 의도(정책 고정 vs 특성화)**. ticket 도메인도 정책 계약("결제 성공 시 예약")은 충분히 쓸 수 있다.

### 2. 사용자 정정 — 진짜 질문은 "게이트 자격"
feature가 요구사항에 종속되는 건 당연. 문제는 **시스템이 뽑는 test 코드가 구현 세부에 종속되면 리팩터링 최종 게이트가 못 된다**는 것. + "인터페이스를 어디까지 봐야 하나".

### 3. task2 레퍼런스 정답 — 계층 분리
`task2/task5-history/`의 손으로 만든 인수테스트가 정답 형태:
- **인터페이스 경계 = 유스케이스 경계**(`reserveTicket`). 내부(도메인 객체 내부·메서드 쪼개기·호출 순서)는 관찰 안 함.
- **외부만 대역, 그것도 mock이 아니라 fake/recording double.** repo=in-memory fake, PG=recording double. `verify`가 아니라 **상태**를 읽음.
- **구현 세부 종속(quirk: NPE, 보상 없음)은 feature가 아니라 step으로 내림.** feature 문장은 도메인 언어 유지.
- `change-resilience-test.md`가 실증: 리팩터링(Rich Domain+SOLID) 후 R2(판매중지) 추가에 **`TicketService` 0곳 수정**, net 안 깨짐.

대비표(같은 사실 "결제 청구됨"):

| | task2 인수테스트 (게이트 적합) | 하네스 unit 산출물 |
|---|---|---|
| 대역 | in-memory **fake** / recording double | `@Mock` 3개 |
| "결제됨" 단언 | `paymentApi.wasCharged()` (**상태**) | `verify(paymentApi).charge(...)` (**상호작용**) |
| 순서 | 단언 안 함(의도적) | `InOrder`(charge→save), `verifyNoMoreInteractions` |
| 리팩터링 시 | **안 깨짐** | **깨짐** |

### 4. A/B 종속 구분 (핵심 합의)
"세부 종속"은 두 종류. **종속 0은 불가능**(그럼 아무것도 검증 못 함). 목표는 제거가 아니라 **B→A 이동**.

| | 종속 대상 | 리팩터링에 | 정당한가 |
|---|---|---|---|
| **A** | 관찰 가능한 도메인 상태(예약됐나·얼마 청구·거부 사유) | 안 깨짐 | **필요하고 옳다 = 요구사항** |
| **B** | 구현 메커니즘(호출 순서·어느 협력자·NPE 타입·`userId==0L`) | 깨짐 | 게이트엔 안 됨 |

task2도 quirk 시나리오는 B 종속이나, **요구사항이 아닌 시나리오에만 가둠**(특성화 net, 리팩터링이 quirk 고치면 삭제/재작성).

### 5. "결과 상태 확인도 종속 아닌가?" — 관찰 표면 문제
맞다, 종속이다. 관건은 *무엇을 관찰 표면으로 삼느냐*. 스펙트럼(종속 큰 순):
리플렉션 → 임의 getter → **포트로 되읽기** → **유스케이스 반환값** → **외부 관찰 효과**(뒤 3개가 A).
- 결정적 판별 테스트: **"구현을 바꾸되 도메인 동작이 같으면 이 단언이 통과하는가?"**
- 예: `getUserId()==0L`(B, Optional로 바꾸면 깨짐) vs `isReserved()==false`(A). **같은 사실을 도메인 어휘로 다시 쓰면 B→A.**
- **특수 라이브러리 필요? 아니오.** 종속 바닥은 의미론적이라 라이브러리로 못 없앤다. 필요한 건 **아키텍처(포트+fake)**. (골든마스터/승인 라이브러리는 A가 아니라 B=특성화에 어울림.)

### 6. "테스트가 필요없는 관찰점/포트를 만들어내지 않나?"
두 위험: **test-induced design damage**(관찰용 getter 강요) / **test-only 포트**(mock 꽂으려 판 seam).
- 판별자: **그 seam/쿼리에 production 클라이언트나 변경 축이 있는가?**
  - `isReserved()`는 production이 이미 씀(reserveTicket이 검사) → test-induced 아님.
  - `getUserId()` 되읽기는 회색 — "내 예매 조회" 유스케이스가 있으면 정당, 없으면 누수. **단일 테스트만으론 못 정함, 유스케이스 집합을 봐야 함.**
- 포트 두 종류: **실제 I/O 경계**(repo·PG, 변경 축 有 → fake) vs **도메인 중간 mock seam**(축 無 → 과설계, 실제 객체 써라). task2 CLAUDE.md YAGNI/DIP 규칙과 일치.

### 7. 사용자 핵심 문제 제기 — "인터페이스 외 구체 종속 0이 가능한가?"
step 계층 종속 표면 분해: ① 진입점 시그니처(=인터페이스, 수용) / ② 사전상태 셋업(유스케이스로 셋업하면 ①로 흡수) / ③ 결과 관찰(쟁점).
③은 α(경계 관찰 가능 → 해결) vs β(내부에만 보임 → 시스템 안 바꾸면 미해결)로 갈림.
- 정직한 딜레마: β→α로 바꾸려 쿼리를 추가하면 그 자체가 test-induced일 수 있음 → 때로 불가피.
- (이 시점 내 과한 결론: "게이트 자격은 시스템 설계가 쥐어서 test-first 생성은 구조적 불가" ← **다음 단계에서 정정됨**.)

### 8. 정정 — outside-in에서는 테스트가 관찰가능성을 "강제"한다
사용자 반박: test로 먼저 설계할 수 있고, 그게 task1 파이프라인(도메인 요구사항 → 인수테스트)이다.
→ **정정 채택**: outside-in 인수테스트는 관찰가능성 설계를 *기다리는* 게 아니라 *강제*한다. 경계 언어로만 단언하면 구현은 그 결과를 경계에서 관찰 가능하게 만들 수밖에 없다 → **β를 테스트가 원천 배제.** "구조적 불가"는 과했다 → **"구조적 미달"**로 정정.

### 9. outside-in의 원형 — 이중 루프
- 바깥 루프: 슬라이스마다 인수테스트 1개를 먼저(경계 언어).
- 안쪽 루프: 구현하며 단위테스트로 설계 창발(mock=역할 발견 도구).
- **모든 인수테스트를 미리 쓰지 않고, 내부 설계를 미리 하지 않는다**(발견됨).
- 하네스는 "정책 → 계약 전체 + 단위 전체 일괄 생성, 구현·안쪽 루프·창발 없음" = **specification by example**이지 full outside-in 아님.
- 함의: **바깥(인수 계약) 먼저 생성은 정당**(실행 가능하면). **구현 전 단위 생성은 방법론적 오배치**(→ 고민 2 lock-in).

### 10. "When ↔ Inbound Port 1:1" — 게이트 자격의 검사기
과제 요구: "핵심 유스케이스 하나를 Gherkin으로. When 절이 **Inbound Port와 1:1 매칭**. 이 Feature는 C-5에서 **실제로 실행**." → 이게 "구동 포트로만 진입" 규칙의 구체·검사 가능한 형태.
- **먼저 있어야 하는 실체 = Inbound Port(경계 계약)이지 구현이 아니다.**
  - 리팩터링(C-5): 실체 = 기존 코드의 진입점(이미 있음). feature는 그 포트에 1:1로 묶인 안전망.
  - greenfield: feature가 포트를 정의, 구현은 뒤따라 C-5에 green.
- 1:1 깨짐(When이 내부 service/절차로 내려감) = 구체 종속 실패.

### 11. Inbound / Outbound Port 정의
- **Inbound(구동) 포트**: 바깥이 앱을 부르는 진입점 = 유스케이스. 예: `ReserveTicketUseCase.reserve` / `TicketService.reserveTicket`. When이 이걸 1:1로 호출.
- **Outbound(구동되는) 포트**: 앱이 부르는 외부. 예: `TicketRepository`·`UserRepository`·`PaymentApi`. **테스트에서 fake로 두는 건 이쪽.**

### 12. "outbound port로 확인"의 함정 — 상태 vs 상호작용
"inbound 하나 호출 + 반환값 or outbound port로 확인"은 거의 맞으나 **결정적 단서**:
- **상태 기반**(`paymentDouble.wasCharged()`, `lastAmount()`, `findById().isReserved()`) → A, 안 깨짐.
- **상호작용 기반**(`verify`, `InOrder`, 호출 횟수) → B, 깨짐. ← 하네스 unit이 틀린 지점.
- 규칙: **outbound는 결과 상태만. verify/순서/횟수 금지.**
- 확인 우선순위: **반환값 > outbound fake의 결과 상태 > (절대 금지) 상호작용.**

---

## 잠정 합격 규칙 (열림 — 실증 전)

feature 인수테스트가 **구현 비종속 결정적 게이트**가 되기 위한 5규칙:

1. **When = Inbound Port 1:1 호출** (내부 service/절차를 직접 조립하지 않음).
2. **구동되는(outbound) 포트만 fake/double** (도메인은 절대 mock 안 함).
3. **반환값 또는 outbound fake의 "결과 상태"로만 단언** (`verify`·`InOrder`·호출 횟수 금지).
4. **결과는 도메인 어휘로** (`isReserved()`, `userId==0L` 금지).
5. **feature 텍스트는 도메인 언어, 구현 세부(quirk 등)는 step 계층으로 격리.**

> 도달한 성질: 이 규칙을 지키면 "구현 종속 0"은 아니지만 **"구현 비종속(내부를 어떻게 짰든 안 깨짐)"** 은 달성된다.
> 남는 종속 3개는 전부 A(계약): ① inbound 시그니처(=인터페이스) ② outbound 포트 계약의 존재 ③ 도메인 어휘.
> 깨진다면 그건 도메인 계약이 바뀐 것이라 깨지는 게 맞다.

---

## 하네스 현재 상태 vs 합격 규칙 (실측)

`find`·`grep` 확인 결과:
- **contract 모드**: `refund.feature` **텍스트 한 개만** 산출. step/glue/fake **없음**(파이프라인 전체에 그 개념 자체가 없음). → **실행 불가 = 게이트로 못 돌림.** "When ↔ Inbound Port 1:1" 확인할 대상조차 없음.
- **unit 모드**: 실행되는 `.java`를 내지만 `@Mock`+`verify`+`InOrder` = 규칙 2·3 정반대.
- **bundled**: `.feature` + `.java` 단위, 서로 연결 안 됨(step definition 아님).

→ 결론: **"구조적 불가"가 아니라 "구조적 미달".** test-first 인수테스트 생성은 정당·가능하나,
하네스는 **경계-전용 실행 가능 인수테스트(feature+step+fake 3종 세트)를 산출할 계층을 아예 안 만든다.**
task1 최종 산출물이 "요구사항 → 실행 가능한 인수 게이트"를 지향한다면, 지금은 그 게이트를 못 만들고 있다.

---

## 남은 열린 쟁점 (닫지 않는 이유)

1. **5규칙을 하네스가 실제로 산출할 수 있는가** — contract 모드가 feature뿐 아니라 step+fake까지,
   그리고 "When=Inbound Port 1:1"을 강제하도록 만들 수 있는지 **실증 필요**(사용자가 skill 복사해 직접 테스트 예정).
2. **Inbound Port를 하네스가 어디서 아는가** — greenfield면 feature가 정의(사람 소유 게이트), 리팩터링이면 기존 코드에서. 입력 계약에 어떻게 실을지.
3. **A/B 경계의 회색지대** — `wasCharged()`(호출됐다는 사실)는 A/B 경계. 어디까지 상태로 볼지.
4. **특성화 job과의 관계** — job=특성화면 B 종속이 정답. 하네스가 job(정책고정 vs 특성화)을 입력에서 구분해야 하는가(→ 고민 1·2 공통).

---

## 실측 로그 (generate-test-v1)

### 실측 1 (2026-07-21) — contract 첫 run: 게이트는 옳게 작동, Gen이 분업 위반
- 입력: 환불 정책(f1ba346d). 결과 **FAILED(max_iteration=3)**, codex 정상(303s).
- 점수 시소: iter1 3.55(cov3·bf4·un3·alt4) → iter2 3.5(cov5·**bf2**·un4·**alt2**) → iter3 3.9(cov5·bf3·un4·alt3).
  refine이 coverage를 3→5로 올리자 boundary_fidelity·altitude가 무너짐 → **위 토론의 "coverage↔altitude 긴장"이 실측으로 재현.**
- **게이트(축)는 설계대로 정확히 작동**: boundary_fidelity가 *"When이 내부 연산('…금액을 계산하면/…가능 여부를 검증하면/…유형을 결정하면')이라 유스케이스(inbound port) 1:1이 아님 + 원시 누적값 단언"* 을 잡음. behavioral_altitude가 *"하루기준금액·UTC 시각·산식이 feature 본문에 섞임"* 을 잡음.
- **실패 원인은 Gen**: coverage 압력에 밀려 **rules 문서에 가야 할 산식·내부연산 시나리오를 feature에 끌어옴.** 그 "계산하면" 시나리오가 문자 그대로 rules 내용 → **두 문서 분리가 옳다는 실증.**
- 조치: gen_contract에 강한 금지 추가(내부연산 When 금지·산식/중간값/UTC 금지·coverage는 유스케이스 경계만) 후 재실행(실측 2).

### 실측 3 (2026-07-21) — critique 분리 + coverage 좁힘: 시소 해소, 새 병목 unambiguity
- 조치: 공유 critique가 contract draft에 "산식 표·UTC 넣어라"(rules용 조언)를 쏜 게 실측 2의 원인 → **critique를 모드별로 분리**(critique_contract/critique_rules) + **contract coverage를 유스케이스 경계 행동만 세도록 좁힘**(내부 산술 제외).
- 결과 FAILED(max_iter)이나 **큰 진전**: iter2에서 **cov5·bf4·alt4 동시 달성**(실측 1·2의 coverage↔boundary/altitude 시소 **해소**). total 4.1로 0.1 부족.
- 남은 병목 = **unambiguity 3**(min 3.5): eval 근거 *"'부분 환불이 되는 금액'·'남은 금액보다 작은 금액' 등 느슨한 결과 표현"*. **원인: "구체 금액을 rules로 미뤄라"를 과적용**해 feature가 결과 금액까지 모호해짐.
- 재조정: **결과 값(환불 금액 15000원)은 feature에 구체적으로**, 산식·중간값(일할단가)만 rules로 — 분업선을 "결과 vs 파생"으로 정정. (cov5/bf4/un4/alt4=4.3 ≥ 4.2 예상.)
- 또한 iter2→iter3 refine 퇴행(bf4→3·alt4→2) 재확인 → **refine 분리는 다음 후보 레버**.

### 실측 4 (2026-07-21) — 분업선 정정(결과=구체/산식=rules): unambiguity 해결, 이번엔 altitude 붕괴 → refine 범인 확정
- 결과 FAILED. iter3 cov5·bf4·**un4(해결)**·**alt2.5**=4.0. 분업선 정정("결과 값은 feature 구체, 산식만 rules")으로 unambiguity 3→4.
- altitude 2.5 원인(eval): *"22000/2331 같은 일할 산출값 검증이 여러 곳에 섞여 contract feature보다 rules 예시표 성격이 강함."* → **refine이 unambiguity 쫓다 일할 계산 사례를 feature에 다시 끌어옴.**
- **4개 run 관통 결론**: 시소·병목이 옮겨 다닌 근본 원인 = **공유·mode-blind refine.** weak_axis만 쫓고 passing axis(altitude/bf)를 보호하지 않아 매번 퇴행. 유일한 미수정 content-shaping 스테이지.
- 다음 레버: **refine 분리(refine_contract)** — boundary 규율 + "passing axis 보호(일할 계산 사례 추가 금지)". (또는 게이트 노이즈 고려해 min_total 소폭 완화.)
- 현 상태: 게이트 미달이나 **feature 콘텐츠 품질은 양호**(cov5/bf4/un4). 사람이 소유해 다듬을 수준.

### 실측 5 (2026-07-21) — refine 분리(passing axis 보호): 급붕괴 제거, 그러나 게이트가 병목으로 확정
- gen·critique·refine 모두 모드별 분리 완료(eval만 공유). 결과 FAILED. iter1 3.5(cov4·bf3·un3·alt4) → iter2 3.5(cov4·bf3·un4·alt3) → iter3 3.6(cov4·bf4·un3·alt3).
- refine 분리로 alt 2.5 같은 급붕괴는 사라졌으나 **매 iter 한 축이 3에 머물고 회전** — 4축 동시 ≥floor 달성 실패.
- **5개 run 관통 최종 진단**: 콘텐츠 생성은 안정적 양호(cov 4-5, 4축 중 3개가 4). 남은 병목은 **생성이 아니라 게이트**다 — min_total 4.2 + 네 floor를 3-iter LLM이 동시 충족 못 하고, **eval 노이즈**(동일 콘텐츠 altitude 4↔2.5)까지 겹침. 최고 4.1.
- **결론**: 이 지점부터 프롬프트 조임은 노이즈 대비 수익 체감. 다음은 생성이 아니라 (a) max-iterations 상향, (b) 게이트 완화(min_total 4.0 등), (c) **설계 원칙("사람이 명세 소유")대로 최고 draft를 사람이 확정** 중 택. → 고민 1의 "정밀 게이트가 계산 많은 정책에 과한가"가 실증 쟁점으로 굳음.

## 토론 로그
- (2026-07-21) 문서 개설. 신호 A·B, 쟁점 정리.
- (2026-07-21) 전체 토론(1~12) 정리. 잠정 5규칙 + 하네스 미달 확인. **열림 유지** — skill 복사해 실증 후 closed 이관 예정.
- (2026-07-21) 결정을 [../v1-spec-first-design.md](../v1-spec-first-design.md)에 fixed(피드백 Q1·Q2 답 + trade-off 포함). 이 문서는 **열림 유지** — `generate-test-v1` 복사본에서 5규칙 산출을 실증한 뒤 closed 이관.

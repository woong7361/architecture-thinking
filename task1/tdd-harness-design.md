# TDD 하네스 설계 (A-5 파이프라인 v0)

> 목적: 요구사항 → 테스트 변환 파이프라인의 흐름 설계.
> 개념적 근거(왜 이렇게 나뉘는가)는 tdd-ai-notes.md 참조. 이 문서는 "어떻게(흐름)".
> 재사용 기반: .codex/skills/blog-draft/pipeline/ (Gen·Critique·Eval·Refine·Validate
> + JSON 스키마 + YAML 루브릭 + runs/ 반복 수렴). recruiting-harness-pipeline/도 참고.
> 주의: 아래는 설계 제안. 책·과제문에 명시된 게 아님.

---

## 핵심 결론: 왜 2스트림 · 2단인가

블로그 하네스는 산출물이 **하나(초안)**라 일렬 루프가 맞았다.
테스트 도메인은 산출물이 **수명이 다른 둘**이다:

- **계약(Gherkin) = 안정 게이트.** 사람이 소유, 동결 대상. (인수 층)
- **단위 = 유동 비계.** 재생성 가능, 계약이 지켜봄. (단위 층)

수명이 다르면 같은 파이프를 통과시키면 안 된다.
→ 이것이 "Phase 0에서 재사용한 것 vs 새로 만든 것"(수행내용 1)의 핵심 차이점.

---

## 흐름 다이어그램 (v0)

```
요구사항(NL)
   │
   ├─ Phase A · 계약 스트림 (안정 / 사람 소유)
   │    Gen_contract → Critique(시니어 QA) → Eval(계약 루브릭)
   │    → Validate[스키마 + 행동-고도 가드 + 금지패턴]
   │    → ★사람 승인/REJECT★ → 【동결된 인수 게이트】
   │                                    │  (계약이 다음 단계의 입력·제약)
   │                                    ▼
   └─ Phase B · 단위 스트림 (유동 / 재생성 가능)
        Gen_unit(계약 제약 하) → Critique → Eval(FIRST·설계 루브릭)
        → Validate → 단위 테스트  ← 【동결 게이트가 행동 회귀 판정】

설계 변경 시:  Phase B만 재실행,  Phase A 게이트가 "행동 보존됐나" 판정
```

---

## 4단 매핑 (과제문 Step ↔ 스트림)

| Step | 계약 스트림(A) | 단위 스트림(B) |
|---|---|---|
| Gen | 요구사항 → Gherkin 시나리오 | 계약 제약 하 → 단위 테스트 초안 |
| Critique | 시니어 QA: 누락 시나리오·모호한 Then | Mock 남용·경계값 누락 |
| Eval | 계약 루브릭(아래) | 단위 루브릭(아래) |
| Validate | 스키마 + 행동-고도 가드 + 금지패턴 + 사람 승인 | 스키마 + 금지패턴 |

---

## 원래 A-5 스케치 대비 달라진 5가지

1. **한 스트림 → 두 스트림.** 계약/단위 분리 생성·분리 검증.
2. **병렬 산출 → 2단(outside-in).** 계약을 먼저 동결, 단위는 계약을 제약으로 받음.
   형제가 아니라 상하 관계.
3. **단일 루브릭 → 고도별 루브릭.**
   - 계약축: 행동성(구현 세부 미노출), 사용자 관측 시나리오 완결성, unambiguity
   - 단위축: FIRST(fast·independent), 설계를 이끄는가, Mock 남용 없는가
   - 과제문 4축(coverage/unambiguity/independence/executability)은 계약 층에 가까움.
4. **균일 Validate → 계약에 "행동-고도 가드" 추가.**
   'should work' 금지 + **클래스명·메서드명 노출 시 REJECT**(게이트가 너무 낮아지는 것 차단).
   사람 승인 게이트는 계약 스트림에 집중.
5. **일회성 일렬 → 재진입 루프.** 계약=안정 노드, 단위=재실행 노드.
   "단위 자유롭게 갈아엎되 계약이 지켜본다"를 토폴로지로 구현.

---

## 루브릭 초안 (교체 대상)

### 계약 루브릭 (contract.rubric.yaml)
- coverage: 경계·실패 시나리오 포함 여부
- unambiguity: Then이 검증 가능한 단언인가
- behavioral: 구현 세부(클래스/메서드) 미노출 — outside-in 고도 유지
- independence: 시나리오 간 상태 공유 없음

### 단위 루브릭 (unit.rubric.yaml)
- first_fast_independent: 수십 ms·순서 무관
- design_pressure: 테스트가 설계를 이끄는가 (강결합 신호 감지)
- mock_discipline: 순수 도메인 로직을 Mock으로 감싸지 않음
- executability: Step/구현으로 실현 가능한가

---

## v0 범위 결정

- **양보 불가(고도 원칙의 직접 귀결):**
  1. 스트림 분리, 2. 계약 먼저 동결.
- **v0에서 얇게 두고 반복 채움 가능:** 고도별 루브릭 세분화(3), 행동-고도 가드
  자동화(4), 재진입 루프 자동화(5).

---

## 정직하게 마주할 긴장 (실행·회고 시)

- **순환성:** Gen·Critique·Eval 모두 AI면 블라인드 스팟이 상관관계를 가짐 →
  수행내용 2의 손으로 쓴 A-3/A-4 테스트 비교(ground truth)와 사람 소유 루브릭이 순환을 끊음.
- **루브릭 게이밍(Goodhart):** min_total 통과 ≠ 이빨 있음 → 생성된 테스트에
  결함 주입(mutation) 한 번 더.
- **수렴 ≠ 정답:** MAX_ITERATIONS 수렴은 "더 안 변함"일 뿐. 로그에 "수렴했는데 틀린
  케이스" 여부도 기록.

---

## TODO

- [ ] blog-draft/pipeline 구조에서 재사용 파일 vs 신설 파일 매핑표 작성 (수행내용 1)
- [ ] contract.rubric.yaml / unit.rubric.yaml 실제 작성
- [ ] 행동-고도 가드(구현 세부 노출 감지) Validate 규칙 구현
- [ ] A-3/A-4 요구사항 통과시키고 손으로 쓴 테스트와 비교 (수행내용 2)
- [ ] 실행 로그 보존 + MAX_ITERATIONS 수렴 횟수 기록 (수행내용 3)
